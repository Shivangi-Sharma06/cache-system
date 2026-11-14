package com.cse309.cache.persistence;

import com.cse309.cache.core.Cache;
import com.cse309.cache.core.CacheEntrySnapshot;
import com.cse309.cache.exception.CacheException;
import com.cse309.cache.exception.CachePersistenceException;
import com.cse309.cache.history.OperationRecord;
import com.cse309.cache.logging.AppLogging;
import com.cse309.cache.threading.BoundedBuffer;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Coordinates durable SQLite persistence out-of-band on dedicated background threads.
 * Manages write-behind history logging, safe point-in-time snapshots, and warm-start restoration.
 */
public final class PersistenceCoordinator {

    private static final Logger LOGGER = AppLogging.getLogger(PersistenceCoordinator.class);
    private static final int HISTORY_QUEUE_CAPACITY = 2048;
    private static final int BATCH_SIZE = 100;
    private static final long FLUSH_INTERVAL_MILLIS = 2000L;

    private final DatabaseManager databaseManager;
    private final SnapshotRepository snapshotRepository;
    private final OperationHistoryRepository historyRepository;

    private final BoundedBuffer<OperationRecord> historyWriteQueue;
    private volatile PersistenceStatus status;
    private volatile PersistenceMode mode;

    private volatile boolean running;
    private Thread historyWriterThread;

    public record RestoreResult(
            int entriesRestored,
            int expiredSkipped,
            int excessDropped
    ) {}

    public PersistenceCoordinator(DatabaseManager databaseManager, PersistenceMode initialMode) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager cannot be null");
        this.snapshotRepository = new SnapshotRepository(databaseManager);
        this.historyRepository = new OperationHistoryRepository(databaseManager);
        this.historyWriteQueue = new BoundedBuffer<>(HISTORY_QUEUE_CAPACITY);
        this.mode = initialMode != null ? initialMode : PersistenceMode.IN_MEMORY_ONLY;
        this.status = PersistenceStatus.available(System.currentTimeMillis());

        try {
            databaseManager.initSchema();
            this.status = PersistenceStatus.available(System.currentTimeMillis());
        } catch (Exception e) {
            this.status = PersistenceStatus.unavailable("Database init failed: " + e.getMessage());
            LOGGER.log(Level.WARNING, "Failed to initialize database: " + e.getMessage(), e);
        }
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        historyWriterThread = new Thread(this::historyWriterLoop, "cache-history-writer");
        historyWriterThread.setDaemon(true);
        historyWriterThread.start();
        LOGGER.info("PersistenceCoordinator started (mode=" + mode + ")");
    }

    private void historyWriterLoop() {
        List<OperationRecord> batch = new ArrayList<>(BATCH_SIZE);
        long lastFlushTime = System.currentTimeMillis();

        while (running) {
            try {
                // Poll or wait briefly
                OperationRecord record = historyWriteQueue.take();
                if (record != null) {
                    batch.add(record);
                }

                long now = System.currentTimeMillis();
                boolean intervalElapsed = (now - lastFlushTime) >= FLUSH_INTERVAL_MILLIS;

                if (!batch.isEmpty() && (batch.size() >= BATCH_SIZE || intervalElapsed || record == null)) {
                    flushHistoryBatch(batch);
                    batch.clear();
                    lastFlushTime = now;
                }

                if (record == null && historyWriteQueue.isClosed()) {
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in history writer loop: " + e.getMessage(), e);
            }
        }

        // Drain any remaining records upon exit
        if (!batch.isEmpty()) {
            flushHistoryBatch(batch);
            batch.clear();
        }
    }

    private void flushHistoryBatch(List<OperationRecord> batch) {
        if (mode != PersistenceMode.PERSISTENT) {
            return;
        }
        try {
            historyRepository.saveBatch(batch);
            status = PersistenceStatus.available(System.currentTimeMillis());
        } catch (Exception e) {
            status = PersistenceStatus.unavailable("Failed to flush history: " + e.getMessage());
            LOGGER.warning("Could not persist history batch: " + e.getMessage());
        }
    }

    public void enqueueHistoryRecord(OperationRecord record) {
        if (record == null || mode != PersistenceMode.PERSISTENT) return;
        try {
            if (!historyWriteQueue.isClosed()) {
                // Non-blocking offer if possible, or bounded put
                historyWriteQueue.put(record);
            }
        } catch (Exception e) {
            // Drop oldest on overflow; never block cache operations
        }
    }

    public DbSnapshot saveSnapshot(Cache<String, String> cache, String name) throws CachePersistenceException {
        Objects.requireNonNull(cache, "cache cannot be null");
        String snapName = name != null && !name.isBlank() ? name : "snapshot-" + System.currentTimeMillis();

        // 1. Copy entries inside lock (fast), then release
        List<CacheEntrySnapshot<String, String>> entries = cache.getAllEntries();
        DbSnapshot snapshot = DbSnapshot.createNew(
                snapName,
                cache.getEvictionPolicyType().name(),
                cache.getCapacity(),
                entries
        );

        // 2. Write to SQLite outside cache lock
        try {
            DbSnapshot saved = snapshotRepository.save(snapshot);
            status = PersistenceStatus.available(System.currentTimeMillis());
            return saved;
        } catch (CachePersistenceException e) {
            status = PersistenceStatus.unavailable("Snapshot save failed: " + e.getMessage());
            throw e;
        }
    }

    public RestoreResult restoreSnapshot(Cache<String, String> cache, DbSnapshot snapshot, Clock clock)
            throws CacheException {
        Objects.requireNonNull(cache, "cache cannot be null");
        Objects.requireNonNull(snapshot, "snapshot cannot be null");

        List<CacheEntrySnapshot<String, String>> rawEntries = snapshot.getEntries();
        long now = clock.millis();

        // 1. Filter out expired entries
        List<CacheEntrySnapshot<String, String>> liveEntries = new ArrayList<>();
        int expiredSkipped = 0;
        for (CacheEntrySnapshot<String, String> entry : rawEntries) {
            if (entry.isExpired(clock)) {
                expiredSkipped++;
            } else {
                liveEntries.add(entry);
            }
        }

        // 2. If snapshot holds more entries than cache capacity, keep most recently accessed ones
        int excessDropped = 0;
        int capacity = cache.getCapacity();
        if (liveEntries.size() > capacity) {
            excessDropped = liveEntries.size() - capacity;
            // Sort by lastAccessAt descending, take top 'capacity'
            liveEntries = liveEntries.stream()
                    .sorted(Comparator.comparingLong(CacheEntrySnapshot<String, String>::getLastAccessAt).reversed())
                    .limit(capacity)
                    .sorted(Comparator.comparingLong(CacheEntrySnapshot<String, String>::getInsertionSequence))
                    .toList();
        }

        // 3. Atomically replace cache contents
        cache.replaceAll(liveEntries);
        LOGGER.info(String.format("Restored snapshot '%s': %d entries restored, %d expired skipped, %d excess dropped",
                snapshot.getName(), liveEntries.size(), expiredSkipped, excessDropped));

        return new RestoreResult(liveEntries.size(), expiredSkipped, excessDropped);
    }

    public Optional<DbSnapshot> loadLatestSnapshot() {
        try {
            return snapshotRepository.findLatest();
        } catch (Exception e) {
            LOGGER.warning("Could not load latest snapshot: " + e.getMessage());
            return Optional.empty();
        }
    }

    public void stop() {
        running = false;
        historyWriteQueue.close();
        if (historyWriterThread != null) {
            historyWriterThread.interrupt();
            try {
                historyWriterThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public PersistenceStatus getStatus() {
        return status;
    }

    public PersistenceMode getMode() {
        return mode;
    }

    public void setMode(PersistenceMode mode) {
        this.mode = mode != null ? mode : PersistenceMode.IN_MEMORY_ONLY;
        LOGGER.info("Persistence mode changed to: " + this.mode);
    }

    public SnapshotRepository getSnapshotRepository() {
        return snapshotRepository;
    }

    public OperationHistoryRepository getHistoryRepository() {
        return historyRepository;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
