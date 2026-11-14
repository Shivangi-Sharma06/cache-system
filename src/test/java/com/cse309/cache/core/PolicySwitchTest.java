package com.cse309.cache.core;

import com.cse309.cache.eviction.EvictionPolicyType;
import com.cse309.cache.exception.CacheException;
import com.cse309.cache.testutil.MutableClock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicySwitchTest {

    @Test
    void testPolicySwitchRetainsAllEntries() throws CacheException {
        MutableClock clock = new MutableClock();
        ThreadSafeCache<String, String> cache = new ThreadSafeCache<>(5, EvictionPolicyType.LRU, clock);

        cache.put("k1", "v1");
        cache.put("k2", "v2");
        cache.put("k3", "v3");

        assertEquals(EvictionPolicyType.LRU, cache.getEvictionPolicyType());
        assertEquals(3, cache.size());

        // Switch to FIFO
        cache.setEvictionPolicy(EvictionPolicyType.FIFO);
        assertEquals(EvictionPolicyType.FIFO, cache.getEvictionPolicyType());
        assertEquals(3, cache.size());
        assertTrue(cache.containsKey("k1"));
        assertTrue(cache.containsKey("k2"));
        assertTrue(cache.containsKey("k3"));

        // Switch to LFU
        cache.setEvictionPolicy(EvictionPolicyType.LFU);
        assertEquals(EvictionPolicyType.LFU, cache.getEvictionPolicyType());
        assertEquals(3, cache.size());

        List<CacheEntrySnapshot<String, String>> entries = cache.getAllEntries();
        assertEquals(3, entries.size());
    }
}
