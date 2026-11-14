package com.cse309.cache.core;

import java.util.Objects;
import java.util.Optional;

/**
 * Result object returned by cache query operations (get, peek, remove).
 *
 * @param <V> the type of value
 */
public final class CacheLookupResult<V> {

    public enum Status {
        HIT,
        MISS,
        EXPIRED
    }

    private final Status status;
    private final V value;
    private final CacheEntrySnapshot<?, V> snapshot;

    private CacheLookupResult(Status status, V value, CacheEntrySnapshot<?, V> snapshot) {
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.value = value;
        this.snapshot = snapshot;
    }

    public static <V> CacheLookupResult<V> hit(V value, CacheEntrySnapshot<?, V> snapshot) {
        Objects.requireNonNull(value, "value cannot be null on HIT");
        return new CacheLookupResult<>(Status.HIT, value, snapshot);
    }

    public static <V> CacheLookupResult<V> miss() {
        return new CacheLookupResult<>(Status.MISS, null, null);
    }

    public static <V> CacheLookupResult<V> expired() {
        return new CacheLookupResult<>(Status.EXPIRED, null, null);
    }

    public static <V> CacheLookupResult<V> expired(CacheEntrySnapshot<?, V> snapshot) {
        return new CacheLookupResult<>(Status.EXPIRED, null, snapshot);
    }

    public Status getStatus() {
        return status;
    }

    public boolean isHit() {
        return status == Status.HIT;
    }

    public boolean isMiss() {
        return status == Status.MISS;
    }

    public boolean isExpired() {
        return status == Status.EXPIRED;
    }

    public Optional<V> getValue() {
        return Optional.ofNullable(value);
    }

    public V getValueOrNull() {
        return value;
    }

    public Optional<CacheEntrySnapshot<?, V>> getSnapshot() {
        return Optional.ofNullable(snapshot);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CacheLookupResult<?> that)) return false;
        return status == that.status &&
                Objects.equals(value, that.value) &&
                Objects.equals(snapshot, that.snapshot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, value, snapshot);
    }

    @Override
    public String toString() {
        return String.format("CacheLookupResult{status=%s, hasValue=%s}", status, value != null);
    }
}
