package com.cse309.cache.eviction;

import com.cse309.cache.core.CacheEntrySnapshot;

import java.util.List;

/**
 * Strategy interface defining eviction policy algorithms.
 * Implementations track keys and ordering metadata to identify candidate victims for eviction.
 *
 * @param <K> the type of key
 */
public interface EvictionPolicy<K> {

    /**
     * Called when a new key is inserted into the cache.
     *
     * @param key the key inserted
     * @param sequence monotonic insertion sequence counter
     */
    void onInsert(K key, long sequence);

    /**
     * Called when an existing key is accessed or read.
     *
     * @param key the key accessed
     */
    void onAccess(K key);

    /**
     * Called when a key is removed or evicted from the cache.
     *
     * @param key the key removed
     */
    void onRemove(K key);

    /**
     * Selects a candidate key for eviction without removing it.
     *
     * @return the victim key, or null if the policy contains no entries
     */
    K selectVictim();

    /**
     * Rebuilds the policy state from a collection of active entry snapshots.
     *
     * @param entries existing cache entry snapshots
     */
    void rebuild(List<CacheEntrySnapshot<K, ?>> entries);

    /**
     * Produces a human-readable explanation of why this key is selected as victim.
     *
     * @param key the victim key
     * @return human explanation string
     */
    String describeVictim(K key);

    /**
     * Clears all internal tracking state.
     */
    void clear();

    /**
     * Returns the number of keys currently tracked by this policy.
     *
     * @return key count
     */
    int size();

    /**
     * Returns the eviction policy type.
     *
     * @return policy type enum
     */
    EvictionPolicyType getType();
}
