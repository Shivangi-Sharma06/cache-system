package com.cse309.cache.core;

import java.time.Duration;

/**
 * Global default values and operational boundaries for the caching system.
 */
public final class CacheDefaults {

    public static final int DEFAULT_CAPACITY = 100;
    public static final int MIN_CAPACITY = 1;
    public static final int MAX_CAPACITY = 1_000_000;

    public static final Duration DEFAULT_TTL = Duration.ofSeconds(300);
    public static final Duration MIN_TTL = Duration.ofSeconds(1);
    public static final Duration MAX_TTL = Duration.ofDays(365);

    public static final Duration DEFAULT_CLEANUP_INTERVAL = Duration.ofSeconds(5);
    public static final Duration MIN_CLEANUP_INTERVAL = Duration.ofMillis(100);
    public static final Duration MAX_CLEANUP_INTERVAL = Duration.ofHours(1);

    public static final int DEFAULT_SIMULATOR_WORKERS = 4;
    public static final int MIN_WORKERS = 1;
    public static final int MAX_WORKERS = 64;

    public static final int DEFAULT_HISTORY_CAPACITY = 1000;
    public static final int MIN_HISTORY_CAPACITY = 100;
    public static final int MAX_HISTORY_CAPACITY = 10_000;

    public static final int MAX_STRING_KEY_LENGTH = 250;
    public static final int MAX_STRING_VALUE_LENGTH = 8192;

    public static final long NO_EXPIRY_SENTINEL = -1L;

    private CacheDefaults() {
        // Prevent instantiation
    }
}
