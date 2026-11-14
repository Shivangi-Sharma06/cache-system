package com.cse309.cache.core;

import com.cse309.cache.config.CacheConfiguration;
import com.cse309.cache.eviction.EvictionPolicyType;
import com.cse309.cache.stats.CacheStatistics;

import java.time.Clock;
import java.time.Duration;

/**
 * Static factory methods for creating configured Cache instances.
 * Demonstrates static factory pattern (Unit 1).
 */
public final class CacheFactory {

    private CacheFactory() {
        // Prevent instantiation
    }

    public static StringCache createStringCache(CacheConfiguration config, Clock clock) {
        return new StringCache(config, clock, new CacheStatistics());
    }

    public static StringCache createStringCache(CacheConfiguration config) {
        return createStringCache(config, Clock.systemUTC());
    }

    public static StringCache createStringCache() {
        return createStringCache(new CacheConfiguration());
    }

    public static <K, V> Cache<K, V> createGenericCache(CacheConfiguration config, Clock clock) {
        return new ThreadSafeCache<>(config, clock, new CacheStatistics());
    }

    public static <K, V> Cache<K, V> createGenericCache(int capacity,
                                                        EvictionPolicyType policy,
                                                        Duration defaultTtl,
                                                        Clock clock) {
        return new ThreadSafeCache<>(capacity, policy, defaultTtl, clock);
    }

    public static <K, V> Cache<K, V> createGenericCache(int capacity, EvictionPolicyType policy, Clock clock) {
        return new ThreadSafeCache<>(capacity, policy, clock);
    }
}
