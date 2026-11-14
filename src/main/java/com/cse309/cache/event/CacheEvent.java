package com.cse309.cache.event;

import java.time.Instant;
import java.util.Objects;

/**
 * Event object dispatched when a cache lifecycle or mutation event occurs.
 * Follows the Delegation Event Model.
 */
public final class CacheEvent {

    public enum EventType {
        PUT,
        UPDATE,
        REMOVE,
        EVICT,
        EXPIRE,
        CLEAR,
        POLICY_CHANGE,
        CAPACITY_CHANGE
    }

    private final EventType type;
    private final Object key;
    private final Object details;
    private final long timestamp;
    private final String threadName;

    public CacheEvent(EventType type, Object key, Object details, long timestamp, String threadName) {
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.key = key;
        this.details = details;
        this.timestamp = timestamp;
        this.threadName = threadName != null ? threadName : Thread.currentThread().getName();
    }

    public CacheEvent(EventType type, Object key, Object details) {
        this(type, key, details, System.currentTimeMillis(), Thread.currentThread().getName());
    }

    public EventType getType() {
        return type;
    }

    public Object getKey() {
        return key;
    }

    public Object getDetails() {
        return details;
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

    @Override
    public String toString() {
        return String.format("[%s] Event %s for key=%s (details=%s) on thread=%s",
                getInstant(), type, key, details, threadName);
    }
}
