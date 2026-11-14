package com.cse309.cache.threading;

import com.cse309.cache.core.Cache;
import com.cse309.cache.exception.CacheException;
import com.cse309.cache.history.OperationHistory;
import com.cse309.cache.history.OperationRecord;
import com.cse309.cache.logging.AppLogging;
import com.cse309.cache.simulation.PlannedOperation;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Worker thread executing planned cache operations taken from a shared BoundedBuffer.
 * Extends Thread (Unit 3). Installs custom UncaughtExceptionHandler (Unit 2).
 */
public final class WorkerThread extends Thread {

    private static final Logger LOGGER = AppLogging.getLogger(WorkerThread.class);

    private final Cache<String, String> cache;
    private final BoundedBuffer<PlannedOperation> buffer;
    private final PauseGate pauseGate;
    private final OperationHistory history;
    private final boolean logEveryOperation;

    private final AtomicInteger completedCount;
    private final AtomicInteger successCount;
    private final AtomicInteger failureCount;

    private volatile boolean failed = false;
    private volatile int operationsProcessedByThisWorker = 0;

    public WorkerThread(String name,
                        Cache<String, String> cache,
                        BoundedBuffer<PlannedOperation> buffer,
                        PauseGate pauseGate,
                        OperationHistory history,
                        boolean logEveryOperation,
                        AtomicInteger completedCount,
                        AtomicInteger successCount,
                        AtomicInteger failureCount) {
        super(name);
        this.cache = Objects.requireNonNull(cache, "cache cannot be null");
        this.buffer = Objects.requireNonNull(buffer, "buffer cannot be null");
        this.pauseGate = Objects.requireNonNull(pauseGate, "pauseGate cannot be null");
        this.history = history;
        this.logEveryOperation = logEveryOperation;
        this.completedCount = Objects.requireNonNull(completedCount, "completedCount cannot be null");
        this.successCount = Objects.requireNonNull(successCount, "successCount cannot be null");
        this.failureCount = Objects.requireNonNull(failureCount, "failureCount cannot be null");

        // Per-worker UncaughtExceptionHandler (Syllabus U2)
        setUncaughtExceptionHandler((t, e) -> {
            this.failed = true;
            LOGGER.log(Level.SEVERE, "Worker " + t.getName() + " encountered fatal uncaught exception: " + e.getMessage(), e);
        });
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                pauseGate.await();

                PlannedOperation op = buffer.take();
                if (op == null) {
                    // Buffer closed and drained
                    break;
                }

                executeOperation(op);
                operationsProcessedByThisWorker++;
                completedCount.incrementAndGet();
            }
        } catch (InterruptedException e) {
            interrupt();
        }
    }

    private void executeOperation(PlannedOperation op) {
        long start = System.nanoTime();
        boolean success = false;
        String outcome = "UNKNOWN";

        try {
            switch (op.getType()) {
                case GET -> {
                    var res = cache.get(op.getKey());
                    outcome = res.getStatus().name();
                    success = true;
                }
                case PUT -> {
                    var res = op.getTtl() != null
                            ? cache.put(op.getKey(), op.getValue(), op.getTtl())
                            : cache.put(op.getKey(), op.getValue());
                    outcome = res.getStatus().name();
                    success = true;
                }
                case UPDATE -> {
                    var res = cache.update(op.getKey(), op.getValue());
                    outcome = res.getStatus().name();
                    success = true;
                }
                case DELETE -> {
                    var res = cache.remove(op.getKey());
                    outcome = res.getStatus().name();
                    success = true;
                }
                case SEARCH -> {
                    var res = cache.search(k -> k.equals(op.getKey()));
                    outcome = "MATCHES_" + res.size();
                    success = true;
                }
            }
            successCount.incrementAndGet();
        } catch (CacheException ce) {
            outcome = "FAILED: " + ce.getClass().getSimpleName();
            failureCount.incrementAndGet();
        } catch (Exception e) {
            outcome = "UNEXPECTED: " + e.getClass().getSimpleName();
            failureCount.incrementAndGet();
            LOGGER.log(Level.WARNING, "Unexpected error in worker " + getName() + ": " + e.getMessage(), e);
        } finally {
            long latency = System.nanoTime() - start;
            if (logEveryOperation && history != null) {
                history.record(new OperationRecord(
                        System.currentTimeMillis(),
                        getName(),
                        op.getType().name(),
                        op.getKey(),
                        outcome,
                        latency,
                        "len=" + (op.getValue() != null ? op.getValue().length() : 0)
                ));
            }
        }
    }

    public boolean hasFailed() {
        return failed;
    }

    public int getOperationsProcessedByThisWorker() {
        return operationsProcessedByThisWorker;
    }
}
