package com.cse309.cache.simulation;

/**
 * Immutable value object reporting live simulation metrics and completion state.
 */
public final class SimulationProgress {

    public enum State {
        IDLE,
        RUNNING,
        PAUSED,
        COMPLETED,
        CANCELLED
    }

    private final int generated;
    private final int completed;
    private final int succeeded;
    private final int failed;
    private final int pending;
    private final int runningWorkers;
    private final double elapsedSeconds;
    private final double opsPerSecond;
    private final int currentCacheSize;
    private final long hits;
    private final long misses;
    private final State state;

    public SimulationProgress(int generated,
                              int completed,
                              int succeeded,
                              int failed,
                              int runningWorkers,
                              double elapsedSeconds,
                              int currentCacheSize,
                              long hits,
                              long misses,
                              State state) {
        this.generated = generated;
        this.completed = completed;
        this.succeeded = succeeded;
        this.failed = failed;
        this.pending = Math.max(0, generated - completed);
        this.runningWorkers = runningWorkers;
        this.elapsedSeconds = elapsedSeconds;
        this.opsPerSecond = elapsedSeconds > 0 ? (double) completed / elapsedSeconds : 0.0;
        this.currentCacheSize = currentCacheSize;
        this.hits = hits;
        this.misses = misses;
        this.state = state;
    }

    public static SimulationProgress idle(int cacheSize) {
        return new SimulationProgress(0, 0, 0, 0, 0, 0.0, cacheSize, 0, 0, State.IDLE);
    }

    public int getGenerated() { return generated; }
    public int getCompleted() { return completed; }
    public int getSucceeded() { return succeeded; }
    public int getFailed() { return failed; }
    public int getPending() { return pending; }
    public int getRunningWorkers() { return runningWorkers; }
    public double getElapsedSeconds() { return elapsedSeconds; }
    public double getOpsPerSecond() { return opsPerSecond; }
    public int getCurrentCacheSize() { return currentCacheSize; }
    public long getHits() { return hits; }
    public long getMisses() { return misses; }
    public State getState() { return state; }

    public double getProgressFraction(int totalOperations) {
        return totalOperations <= 0 ? 0.0 : Math.min(1.0, (double) completed / totalOperations);
    }

    @Override
    public String toString() {
        return String.format("[%s] Completed %d/%d (%.1f ops/sec, size=%d)",
                state, completed, generated, opsPerSecond, currentCacheSize);
    }
}
