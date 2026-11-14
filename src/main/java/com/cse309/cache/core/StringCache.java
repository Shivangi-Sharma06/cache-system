package com.cse309.cache.core;

import com.cse309.cache.config.CacheConfiguration;
import com.cse309.cache.eviction.EvictionPolicyType;
import com.cse309.cache.exception.CacheException;
import com.cse309.cache.exception.InvalidCacheOperationException;
import com.cse309.cache.stats.CacheStatistics;

import java.time.Clock;
import java.time.Duration;

/**
 * Specialized String-to-String cache implementation for the desktop GUI.
 * Enforces key length (&lt;= 250 chars) and value length (&lt;= 8,192 chars) constraints.
 * Demonstrates multilevel inheritance: AbstractCache -&gt; ThreadSafeCache -&gt; StringCache (Unit 1).
 */
public class StringCache extends ThreadSafeCache<String, String> {

    public StringCache(CacheConfiguration config, Clock clock, CacheStatistics statistics) {
        super(config, clock, statistics);
    }

    public StringCache(int capacity, EvictionPolicyType policyType, Duration defaultTtl, Clock clock) {
        super(capacity, policyType, defaultTtl, clock);
    }

    public StringCache(int capacity, EvictionPolicyType policyType, Clock clock) {
        super(capacity, policyType, clock);
    }

    public StringCache(int capacity, EvictionPolicyType policyType) {
        super(capacity, policyType);
    }

    public StringCache() {
        super();
    }

    @Override
    protected void validateKeyAndValue(String key, String value) throws CacheException {
        super.validateKeyAndValue(key, value);

        if (key.trim().isEmpty()) {
            throw new InvalidCacheOperationException(
                    "String key cannot be empty or blank",
                    "Key cannot be empty or whitespace only.");
        }

        if (key.length() > CacheDefaults.MAX_STRING_KEY_LENGTH) {
            throw new InvalidCacheOperationException(
                    "Key length (" + key.length() + ") exceeds maximum limit of " + CacheDefaults.MAX_STRING_KEY_LENGTH,
                    "Key is too long (maximum " + CacheDefaults.MAX_STRING_KEY_LENGTH + " characters).");
        }

        if (value.length() > CacheDefaults.MAX_STRING_VALUE_LENGTH) {
            throw new InvalidCacheOperationException(
                    "Value length (" + value.length() + ") exceeds maximum limit of " + CacheDefaults.MAX_STRING_VALUE_LENGTH,
                    "Value is too long (maximum " + CacheDefaults.MAX_STRING_VALUE_LENGTH + " characters).");
        }
    }
}
