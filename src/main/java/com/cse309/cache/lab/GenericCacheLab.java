package com.cse309.cache.lab;

import com.cse309.cache.core.Cache;
import com.cse309.cache.core.CacheEntrySnapshot;
import com.cse309.cache.core.CacheFactory;
import com.cse309.cache.eviction.EvictionPolicyType;
import com.cse309.cache.exception.CacheException;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * Educational laboratory demonstrating the generic cache engine across diverse types.
 * Fulfills syllabus requirements for two type parameters, wildcards (Cache&lt;?, ?&gt;),
 * and bounded generic methods (&lt;K extends Comparable&lt;K&gt;, V&gt;) (Unit 5).
 */
public final class GenericCacheLab {

    private GenericCacheLab() {
        // Utility class
    }

    public static Cache<Integer, String> createIntegerStringCache(int capacity, Clock clock) {
        return CacheFactory.createGenericCache(capacity, EvictionPolicyType.LRU, clock);
    }

    public static Cache<String, Student> createStringStudentCache(int capacity, Clock clock) {
        return CacheFactory.createGenericCache(capacity, EvictionPolicyType.LRU, clock);
    }

    public static Cache<Long, Double> createLongDoubleCache(int capacity, Clock clock) {
        return CacheFactory.createGenericCache(capacity, EvictionPolicyType.LRU, clock);
    }

    /**
     * Demonstrates an unbounded wildcard parameter: {@code Cache<?, ?>} (Unit 5).
     *
     * @param cache any cache instance
     * @return list of formatted string summaries
     */
    public static List<String> inspectAllEntries(Cache<?, ?> cache) {
        if (cache == null) return List.of();
        List<String> lines = new ArrayList<>();
        for (CacheEntrySnapshot<?, ?> snapshot : cache.getAllEntries()) {
            lines.add(String.format("Key: %s | Value: %s | Freq: %d | Seq: %d",
                    snapshot.getKey(), snapshot.getValue(), snapshot.getAccessCount(), snapshot.getInsertionSequence()));
        }
        return lines;
    }

    /**
     * Demonstrates a bounded generic method: {@code <K extends Comparable<K>, V>} (Unit 5).
     * Finds the smallest key currently stored in the cache.
     *
     * @param cache cache instance whose keys are Comparable
     * @param <K> comparable key type
     * @param <V> value type
     * @return the smallest key, or null if cache is empty
     */
    public static <K extends Comparable<K>, V> K smallestKey(Cache<K, V> cache) {
        if (cache == null) return null;
        K smallest = null;
        for (CacheEntrySnapshot<K, V> entry : cache.getAllEntries()) {
            if (smallest == null || entry.getKey().compareTo(smallest) < 0) {
                smallest = entry.getKey();
            }
        }
        return smallest;
    }
}
