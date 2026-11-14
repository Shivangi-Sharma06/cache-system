package com.cse309.cache.core;

import com.cse309.cache.eviction.EvictionPolicyType;
import com.cse309.cache.exception.CacheException;
import com.cse309.cache.exception.CacheKeyNotFoundException;
import com.cse309.cache.exception.InvalidCacheOperationException;
import com.cse309.cache.testutil.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreadSafeCacheTest {

    private MutableClock clock;
    private ThreadSafeCache<String, String> cache;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(1_000_000L);
        cache = new ThreadSafeCache<>(3, EvictionPolicyType.LRU, Duration.ofSeconds(60), clock);
    }

    @Test
    void testGetOnLiveKey() throws CacheException {
        cache.put("k1", "v1");
        CacheLookupResult<String> result = cache.get("k1");

        assertTrue(result.isHit());
        assertEquals("v1", result.getValueOrNull());
        assertNotNull(result.getSnapshot().orElse(null));
        assertEquals(2L, result.getSnapshot().get().getAccessCount()); // 1 on put, 1 on get
    }

    @Test
    void testGetOnMissingKey() {
        CacheLookupResult<String> result = cache.get("missing");
        assertTrue(result.isMiss());
        assertFalse(result.isHit());
        assertFalse(result.isExpired());
    }

    @Test
    void testGetOnExpiredKey() throws CacheException {
        cache.put("k1", "v1", Duration.ofSeconds(10));
        clock.advance(Duration.ofSeconds(11));

        CacheLookupResult<String> result = cache.get("k1");
        assertTrue(result.isExpired());
        assertFalse(result.isHit());

        // Entry should now be removed lazily
        assertFalse(cache.containsKey("k1"));
        assertEquals(0, cache.size());
    }

    @Test
    void testPutNewKeyAndEviction() throws CacheException {
        CacheWriteResult<String> r1 = cache.put("k1", "v1");
        assertTrue(r1.isInserted());
        assertFalse(r1.hadEvictions());

        cache.put("k2", "v2");
        cache.put("k3", "v3");
        assertEquals(3, cache.size());

        // Putting 4th key into capacity 3 should evict least recently used (k1)
        CacheWriteResult<String> r4 = cache.put("k4", "v4");
        assertTrue(r4.isInserted());
        assertTrue(r4.hadEvictions());
        assertEquals(1, r4.getEvictions().size());
        assertEquals("k1", r4.getEvictions().get(0).getKey());
    }

    @Test
    void testPutExistingKeyReplaces() throws CacheException {
        cache.put("k1", "v1");
        CacheWriteResult<String> r2 = cache.put("k1", "v1_updated");

        assertTrue(r2.isReplaced());
        assertFalse(r2.hadEvictions());
        assertEquals("v1_updated", cache.get("k1").getValueOrNull());
        assertEquals(1, cache.size());
    }

    @Test
    void testUpdateExistingKey() throws CacheException {
        cache.put("k1", "v1");
        CacheWriteResult<String> r = cache.update("k1", "v1_mod");

        assertTrue(r.isReplaced());
        assertEquals("v1_mod", cache.get("k1").getValueOrNull());
    }

    @Test
    void testUpdateMissingKeyThrows() {
        assertThrows(CacheKeyNotFoundException.class, () -> cache.update("absent", "val"));
    }

    @Test
    void testUpdateExpiredKeyThrows() throws CacheException {
        cache.put("expKey", "val", Duration.ofSeconds(5));
        clock.advance(Duration.ofSeconds(6));

        assertThrows(CacheKeyNotFoundException.class, () -> cache.update("expKey", "newVal"));
    }

    @Test
    void testRemove() throws CacheException {
        cache.put("k1", "v1");
        CacheLookupResult<String> rem = cache.remove("k1");
        assertTrue(rem.isHit());
        assertEquals("v1", rem.getValueOrNull());
        assertEquals(0, cache.size());

        CacheLookupResult<String> rem2 = cache.remove("k1");
        assertTrue(rem2.isMiss());
    }

    @Test
    void testPeekAndContainsKeyHaveNoSideEffects() throws CacheException {
        cache.put("k1", "v1");
        long initialFreq = cache.peek("k1").getSnapshot().get().getAccessCount();

        assertTrue(cache.containsKey("k1"));
        CacheLookupResult<String> peekRes = cache.peek("k1");
        assertTrue(peekRes.isHit());

        // Access frequency should remain unchanged
        assertEquals(initialFreq, peekRes.getSnapshot().get().getAccessCount());
    }

    @Test
    void testSearchAndGetAllEntries() throws CacheException {
        cache.put("user:1", "Alice");
        cache.put("user:2", "Bob");
        cache.put("order:1", "Item1");

        List<CacheEntrySnapshot<String, String>> users = cache.search(k -> k.startsWith("user:"));
        assertEquals(2, users.size());

        List<CacheEntrySnapshot<String, String>> all = cache.getAllEntries();
        assertEquals(3, all.size());
    }

    @Test
    void testClear() throws CacheException {
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        assertEquals(2, cache.size());

        cache.clear();
        assertEquals(0, cache.size());
        assertTrue(cache.isEmpty());
    }

    @Test
    void testValidationErrors() {
        assertThrows(InvalidCacheOperationException.class, () -> cache.put(null, "val"));
        assertThrows(InvalidCacheOperationException.class, () -> cache.put("k", null));
        assertThrows(InvalidCacheOperationException.class, () -> cache.put("k", "v", Duration.ofSeconds(-1)));
    }
}
