package com.cse309.cache.stats;

import java.time.Instant;
import java.util.Arrays;

/**
 * Immutable snapshot of cache statistics and performance counters.
 */
public final class StatisticsSnapshot {

    private final long getRequests;
    private final long putRequests;
    private final long updateRequests;
    private final long removeRequests;
    private final long searchRequests;

    private final long hits;
    private final long misses;
    private final long evictions;
    private final long expirations;

    private final long successfulOperations;
    private final long failedOperations;

    private final long entriesInserted;
    private final long entriesRemoved;
    private final long entriesCleared;

    private final int currentSize;
    private final int currentCapacity;

    // Per-operation latencies: [0]=count, [1]=totalNanos, [2]=maxNanos
    private final long[] getLatency;
    private final long[] putLatency;
    private final long[] updateLatency;
    private final long[] removeLatency;
    private final long[] searchLatency;

    private final long[] histogramCounts;
    private final long timestampMillis;
    private final double elapsedSeconds;

    public StatisticsSnapshot(long getRequests,
                              long putRequests,
                              long updateRequests,
                              long removeRequests,
                              long searchRequests,
                              long hits,
                              long misses,
                              long evictions,
                              long expirations,
                              long successfulOperations,
                              long failedOperations,
                              long entriesInserted,
                              long entriesRemoved,
                              long entriesCleared,
                              int currentSize,
                              int currentCapacity,
                              long[] getLatency,
                              long[] putLatency,
                              long[] updateLatency,
                              long[] removeLatency,
                              long[] searchLatency,
                              long[] histogramCounts,
                              long timestampMillis,
                              double elapsedSeconds) {
        this.getRequests = getRequests;
        this.putRequests = putRequests;
        this.updateRequests = updateRequests;
        this.removeRequests = removeRequests;
        this.searchRequests = searchRequests;
        this.hits = hits;
        this.misses = misses;
        this.evictions = evictions;
        this.expirations = expirations;
        this.successfulOperations = successfulOperations;
        this.failedOperations = failedOperations;
        this.entriesInserted = entriesInserted;
        this.entriesRemoved = entriesRemoved;
        this.entriesCleared = entriesCleared;
        this.currentSize = currentSize;
        this.currentCapacity = currentCapacity;
        this.getLatency = Arrays.copyOf(getLatency, 3);
        this.putLatency = Arrays.copyOf(putLatency, 3);
        this.updateLatency = Arrays.copyOf(updateLatency, 3);
        this.removeLatency = Arrays.copyOf(removeLatency, 3);
        this.searchLatency = Arrays.copyOf(searchLatency, 3);
        this.histogramCounts = Arrays.copyOf(histogramCounts, histogramCounts.length);
        this.timestampMillis = timestampMillis;
        this.elapsedSeconds = Math.max(0.0001, elapsedSeconds);
    }

    public long getGetRequests() { return getRequests; }
    public long getPutRequests() { return putRequests; }
    public long getUpdateRequests() { return updateRequests; }
    public long getRemoveRequests() { return removeRequests; }
    public long getSearchRequests() { return searchRequests; }

    public long getTotalRequests() {
        return getRequests + putRequests + updateRequests + removeRequests + searchRequests;
    }

    public long getHits() { return hits; }
    public long getMisses() { return misses; }
    public long getEvictions() { return evictions; }
    public long getExpirations() { return expirations; }

    public long getSuccessfulOperations() { return successfulOperations; }
    public long getFailedOperations() { return failedOperations; }

    public long getEntriesInserted() { return entriesInserted; }
    public long getEntriesRemoved() { return entriesRemoved; }
    public long getEntriesCleared() { return entriesCleared; }

    public int getCurrentSize() { return currentSize; }
    public int getCurrentCapacity() { return currentCapacity; }

    public double getHitRatio() {
        long totalLookups = hits + misses;
        return totalLookups == 0 ? 0.0 : ((double) hits / totalLookups) * 100.0;
    }

    public double getMissRatio() {
        long totalLookups = hits + misses;
        return totalLookups == 0 ? 0.0 : ((double) misses / totalLookups) * 100.0;
    }

    public double getUtilization() {
        return currentCapacity == 0 ? 0.0 : ((double) currentSize / currentCapacity) * 100.0;
    }

    public double getThroughputOpsPerSec() {
        return (double) getTotalRequests() / elapsedSeconds;
    }

    public long[] getGetLatency() { return Arrays.copyOf(getLatency, 3); }
    public long[] getPutLatency() { return Arrays.copyOf(putLatency, 3); }
    public long[] getUpdateLatency() { return Arrays.copyOf(updateLatency, 3); }
    public long[] getRemoveLatency() { return Arrays.copyOf(removeLatency, 3); }
    public long[] getSearchLatency() { return Arrays.copyOf(searchLatency, 3); }

    public double getAverageLatencyMicros(long[] lat) {
        return lat[0] == 0 ? 0.0 : ((double) lat[1] / lat[0]) / 1_000.0;
    }

    public double getMaxLatencyMicros(long[] lat) {
        return (double) lat[2] / 1_000.0;
    }

    public long[] getHistogramCounts() {
        return Arrays.copyOf(histogramCounts, histogramCounts.length);
    }

    public long getTimestampMillis() { return timestampMillis; }
    public Instant getInstant() { return Instant.ofEpochMilli(timestampMillis); }
    public double getElapsedSeconds() { return elapsedSeconds; }
}
