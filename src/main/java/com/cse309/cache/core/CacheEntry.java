package com.cse309.cache.core;

/**
 * Package-private mutable internal representation of an entry stored within the cache engine.
 * Never exposed outside the core package.
 *
 * @param <V> the type of the value
 */
final class CacheEntry<V> {

    private V value;
    private final long createdAt;
    private long lastAccessAt;
    private long expiresAt;
    private long accessCount;
    private long lastAccessTick;
    private final long insertionSequence;

    CacheEntry(V value, long createdAt, long expiresAt, long insertionSequence, long initialTick) {
        this.value = value;
        this.createdAt = createdAt;
        this.lastAccessAt = createdAt;
        this.expiresAt = expiresAt;
        this.accessCount = 1L;
        this.lastAccessTick = initialTick;
        this.insertionSequence = insertionSequence;
    }

    // Constructor for restoring from snapshot
    CacheEntry(V value,
               long createdAt,
               long lastAccessAt,
               long expiresAt,
               long accessCount,
               long lastAccessTick,
               long insertionSequence) {
        this.value = value;
        this.createdAt = createdAt;
        this.lastAccessAt = lastAccessAt;
        this.expiresAt = expiresAt;
        this.accessCount = accessCount;
        this.lastAccessTick = lastAccessTick;
        this.insertionSequence = insertionSequence;
    }

    V getValue() {
        return value;
    }

    void setValue(V value) {
        this.value = value;
    }

    long getCreatedAt() {
        return createdAt;
    }

    long getLastAccessAt() {
        return lastAccessAt;
    }

    long getExpiresAt() {
        return expiresAt;
    }

    void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    long getAccessCount() {
        return accessCount;
    }

    long getLastAccessTick() {
        return lastAccessTick;
    }

    long getInsertionSequence() {
        return insertionSequence;
    }

    boolean isExpired(long now) {
        return expiresAt != CacheDefaults.NO_EXPIRY_SENTINEL && now >= expiresAt;
    }

    void recordAccess(long now, long tick) {
        this.lastAccessAt = now;
        this.lastAccessTick = tick;
        this.accessCount++;
    }

    void update(V newValue, long now, long tick, Long updatedExpiresAt) {
        this.value = newValue;
        this.lastAccessAt = now;
        this.lastAccessTick = tick;
        this.accessCount++;
        if (updatedExpiresAt != null) {
            this.expiresAt = updatedExpiresAt;
        }
    }
}
