package com.cse309.cache.simulation;

import com.cse309.cache.threading.BoundedBuffer;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Producer task generating planned operations into the shared BoundedBuffer.
 * Implements Runnable (Unit 3).
 */
public final class OperationProducerTask implements Runnable {

    private final SimulationConfig config;
    private final BoundedBuffer<PlannedOperation> buffer;
    private final AtomicInteger generatedCount;
    private volatile boolean cancelled = false;

    public OperationProducerTask(SimulationConfig config,
                                 BoundedBuffer<PlannedOperation> buffer,
                                 AtomicInteger generatedCount) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.buffer = Objects.requireNonNull(buffer, "buffer cannot be null");
        this.generatedCount = Objects.requireNonNull(generatedCount, "generatedCount cannot be null");
    }

    @Override
    public void run() {
        OperationGenerator generator = new OperationGenerator(config);
        int total = config.getTotalOperations();

        try {
            for (int i = 1; i <= total && !cancelled; i++) {
                PlannedOperation op = generator.nextOperation(i);
                buffer.put(op);
                generatedCount.incrementAndGet();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IllegalStateException e) {
            // Buffer was closed by cancellation; exit gracefully
        } finally {
            buffer.close();
        }
    }

    public void cancel() {
        this.cancelled = true;
    }
}
