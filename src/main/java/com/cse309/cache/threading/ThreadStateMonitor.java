package com.cse309.cache.threading;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Monitors and inspects worker thread states.
 * Fulfills syllabus requirement for Thread.getState() inspection (NEW, RUNNABLE, BLOCKED,
 * WAITING, TIMED_WAITING, TERMINATED) (Unit 3).
 */
public final class ThreadStateMonitor {

    public record WorkerStatus(
            String name,
            int priority,
            Thread.State state,
            int operationsProcessed,
            boolean alive,
            boolean failed
    ) {
        @Override
        public String toString() {
            return String.format("%s (pri=%d) [%s] - %d ops %s",
                    name, priority, state, operationsProcessed, failed ? "[FAILED]" : (alive ? "[ALIVE]" : "[STOPPED]"));
        }
    }

    private ThreadStateMonitor() {
        // Utility class
    }

    public static List<WorkerStatus> inspectWorkers(List<WorkerThread> workers) {
        if (workers == null || workers.isEmpty()) {
            return Collections.emptyList();
        }

        List<WorkerStatus> statuses = new ArrayList<>(workers.size());
        for (WorkerThread worker : workers) {
            statuses.add(new WorkerStatus(
                    worker.getName(),
                    worker.getPriority(),
                    worker.getState(), // Real Thread.getState() call (Unit 3)
                    worker.getOperationsProcessedByThisWorker(),
                    worker.isAlive(),
                    worker.hasFailed()
            ));
        }
        return Collections.unmodifiableList(statuses);
    }
}
