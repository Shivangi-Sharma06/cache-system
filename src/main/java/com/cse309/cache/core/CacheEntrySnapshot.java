package com.cse309.cache.core;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable snapshot of a cache entry and its complete metadata.
 * Safe to share across threads and pass directly to UI components.
 *
 * @param <K> the type of key
 * @param <V> the type of value
 */
public final class CacheEntrySnapshot<K, V> implements Comparable<CacheEntrySnapshot<K, V>> {

    private final K key;
    private final V value;
    private final long createdAt;
    private final long lastAccessAt;
    private final long expiresAt;
    private final long accessCount;
    private final long insertionSequence;

    public CacheEntrySnapshot(K key,
                              V value,
                              long createdAt,
                              long lastAccessAt,
                              long expiresAt,
                              long accessCount,
                              long insertionSequence) {
        this.key = Objects.requireNonNull(key, "key cannot be null");
        this.value = Objects.requireNonNull(value, "value cannot be null");
        this.createdAt = createdAt;
        this.lastAccessAt = lastAccessAt;
        this.expiresAt = expiresAt;
        this.accessCount = accessCount;
        this.insertionSequence = insertionSequence;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public Instant getCreatedInstant() {
        return Instant.ofEpochMilli(createdAt);
    }

    public long getLastAccessAt() {
        return lastAccessAt;
    }

    public Instant getLastAccessInstant() {
        return Instant.ofEpochMilli(lastAccessAt);
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public Optional<Instant> getExpiresInstant() {
        return expiresAt == CacheDefaults.NO_EXPIRY_SENTINEL
                ? Optional.empty()
                : Optional.of(Instant.ofEpochMilli(expiresAt));
    }

    public boolean hasExpiry() {
        return expiresAt != CacheDefaults.NO_EXPIRY_SENTINEL;
    }

    public long getAccessCount() {
        return accessCount;
    }

    public long getInsertionSequence() {
        return insertionSequence;
    }

    public Optional<Duration> remainingTtl(Clock clock) {
        if (!hasExpiry()) {
            return Optional.empty();
        }
        long now = clock.millis();
        long remaining = Math.max(0L, expiresAt - now);
        return Optional.of(Duration.ofMillis(remaining));
    }

    public boolean isExpired(Clock clock) {
        return hasExpiry() && clock.millis() >= expiresAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CacheEntrySnapshot<?, ?> that)) return false;
        return createdAt == that.createdAt &&
                lastAccessAt == that.lastAccessAt &&
                expiresAt == that.expiresAt &&
                accessCount == that.accessCount &&
                insertionSequence == that.insertionSequence &&
                key.equals(that.key) &&
                value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value, createdAt, lastAccessAt, expiresAt, accessCount, insertionSequence);
    }

    @Override
    public String toString() {
        return String.format("CacheEntrySnapshot{key=%s, value=%s, seq=%d, freq=%d, expiresAt=%d}",
                key, value, insertionSequence, accessCount, expiresAt);
    }

    @Override
    @SuppressWarnings("unchecked")
    public int compareTo(CacheEntrySnapshot<K, V> other) {
        if (this.key instanceof Comparable<?> && other.key instanceof Comparable<?>) {
            int comp = ((Comparable<Object>) this.key).compareTo(other.key);
            if (comp != 0) return comp;
        }
        return Long.compare(this.insertionSequence, other.insertionSequence);
    }
}
