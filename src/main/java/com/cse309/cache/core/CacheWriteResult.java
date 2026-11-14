package com.cse309.cache.core;

import com.cse309.cache.eviction.EvictionRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Result object returned by cache mutation operations (put, update).
 *
 * @param <K> the type of key
 */
public final class CacheWriteResult<K> {

    public enum Status {
        INSERTED,
        REPLACED
    }

    private final Status status;
    private final K key;
    private final List<EvictionRecord> evictions;

    public CacheWriteResult(Status status, K key, List<EvictionRecord> evictions) {
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.key = Objects.requireNonNull(key, "key cannot be null");
        this.evictions = evictions != null
                ? Collections.unmodifiableList(new ArrayList<>(evictions))
                : Collections.emptyList();
    }

    public static <K> CacheWriteResult<K> inserted(K key, List<EvictionRecord> evictions) {
        return new CacheWriteResult<>(Status.INSERTED, key, evictions);
    }

    public static <K> CacheWriteResult<K> inserted(K key) {
        return new CacheWriteResult<>(Status.INSERTED, key, Collections.emptyList());
    }

    public static <K> CacheWriteResult<K> replaced(K key) {
        return new CacheWriteResult<>(Status.REPLACED, key, Collections.emptyList());
    }

    public Status getStatus() {
        return status;
    }

    public boolean isInserted() {
        return status == Status.INSERTED;
    }

    public boolean isReplaced() {
        return status == Status.REPLACED;
    }

    public K getKey() {
        return key;
    }

    public List<EvictionRecord> getEvictions() {
        return evictions;
    }

    public boolean hadEvictions() {
        return !evictions.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CacheWriteResult<?> that)) return false;
        return status == that.status &&
                key.equals(that.key) &&
                evictions.equals(that.evictions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, key, evictions);
    }

    @Override
    public String toString() {
        return String.format("CacheWriteResult{status=%s, key=%s, evictionsCount=%d}",
                status, key, evictions.size());
    }
}
