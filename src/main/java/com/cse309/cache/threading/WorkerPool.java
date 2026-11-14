package com.cse309.cache.threading;

import com.cse309.cache.core.Cache;
import com.cse309.cache.history.OperationHistory;
import com.cse309.cache.simulation.PlannedOperation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages the lifecycle, names, and thread priorities of simulation WorkerThreads.
 * Demonstrates thread priorities and join() synchronization (Unit 3).
 */
public final class WorkerPool {

    private final List<WorkerThread> workers;

    public WorkerPool(int workerCount,
                      boolean priorityExperiment,
                      Cache<String, String> cache,
                      BoundedBuffer<PlannedOperation> buffer,
                      PauseGate pauseGate,
                      OperationHistory history,
                      boolean logEveryOperation,
                      AtomicInteger completedCount,
                      AtomicInteger successCount,
                      AtomicInteger failureCount) {
        this.workers = new ArrayList<>(workerCount);

        for (int i = 1; i <= workerCount; i++) {
            String name = "cache-worker-" + i;
            WorkerThread worker = new WorkerThread(
                    name, cache, buffer, pauseGate, history,
                    logEveryOperation, completedCount, successCount, failureCount
            );

            // Priority experiment (Unit 3): half MIN_PRIORITY, half MAX_PRIORITY
            if (priorityExperiment) {
                int priority = (i % 2 == 0) ? Thread.MAX_PRIORITY : Thread.MIN_PRIORITY;
                worker.setPriority(priority);
            } else {
                worker.setPriority(Thread.NORM_PRIORITY);
            }

            workers.add(worker);
        }
    }

    public void startAll() {
        for (WorkerThread worker : workers) {
            worker.start();
        }
    }

    public void interruptAll() {
        for (WorkerThread worker : workers) {
            worker.interrupt();
        }
    }

    public void joinAll(long timeoutMillis) throws InterruptedException {
        for (WorkerThread worker : workers) {
            worker.join(timeoutMillis);
        }
    }

    public List<WorkerThread> getWorkers() {
        return Collections.unmodifiableList(workers);
    }

    public int getRunningCount() {
        int count = 0;
        for (WorkerThread w : workers) {
            if (w.isAlive()) count++;
        }
        return count;
    }
}
