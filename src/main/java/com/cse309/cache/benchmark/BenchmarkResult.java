package com.cse309.cache.benchmark;

import java.time.Instant;
import java.util.Arrays;

/**
 * Encapsulates the results, environment parameters, and performance matrix of a benchmark run.
 * Demonstrates 2-D double[][] matrix usage (Unit 1).
 */
public final class BenchmarkResult {

    private final Long id;
    private final long ranAt;
    private final String evictionPolicy;
    private final int capacity;
    private final int threads;
    private final String keyDistribution;
    private final String operationMix;
    private final int totalOps;
    private final long durationNanos;
    private final double opsPerSecond;
    private final double hitRatio;
    private final int evictions;

    // Environmental metadata
    private final String jdkVersion;
    private final String osName;
    private final int availableProcessors;

    // 2-D result matrix: [repetition][metric] where metrics are:
    // [0]=opsPerSec, [1]=hitRatio, [2]=durationNanos (Unit 1 double[][])
    private final double[][] runMatrix;

    public BenchmarkResult(Long id,
                           long ranAt,
                           String evictionPolicy,
                           int capacity,
                           int threads,
                           String keyDistribution,
                           String operationMix,
                           int totalOps,
                           long durationNanos,
                           double opsPerSecond,
                           double hitRatio,
                           int evictions,
                           String jdkVersion,
                           String osName,
                           int availableProcessors,
                           double[][] runMatrix) {
        this.id = id;
        this.ranAt = ranAt;
        this.evictionPolicy = evictionPolicy;
        this.capacity = capacity;
        this.threads = threads;
        this.keyDistribution = keyDistribution;
        this.operationMix = operationMix;
        this.totalOps = totalOps;
        this.durationNanos = durationNanos;
        this.opsPerSecond = opsPerSecond;
        this.hitRatio = hitRatio;
        this.evictions = evictions;
        this.jdkVersion = jdkVersion;
        this.osName = osName;
        this.availableProcessors = availableProcessors;
        this.runMatrix = runMatrix != null ? copyMatrix(runMatrix) : new double[0][0];
    }

    private static double[][] copyMatrix(double[][] src) {
        double[][] dst = new double[src.length][];
        for (int i = 0; i < src.length; i++) {
            dst[i] = Arrays.copyOf(src[i], src[i].length);
        }
        return dst;
    }

    public Long getId() { return id; }
    public long getRanAt() { return ranAt; }
    public Instant getRanAtInstant() { return Instant.ofEpochMilli(ranAt); }
    public String getEvictionPolicy() { return evictionPolicy; }
    public int getCapacity() { return capacity; }
    public int getThreads() { return threads; }
    public String getKeyDistribution() { return keyDistribution; }
    public String getOperationMix() { return operationMix; }
    public int getTotalOps() { return totalOps; }
    public long getDurationNanos() { return durationNanos; }
    public double getOpsPerSecond() { return opsPerSecond; }
    public double getHitRatio() { return hitRatio; }
    public int getEvictions() { return evictions; }
    public String getJdkVersion() { return jdkVersion; }
    public String getOsName() { return osName; }
    public int getAvailableProcessors() { return availableProcessors; }
    public double[][] getRunMatrix() { return copyMatrix(runMatrix); }

    @Override
    public String toString() {
        return String.format("Benchmark [%s] policy=%s, cap=%d, threads=%d -> %.1f ops/sec (hit=%.1f%%)",
                getRanAtInstant(), evictionPolicy, capacity, threads, opsPerSecond, hitRatio);
    }
}
