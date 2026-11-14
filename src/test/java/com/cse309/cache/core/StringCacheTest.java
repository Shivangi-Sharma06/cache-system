package com.cse309.cache.core;

import com.cse309.cache.eviction.EvictionPolicyType;
import com.cse309.cache.exception.InvalidCacheOperationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StringCacheTest {

    @Test
    void testBlankKeyThrows() {
        StringCache cache = new StringCache(10, EvictionPolicyType.LRU);
        assertThrows(InvalidCacheOperationException.class, () -> cache.put("", "val"));
        assertThrows(InvalidCacheOperationException.class, () -> cache.put("   ", "val"));
    }

    @Test
    void testOversizedKeyThrows() {
        StringCache cache = new StringCache(10, EvictionPolicyType.LRU);
        String longKey = "a".repeat(251);
        assertThrows(InvalidCacheOperationException.class, () -> cache.put(longKey, "val"));

        String validKey = "a".repeat(250);
        assertDoesNotThrow(() -> cache.put(validKey, "val"));
    }

    @Test
    void testOversizedValueThrows() {
        StringCache cache = new StringCache(10, EvictionPolicyType.LRU);
        String longVal = "x".repeat(8193);
        assertThrows(InvalidCacheOperationException.class, () -> cache.put("k", longVal));

        String validVal = "x".repeat(8192);
        assertDoesNotThrow(() -> cache.put("k", validVal));
    }
}
