package com.cse309.cache.persistence;

import com.cse309.cache.benchmark.BenchmarkResult;
import com.cse309.cache.core.CacheEntrySnapshot;
import com.cse309.cache.core.StringCache;
import com.cse309.cache.eviction.EvictionPolicyType;
import com.cse309.cache.history.OperationRecord;
import com.cse309.cache.testutil.MutableClock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void testIdempotentSchemaAndSnapshotCrud() throws Exception {
        Path dbPath = tempDir.resolve("test.db");
        DatabaseManager db = new DatabaseManager(dbPath);

        // Idempotent schema initialization
        db.initSchema();
        db.initSchema(); // Calling twice should succeed with IF NOT EXISTS

        SnapshotRepository repo = new SnapshotRepository(db);

        List<CacheEntrySnapshot<String, String>> entries = List.of(
                new CacheEntrySnapshot<>("k1", "v1", 100L, 100L, -1L, 1L, 1L),
                new CacheEntrySnapshot<>("k2", "v2", 200L, 200L, 5000L, 3L, 2L)
        );

        DbSnapshot snap = DbSnapshot.createNew("test-snapshot", "LRU", 100, entries);
        DbSnapshot saved = repo.save(snap);
        assertNotNull(saved.getId());

        // Find by ID
        Optional<DbSnapshot> loaded = repo.findById(saved.getId());
        assertTrue(loaded.isPresent());
        assertEquals("test-snapshot", loaded.get().getName());
        assertEquals(2, loaded.get().getEntries().size());

        // Find All
        List<DbSnapshot> all = repo.findAll();
        assertEquals(1, all.size());

        // Delete
        boolean deleted = repo.deleteById(saved.getId());
        assertTrue(deleted);
        assertTrue(repo.findById(saved.getId()).isEmpty());
    }

    @Test
    void testRestoreWithExpiredAndCapacityTruncation() throws Exception {
        Path dbPath = tempDir.resolve("restore.db");
        DatabaseManager db = new DatabaseManager(dbPath);
        db.initSchema();

        MutableClock clock = new MutableClock(10_000L);
        PersistenceCoordinator coordinator = new PersistenceCoordinator(db, PersistenceMode.PERSISTENT);

        long now = clock.millis();
        List<CacheEntrySnapshot<String, String>> entries = List.of(
                new CacheEntrySnapshot<>("live1", "v1", now, now, -1L, 1L, 1L),
                new CacheEntrySnapshot<>("expiredKey", "v2", now - 5000, now - 5000, now - 1000, 1L, 2L), // Expired!
                new CacheEntrySnapshot<>("live2", "v3", now, now + 10, -1L, 1L, 3L),
                new CacheEntrySnapshot<>("live3", "v4", now, now + 20, -1L, 1L, 4L)
        );

        DbSnapshot snap = DbSnapshot.createNew("restore-test", "LRU", 10, entries);

        // Cache with capacity 2!
        StringCache cache = new StringCache(2, EvictionPolicyType.LRU, clock);

        var restoreResult = coordinator.restoreSnapshot(cache, snap, clock);

        assertEquals(1, restoreResult.expiredSkipped()); // expiredKey skipped
        assertEquals(1, restoreResult.excessDropped());   // live1, live2, live3 = 3 live entries, capacity 2 => 1 dropped
        assertEquals(2, restoreResult.entriesRestored());
        assertEquals(2, cache.size());

        coordinator.stop();
    }

    @Test
    void testOperationHistoryRepositoryBatch() throws Exception {
        Path dbPath = tempDir.resolve("history.db");
        DatabaseManager db = new DatabaseManager(dbPath);
        db.initSchema();

        OperationHistoryRepository repo = new OperationHistoryRepository(db);

        List<OperationRecord> records = List.of(
                OperationRecord.create("PUT", "k1", "INSERTED", 500, ""),
                OperationRecord.create("GET", "k1", "HIT", 200, ""),
                OperationRecord.create("DELETE", "k1", "REMOVED", 300, "")
        );

        repo.saveBatch(records);

        List<OperationRecord> loaded = repo.findRecent(10);
        assertEquals(3, loaded.size());
    }

    @Test
    void testBenchmarkRepositorySaveAndQuery() throws Exception {
        Path dbPath = tempDir.resolve("benchmark.db");
        DatabaseManager db = new DatabaseManager(dbPath);
        db.initSchema();

        BenchmarkRepository repo = new BenchmarkRepository(db);

        BenchmarkResult result = new BenchmarkResult(
                null, System.currentTimeMillis(), "LRU", 100, 4,
                "UNIFORM", "READ_HEAVY", 10000, 500_000_000L, 20000.0, 85.5, 50,
                "21", "Linux", 8, new double[0][0]
        );

        BenchmarkResult saved = repo.save(result);
        assertNotNull(saved.getId());

        Optional<BenchmarkResult> loaded = repo.findById(saved.getId());
        assertTrue(loaded.isPresent());
        assertEquals(20000.0, loaded.get().getOpsPerSecond());
    }
}
