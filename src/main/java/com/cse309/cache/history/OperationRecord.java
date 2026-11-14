package com.cse309.cache.history;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable log entry capturing an executed cache operation.
 * Overrides equals, hashCode, toString, and implements Comparable (Unit 2).
 */
public final class OperationRecord implements Comparable<OperationRecord> {

    private final long timestamp;
    private final String threadName;
    private final String operation;
    private final String cacheKey;
    private final String outcome;
    private final long latencyNanos;
    private final String details;

    public OperationRecord(long timestamp,
                           String threadName,
                           String operation,
                           String cacheKey,
                           String outcome,
                           long latencyNanos,
                           String details) {
        this.timestamp = timestamp;
        this.threadName = threadName != null ? threadName : Thread.currentThread().getName();
        this.operation = Objects.requireNonNull(operation, "operation cannot be null");
        this.cacheKey = cacheKey != null ? cacheKey : "";
        this.outcome = Objects.requireNonNull(outcome, "outcome cannot be null");
        this.latencyNanos = latencyNanos;
        this.details = details != null ? details : "";
    }

    public static OperationRecord create(String operation, String cacheKey, String outcome, long latencyNanos, String details) {
        return new OperationRecord(
                System.currentTimeMillis(),
                Thread.currentThread().getName(),
                operation,
                cacheKey,
                outcome,
                latencyNanos,
                details
        );
    }

    public static OperationRecord create(String operation, String outcome, String details) {
        return create(operation, "", outcome, 0L, details);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Instant getInstant() {
        return Instant.ofEpochMilli(timestamp);
    }

    public String getThreadName() {
        return threadName;
    }

    public String getOperation() {
        return operation;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public String getOutcome() {
        return outcome;
    }

    public long getLatencyNanos() {
        return latencyNanos;
    }

    public double getLatencyMicros() {
        return latencyNanos / 1_000.0;
    }

    public String getDetails() {
        return details;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OperationRecord that)) return false;
        return timestamp == that.timestamp &&
                latencyNanos == that.latencyNanos &&
                threadName.equals(that.threadName) &&
                operation.equals(that.operation) &&
                cacheKey.equals(that.cacheKey) &&
                outcome.equals(that.outcome) &&
                details.equals(that.details);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, threadName, operation, cacheKey, outcome, latencyNanos, details);
    }

    @Override
    public String toString() {
        return String.format("[%s] [%s] %s key='%s' -> %s (%d ns) %s",
                getInstant(), threadName, operation, cacheKey, outcome, latencyNanos, details);
    }

    @Override
    public int compareTo(OperationRecord other) {
        int timeComp = Long.compare(this.timestamp, other.timestamp);
        if (timeComp != 0) return timeComp;
        return Long.compare(this.latencyNanos, other.latencyNanos);
    }
}
