package com.cse309.cache.simulation;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable configuration defining parameters for a simulation workload run.
 */
public final class SimulationConfig {

    private final int threadCount;
    private final int totalOperations;
    private final OperationMix operationMix;
    private final KeyDistribution keyDistribution;
    private final int keySpaceSize;
    private final int bufferCapacity;
    private final Long seed;
    private final boolean priorityExperiment;
    private final boolean logEveryOperation;

    public SimulationConfig(int threadCount,
                            int totalOperations,
                            OperationMix operationMix,
                            KeyDistribution keyDistribution,
                            int keySpaceSize,
                            int bufferCapacity,
                            Long seed,
                            boolean priorityExperiment,
                            boolean logEveryOperation) {
        if (threadCount < 1 || threadCount > 64) {
            throw new IllegalArgumentException("Worker threads must be between 1 and 64");
        }
        if (totalOperations <= 0) {
            throw new IllegalArgumentException("Total operations must be positive");
        }
        if (keySpaceSize <= 0) {
            throw new IllegalArgumentException("Key space size must be positive");
        }
        this.threadCount = threadCount;
        this.totalOperations = totalOperations;
        this.operationMix = Objects.requireNonNull(operationMix, "operationMix cannot be null");
        this.keyDistribution = Objects.requireNonNull(keyDistribution, "keyDistribution cannot be null");
        this.keySpaceSize = keySpaceSize;
        this.bufferCapacity = bufferCapacity > 0 ? bufferCapacity : 256;
        this.seed = seed;
        this.priorityExperiment = priorityExperiment;
        this.logEveryOperation = logEveryOperation && totalOperations <= 500;
    }

    public static SimulationConfig defaultQuickRun() {
        return new SimulationConfig(4, 1000, OperationMix.MIXED, KeyDistribution.HOTSPOT_80_20, 200, 256, null, false, false);
    }

    public int getThreadCount() { return threadCount; }
    public int getTotalOperations() { return totalOperations; }
    public OperationMix getOperationMix() { return operationMix; }
    public KeyDistribution getKeyDistribution() { return keyDistribution; }
    public int getKeySpaceSize() { return keySpaceSize; }
    public int getBufferCapacity() { return bufferCapacity; }
    public Optional<Long> getSeed() { return Optional.ofNullable(seed); }
    public Long getSeedOrNull() { return seed; }
    public boolean isPriorityExperiment() { return priorityExperiment; }
    public boolean isLogEveryOperation() { return logEveryOperation; }
}
