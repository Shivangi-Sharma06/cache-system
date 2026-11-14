package com.cse309.cache.stats;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe statistics engine tracking real cache operation counters and latencies.
 * Fulfills syllabus AtomicLong counter usage (Unit 3/Enrichment).
 */
public final class CacheStatistics {

    private final AtomicLong getRequests = new AtomicLong();
    private final AtomicLong putRequests = new AtomicLong();
    private final AtomicLong updateRequests = new AtomicLong();
    private final AtomicLong removeRequests = new AtomicLong();
    private final AtomicLong searchRequests = new AtomicLong();

    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();
    private final AtomicLong evictions = new AtomicLong();
    private final AtomicLong expirations = new AtomicLong();

    private final AtomicLong successfulOperations = new AtomicLong();
    private final AtomicLong failedOperations = new AtomicLong();

    private final AtomicLong entriesInserted = new AtomicLong();
    private final AtomicLong entriesRemoved = new AtomicLong();
    private final AtomicLong entriesCleared = new AtomicLong();

    // Latency tracking: [count, totalNanos, maxNanos]
    private final AtomicLong getCount = new AtomicLong();
    private final AtomicLong getTotalNanos = new AtomicLong();
    private final AtomicLong getMaxNanos = new AtomicLong();

    private final AtomicLong putCount = new AtomicLong();
    private final AtomicLong putTotalNanos = new AtomicLong();
    private final AtomicLong putMaxNanos = new AtomicLong();

    private final AtomicLong updateCount = new AtomicLong();
    private final AtomicLong updateTotalNanos = new AtomicLong();
    private final AtomicLong updateMaxNanos = new AtomicLong();

    private final AtomicLong removeCount = new AtomicLong();
    private final AtomicLong removeTotalNanos = new AtomicLong();
    private final AtomicLong removeMaxNanos = new AtomicLong();

    private final AtomicLong searchCount = new AtomicLong();
    private final AtomicLong searchTotalNanos = new AtomicLong();
    private final AtomicLong searchMaxNanos = new AtomicLong();

    private final LatencyHistogram histogram = new LatencyHistogram();
    private volatile long startNanoTime = System.nanoTime();

    public void recordGet(boolean hit, boolean expired, long latencyNanos, boolean success) {
        getRequests.incrementAndGet();
        if (hit) {
            hits.incrementAndGet();
        } else {
            misses.incrementAndGet();
            if (expired) {
                expirations.incrementAndGet();
            }
        }
        recordOpLatency(getCount, getTotalNanos, getMaxNanos, latencyNanos);
        recordSuccess(success);
    }

    public void recordPut(boolean inserted, int evictionsCount, long latencyNanos, boolean success) {
        putRequests.incrementAndGet();
        if (inserted) {
            entriesInserted.incrementAndGet();
        }
        if (evictionsCount > 0) {
            evictions.addAndGet(evictionsCount);
        }
        recordOpLatency(putCount, putTotalNanos, putMaxNanos, latencyNanos);
        recordSuccess(success);
    }

    public void recordUpdate(long latencyNanos, boolean success) {
        updateRequests.incrementAndGet();
        recordOpLatency(updateCount, updateTotalNanos, updateMaxNanos, latencyNanos);
        recordSuccess(success);
    }

    public void recordRemove(boolean removed, long latencyNanos, boolean success) {
        removeRequests.incrementAndGet();
        if (removed) {
            entriesRemoved.incrementAndGet();
        }
        recordOpLatency(removeCount, removeTotalNanos, removeMaxNanos, latencyNanos);
        recordSuccess(success);
    }

    public void recordSearch(long latencyNanos, boolean success) {
        searchRequests.incrementAndGet();
        recordOpLatency(searchCount, searchTotalNanos, searchMaxNanos, latencyNanos);
        recordSuccess(success);
    }

    public void recordExpiration() {
        expirations.incrementAndGet();
    }

    public void recordExpirations(int count) {
        if (count > 0) {
            expirations.addAndGet(count);
        }
    }

    public void recordEviction() {
        evictions.incrementAndGet();
    }

    public void recordEvictions(int count) {
        if (count > 0) {
            evictions.addAndGet(count);
        }
    }

    public void recordClear(int entriesCount) {
        if (entriesCount > 0) {
            entriesCleared.addAndGet(entriesCount);
        }
    }

    private void recordSuccess(boolean success) {
        if (success) {
            successfulOperations.incrementAndGet();
        } else {
            failedOperations.incrementAndGet();
        }
    }

    private void recordOpLatency(AtomicLong count, AtomicLong total, AtomicLong max, long nanos) {
        count.incrementAndGet();
        total.addAndGet(nanos);
        max.accumulateAndGet(nanos, Math::max);
        histogram.record(nanos);
    }

    public void reset() {
        getRequests.set(0);
        putRequests.set(0);
        updateRequests.set(0);
        removeRequests.set(0);
        searchRequests.set(0);

        hits.set(0);
        misses.set(0);
        evictions.set(0);
        expirations.set(0);

        successfulOperations.set(0);
        failedOperations.set(0);

        entriesInserted.set(0);
        entriesRemoved.set(0);
        entriesCleared.set(0);

        getCount.set(0);
        getTotalNanos.set(0);
        getMaxNanos.set(0);

        putCount.set(0);
        putTotalNanos.set(0);
        putMaxNanos.set(0);

        updateCount.set(0);
        updateTotalNanos.set(0);
        updateMaxNanos.set(0);

        removeCount.set(0);
        removeTotalNanos.set(0);
        removeMaxNanos.set(0);

        searchCount.set(0);
        searchTotalNanos.set(0);
        searchMaxNanos.set(0);

        histogram.reset();
        startNanoTime = System.nanoTime();
    }

    public StatisticsSnapshot snapshot(int currentSize, int currentCapacity) {
        long elapsedNanos = Math.max(1L, System.nanoTime() - startNanoTime);
        double elapsedSeconds = elapsedNanos / 1_000_000_000.0;

        return new StatisticsSnapshot(
                getRequests.get(),
                putRequests.get(),
                updateRequests.get(),
                removeRequests.get(),
                searchRequests.get(),
                hits.get(),
                misses.get(),
                evictions.get(),
                expirations.get(),
                successfulOperations.get(),
                failedOperations.get(),
                entriesInserted.get(),
                entriesRemoved.get(),
                entriesCleared.get(),
                currentSize,
                currentCapacity,
                new long[]{getCount.get(), getTotalNanos.get(), getMaxNanos.get()},
                new long[]{putCount.get(), putTotalNanos.get(), putMaxNanos.get()},
                new long[]{updateCount.get(), updateTotalNanos.get(), updateMaxNanos.get()},
                new long[]{removeCount.get(), removeTotalNanos.get(), removeMaxNanos.get()},
                new long[]{searchCount.get(), searchTotalNanos.get(), searchMaxNanos.get()},
                histogram.getCounts(),
                System.currentTimeMillis(),
                elapsedSeconds
        );
    }
}
