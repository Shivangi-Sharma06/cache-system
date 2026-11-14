package com.cse309.cache.simulation;

import com.cse309.cache.core.Cache;
import com.cse309.cache.exception.InvalidCacheOperationException;
import com.cse309.cache.history.OperationHistory;
import com.cse309.cache.history.OperationRecord;
import com.cse309.cache.logging.AppLogging;
import com.cse309.cache.threading.BoundedBuffer;
import com.cse309.cache.threading.PauseGate;
import com.cse309.cache.threading.ThreadStateMonitor;
import com.cse309.cache.threading.WorkerPool;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Orchestrator for concurrent workload simulations.
 * Governs lifecycle (start, pause, resume, stop) and gathers live metrics.
 */
public final class SimulationController {

    private static final Logger LOGGER = AppLogging.getLogger(SimulationController.class);

    private final Cache<String, String> cache;
    private final OperationHistory history;

    private final AtomicInteger generatedCount = new AtomicInteger();
    private final AtomicInteger completedCount = new AtomicInteger();
    private final AtomicInteger successCount = new AtomicInteger();
    private final AtomicInteger failureCount = new AtomicInteger();

    private volatile SimulationConfig activeConfig;
    private volatile SimulationProgress.State currentState = SimulationProgress.State.IDLE;
    private volatile long startTimeNanos;

    private BoundedBuffer<PlannedOperation> buffer;
    private PauseGate pauseGate;
    private OperationProducerTask producerTask;
    private Thread producerThread;
    private WorkerPool workerPool;

    public SimulationController(Cache<String, String> cache, OperationHistory history) {
        this.cache = Objects.requireNonNull(cache, "cache cannot be null");
        this.history = history;
    }

    public synchronized void start(SimulationConfig config) throws InvalidCacheOperationException {
        Objects.requireNonNull(config, "SimulationConfig cannot be null");

        if (isActive()) {
            throw new InvalidCacheOperationException(
                    "A simulation run is already active",
                    "A simulation is already currently running. Please stop it first.");
        }

        this.activeConfig = config;
        this.generatedCount.set(0);
        this.completedCount.set(0);
        this.successCount.set(0);
        this.failureCount.set(0);
        this.startTimeNanos = System.nanoTime();
        this.currentState = SimulationProgress.State.RUNNING;

        this.buffer = new BoundedBuffer<>(config.getBufferCapacity());
        this.pauseGate = new PauseGate();

        if (history != null) {
            history.record(OperationRecord.create(
                    "SIMULATION_START", "SYSTEM",
                    "Started run with " + config.getTotalOperations() + " ops on " + config.getThreadCount() + " threads"
            ));
        }

        // 1. Create and start worker pool
        this.workerPool = new WorkerPool(
                config.getThreadCount(),
                config.isPriorityExperiment(),
                cache,
                buffer,
                pauseGate,
                history,
                config.isLogEveryOperation(),
                completedCount,
                successCount,
                failureCount
        );
        this.workerPool.startAll();

        // 2. Create and start producer thread
        this.producerTask = new OperationProducerTask(config, buffer, generatedCount);
        this.producerThread = new Thread(producerTask, "cache-simulation-producer");
        this.producerThread.setDaemon(true);
        this.producerThread.start();

        LOGGER.info("Simulation started: " + config.getTotalOperations() + " operations, " +
                config.getThreadCount() + " workers");
    }

    public synchronized void pause() {
        if (currentState == SimulationProgress.State.RUNNING && pauseGate != null) {
            pauseGate.pause();
            currentState = SimulationProgress.State.PAUSED;
            LOGGER.info("Simulation paused");
        }
    }

    public synchronized void resume() {
        if (currentState == SimulationProgress.State.PAUSED && pauseGate != null) {
            pauseGate.resume();
            currentState = SimulationProgress.State.RUNNING;
            LOGGER.info("Simulation resumed");
        }
    }

    public synchronized void stop() {
        if (!isActive()) {
            return;
        }

        LOGGER.info("Stopping simulation...");
        currentState = SimulationProgress.State.CANCELLED;

        // Cooperative shutdown sequence
        if (producerTask != null) {
            producerTask.cancel();
        }
        if (buffer != null) {
            buffer.close();
        }
        if (pauseGate != null) {
            pauseGate.resume(); // Ensure workers aren't stuck waiting on pause
        }
        if (producerThread != null) {
            producerThread.interrupt();
        }
        if (workerPool != null) {
            workerPool.interruptAll();
            try {
                workerPool.joinAll(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (history != null) {
            history.record(OperationRecord.create(
                    "SIMULATION_STOP", "SYSTEM",
                    "Stopped run at " + completedCount.get() + " operations completed"
            ));
        }
        LOGGER.info("Simulation stopped");
    }

    public boolean isActive() {
        return currentState == SimulationProgress.State.RUNNING || currentState == SimulationProgress.State.PAUSED;
    }

    public SimulationProgress getProgress() {
        SimulationProgress.State state = this.currentState;
        if (state == SimulationProgress.State.RUNNING || state == SimulationProgress.State.PAUSED) {
            if (activeConfig != null && completedCount.get() >= activeConfig.getTotalOperations()) {
                state = SimulationProgress.State.COMPLETED;
                this.currentState = state;
            }
        }

        double elapsedSeconds = Math.max(0.001, (System.nanoTime() - startTimeNanos) / 1_000_000_000.0);
        int runningWorkers = workerPool != null ? workerPool.getRunningCount() : 0;
        var stats = cache.getStatistics().snapshot(cache.size(), cache.getCapacity());

        return new SimulationProgress(
                generatedCount.get(),
                completedCount.get(),
                successCount.get(),
                failureCount.get(),
                runningWorkers,
                elapsedSeconds,
                cache.size(),
                stats.getHits(),
                stats.getMisses(),
                state
        );
    }

    public List<ThreadStateMonitor.WorkerStatus> getWorkerStatuses() {
        return workerPool != null ? ThreadStateMonitor.inspectWorkers(workerPool.getWorkers()) : List.of();
    }

    public SimulationConfig getActiveConfig() {
        return activeConfig;
    }
}
