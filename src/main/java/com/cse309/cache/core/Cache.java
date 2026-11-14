package com.cse309.cache.core;

import com.cse309.cache.event.CacheEventListener;
import com.cse309.cache.eviction.EvictionPolicyType;
import com.cse309.cache.eviction.EvictionRecord;
import com.cse309.cache.exception.CacheException;
import com.cse309.cache.exception.InvalidCacheCapacityException;
import com.cse309.cache.stats.CacheStatistics;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Primary generic in-memory key-value cache interface.
 * Demonstrates generics with two type parameters (Unit 5).
 *
 * @param <K> the type of key
 * @param <V> the type of value
 */
public interface Cache<K, V> {

    /**
     * Puts a key-value pair using the cache-wide default TTL.
     */
    CacheWriteResult<K> put(K key, V value) throws CacheException;

    /**
     * Puts a key-value pair with an explicit TTL duration.
     */
    CacheWriteResult<K> put(K key, V value, Duration ttl) throws CacheException;

    /**
     * Puts a key-value pair that never expires.
     */
    CacheWriteResult<K> putWithoutExpiry(K key, V value) throws CacheException;

    /**
     * Retrieves the value associated with key.
     * Updates LRU recency and increments LFU frequency.
     */
    CacheLookupResult<V> get(K key);

    /**
     * Updates an existing key-value pair, preserving existing TTL.
     * Throws CacheKeyNotFoundException if key is absent or expired.
     */
    CacheWriteResult<K> update(K key, V value) throws CacheException;

    /**
     * Updates an existing key-value pair with a new TTL.
     * Throws CacheKeyNotFoundException if key is absent or expired.
     */
    CacheWriteResult<K> update(K key, V value, Duration ttl) throws CacheException;

    /**
     * Removes an entry from the cache.
     */
    CacheLookupResult<V> remove(K key);

    /**
     * Checks if key exists and is live. No side effects on access order or counters.
     */
    boolean containsKey(K key);

    /**
     * Queries an entry without updating recency, frequency, or hit/miss statistics.
     */
    CacheLookupResult<V> peek(K key);

    /**
     * Searches entries matching a key filter predicate.
     */
    List<CacheEntrySnapshot<K, V>> search(Predicate<? super K> keyFilter);

    /**
     * Returns an immutable snapshot list of all live entries.
     */
    List<CacheEntrySnapshot<K, V>> getAllEntries();

    /**
     * Clears all entries from the cache.
     */
    void clear();

    /**
     * Returns current number of live entries.
     */
    int size();

    /**
     * Checks whether the cache has no entries.
     */
    boolean isEmpty();

    /**
     * Returns the maximum entry capacity.
     */
    int getCapacity();

    /**
     * Updates cache capacity. If reduced below current size, evicts excess entries immediately.
     */
    List<EvictionRecord> setCapacity(int newCapacity) throws InvalidCacheCapacityException;

    /**
     * Returns the number of entries that would be evicted if capacity were reduced to newCapacity.
     */
    int previewCapacityChange(int newCapacity);

    /**
     * Atomically switches eviction policy and rebuilds tracking structures.
     */
    void setEvictionPolicy(EvictionPolicyType type);

    /**
     * Returns the currently active eviction policy type.
     */
    EvictionPolicyType getEvictionPolicyType();

    /**
     * Manually purges all currently expired entries.
     */
    List<K> purgeExpired();

    /**
     * Atomically restores / replaces all cache entries.
     */
    void replaceAll(List<CacheEntrySnapshot<K, V>> entries) throws CacheException;

    /**
     * Bulk inserts entries from a map.
     * Demonstrates wildcards: Map<? extends K, ? extends V> (Unit 5).
     */
    void putAll(Map<? extends K, ? extends V> entries) throws CacheException;

    /**
     * Registers a cache event listener.
     */
    void addListener(CacheEventListener listener);

    /**
     * Unregisters a cache event listener.
     */
    void removeListener(CacheEventListener listener);

    /**
     * Monotonically increasing counter incremented on every cache modification.
     */
    long getModificationCount();

    /**
     * Returns the statistics engine.
     */
    CacheStatistics getStatistics();

    /**
     * Returns the time clock.
     */
    Clock getClock();
}
