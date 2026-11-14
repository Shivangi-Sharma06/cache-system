package com.cse309.cache.stats;

import java.util.Arrays;

/**
 * Bucketed latency histogram for sub-microsecond through multi-millisecond operation latencies.
 * Demonstrates long[] array usage (Unit 1).
 */
public final class LatencyHistogram {

    public static final String[] BUCKET_LABELS = {
        "< 1 µs",
        "1 - 10 µs",
        "10 - 100 µs",
        "100 µs - 1 ms",
        "1 - 10 ms",
        ">= 10 ms"
    };

    private static final long[] THRESHOLDS_NANOS = {
        1_000L,        // 1 µs
        10_000L,       // 10 µs
        100_000L,      // 100 µs
        1_000_000L,    // 1 ms
        10_000_000L    // 10 ms
    };

    private final long[] counts;

    public LatencyHistogram() {
        this.counts = new long[BUCKET_LABELS.length];
    }

    public LatencyHistogram(long[] existingCounts) {
        if (existingCounts == null || existingCounts.length != BUCKET_LABELS.length) {
            this.counts = new long[BUCKET_LABELS.length];
        } else {
            this.counts = Arrays.copyOf(existingCounts, existingCounts.length);
        }
    }

    public synchronized void record(long latencyNanos) {
        if (latencyNanos < 0) latencyNanos = 0;
        int bucket = findBucket(latencyNanos);
        counts[bucket]++;
    }

    public static int findBucket(long latencyNanos) {
        for (int i = 0; i < THRESHOLDS_NANOS.length; i++) {
            if (latencyNanos < THRESHOLDS_NANOS[i]) {
                return i;
            }
        }
        return THRESHOLDS_NANOS.length;
    }

    public synchronized long[] getCounts() {
        return Arrays.copyOf(counts, counts.length);
    }

    public synchronized void reset() {
        Arrays.fill(counts, 0L);
    }
}
