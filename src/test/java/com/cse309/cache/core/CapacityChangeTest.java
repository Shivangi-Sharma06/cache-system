package com.cse309.cache.core;

import com.cse309.cache.eviction.EvictionPolicyType;
import com.cse309.cache.eviction.EvictionRecord;
import com.cse309.cache.exception.CacheException;
import com.cse309.cache.exception.InvalidCacheCapacityException;
import com.cse309.cache.testutil.MutableClock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapacityChangeTest {

    @Test
    void testCapacityIncrease() throws CacheException {
        ThreadSafeCache<String, String> cache = new ThreadSafeCache<>(3, EvictionPolicyType.LRU);
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        cache.put("k3", "v3");

        List<EvictionRecord> evictions = cache.setCapacity(5);
        assertTrue(evictions.isEmpty());
        assertEquals(5, cache.getCapacity());
        assertEquals(3, cache.size());
    }

    @Test
    void testCapacityReductionEvictsExcessAtomically() throws CacheException {
        MutableClock clock = new MutableClock();
        ThreadSafeCache<String, String> cache = new ThreadSafeCache<>(5, EvictionPolicyType.LRU, clock);

        cache.put("A", "1");
        cache.put("B", "2");
        cache.put("C", "3");
        cache.put("D", "4");
        cache.put("E", "5");

        // Preview reduction to 3
        int preview = cache.previewCapacityChange(3);
        assertEquals(2, preview);

        // Access D and E so A, B, C are older
        cache.get("D");
        cache.get("E");
        cache.get("C"); // Order oldest to newest: A, B, D, E, C

        List<EvictionRecord> evictions = cache.setCapacity(3);
        assertEquals(2, evictions.size());
        assertEquals(3, cache.size());
        assertEquals(3, cache.getCapacity());

        // Reason should be CAPACITY_REDUCED
        assertEquals(EvictionRecord.EvictionReason.CAPACITY_REDUCED, evictions.get(0).getReason());
        assertEquals(EvictionRecord.EvictionReason.CAPACITY_REDUCED, evictions.get(1).getReason());

        // A and B should have been evicted
        assertFalse(cache.containsKey("A"));
        assertFalse(cache.containsKey("B"));
        assertTrue(cache.containsKey("C"));
        assertTrue(cache.containsKey("D"));
        assertTrue(cache.containsKey("E"));
    }

    @Test
    void testInvalidCapacityThrows() {
        ThreadSafeCache<String, String> cache = new ThreadSafeCache<>(10, EvictionPolicyType.LRU);
        assertThrows(InvalidCacheCapacityException.class, () -> cache.setCapacity(0));
        assertThrows(InvalidCacheCapacityException.class, () -> cache.setCapacity(1_000_001));
    }
}
