package com.cse309.cache.eviction;

import com.cse309.cache.core.CacheWriteResult;
import com.cse309.cache.core.ThreadSafeCache;
import com.cse309.cache.exception.CacheException;
import com.cse309.cache.testutil.MutableClock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvictionPoliciesWorkedExamplesTest {

    @Test
    void testLruWorkedExample() throws CacheException {
        // Capacity 3: put A, B, C; get A; put D. B is evicted.
        MutableClock clock = new MutableClock();
        ThreadSafeCache<String, String> cache = new ThreadSafeCache<>(3, EvictionPolicyType.LRU, clock);

        cache.put("A", "1");
        cache.put("B", "2");
        cache.put("C", "3");

        // get A -> A becomes most recent, B is least recent
        cache.get("A");

        CacheWriteResult<String> putD = cache.put("D", "4");
        assertTrue(putD.hadEvictions());
        assertEquals("B", putD.getEvictions().get(0).getKey());

        assertFalse(cache.containsKey("B"));
        assertTrue(cache.containsKey("A"));
        assertTrue(cache.containsKey("C"));
        assertTrue(cache.containsKey("D"));
    }

    @Test
    void testFifoWorkedExample() throws CacheException {
        // Capacity 3: put A, B, C; get A; put D. A is evicted. Then put B again (overwrite): B keeps its original position.
        MutableClock clock = new MutableClock();
        ThreadSafeCache<String, String> cache = new ThreadSafeCache<>(3, EvictionPolicyType.FIFO, clock);

        cache.put("A", "1");
        cache.put("B", "2");
        cache.put("C", "3");

        // get A -> in FIFO, reads have no effect!
        cache.get("A");

        // put D -> A was inserted earliest, so A is evicted
        CacheWriteResult<String> putD = cache.put("D", "4");
        assertTrue(putD.hadEvictions());
        assertEquals("A", putD.getEvictions().get(0).getKey());
        assertFalse(cache.containsKey("A"));

        // Overwrite B: B keeps its original position in queue
        cache.put("B", "2_updated");

        // Now cache has [B, C, D] where B is still earliest!
        // Putting E should evict B!
        CacheWriteResult<String> putE = cache.put("E", "5");
        assertTrue(putE.hadEvictions());
        assertEquals("B", putE.getEvictions().get(0).getKey());
        assertFalse(cache.containsKey("B"));
    }

    @Test
    void testLfuWorkedExample() throws CacheException {
        // Capacity 3: put A, B, C; get A twice; get B once; put D. C is evicted (frequency 1 against 3 and 2).
        MutableClock clock = new MutableClock();
        ThreadSafeCache<String, String> cache = new ThreadSafeCache<>(3, EvictionPolicyType.LFU, clock);

        cache.put("A", "1"); // freq=1
        cache.put("B", "2"); // freq=1
        cache.put("C", "3"); // freq=1

        cache.get("A"); // freq=2
        cache.get("A"); // freq=3

        cache.get("B"); // freq=2
        // C remains freq=1

        CacheWriteResult<String> putD = cache.put("D", "4");
        assertTrue(putD.hadEvictions());
        assertEquals("C", putD.getEvictions().get(0).getKey());

        assertFalse(cache.containsKey("C"));
        assertTrue(cache.containsKey("A"));
        assertTrue(cache.containsKey("B"));
        assertTrue(cache.containsKey("D"));
    }

    @Test
    void testLfuTieBreakWorkedExample() throws CacheException {
        // Capacity 3: put A, B, C with no reads; put D. A is evicted (equal frequency, earliest insertion).
        MutableClock clock = new MutableClock();
        ThreadSafeCache<String, String> cache = new ThreadSafeCache<>(3, EvictionPolicyType.LFU, clock);

        cache.put("A", "1"); // seq=1, freq=1
        cache.put("B", "2"); // seq=2, freq=1
        cache.put("C", "3"); // seq=3, freq=1

        CacheWriteResult<String> putD = cache.put("D", "4");
        assertTrue(putD.hadEvictions());
        assertEquals("A", putD.getEvictions().get(0).getKey());

        assertFalse(cache.containsKey("A"));
        assertTrue(cache.containsKey("B"));
        assertTrue(cache.containsKey("C"));
        assertTrue(cache.containsKey("D"));
    }
}
