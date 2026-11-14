package com.cse309.cache.expiry;

import com.cse309.cache.core.CacheLookupResult;
import com.cse309.cache.core.CacheWriteResult;
import com.cse309.cache.core.ThreadSafeCache;
import com.cse309.cache.eviction.EvictionPolicyType;
import com.cse309.cache.exception.CacheException;
import com.cse309.cache.testutil.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpirationTest {

    private MutableClock clock;
    private ThreadSafeCache<String, String> cache;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(1_000_000L);
        cache = new ThreadSafeCache<>(3, EvictionPolicyType.LRU, Duration.ofSeconds(30), clock);
    }

    @Test
    void testDefaultTtl() throws CacheException {
        cache.put("k1", "v1");
        clock.advance(Duration.ofSeconds(29));
        assertTrue(cache.containsKey("k1"));

        clock.advance(Duration.ofSeconds(2)); // Now 31s elapsed
        assertFalse(cache.containsKey("k1"));
    }

    @Test
    void testCustomPerEntryTtl() throws CacheException {
        cache.put("shortLived", "v1", Duration.ofSeconds(5));
        cache.put("longLived", "v2", Duration.ofSeconds(60));

        clock.advance(Duration.ofSeconds(10));
        assertFalse(cache.containsKey("shortLived"));
        assertTrue(cache.containsKey("longLived"));
    }

    @Test
    void testNeverExpiringEntry() throws CacheException {
        cache.putWithoutExpiry("permanent", "vPermanent");
        clock.advance(Duration.ofDays(365));

        assertTrue(cache.containsKey("permanent"));
        CacheLookupResult<String> res = cache.get("permanent");
        assertTrue(res.isHit());
    }

    @Test
    void testActivePurgeExpired() throws CacheException {
        cache.put("k1", "v1", Duration.ofSeconds(10));
        cache.put("k2", "v2", Duration.ofSeconds(20));
        cache.put("k3", "v3", Duration.ofSeconds(30));

        clock.advance(Duration.ofSeconds(15)); // k1 expired
        List<String> purged = cache.purgeExpired();

        assertEquals(1, purged.size());
        assertEquals("k1", purged.get(0));
        assertEquals(2, cache.size());
    }

    @Test
    void testExpiredEntriesEvictedBeforeLiveEntriesOnPut() throws CacheException {
        // Cache capacity 2
        ThreadSafeCache<String, String> smallCache = new ThreadSafeCache<>(2, EvictionPolicyType.LRU, clock);
        smallCache.put("liveKey", "vLive", Duration.ofMinutes(10));
        smallCache.put("expiredKey", "vExp", Duration.ofSeconds(5));

        clock.advance(Duration.ofSeconds(6)); // expiredKey is now expired

        // Putting a new key should purge expiredKey first, so liveKey is NOT evicted!
        CacheWriteResult<String> putResult = smallCache.put("newKey", "vNew");
        assertTrue(putResult.isInserted());
        assertFalse(putResult.hadEvictions()); // Counted as expiration, not eviction!

        assertTrue(smallCache.containsKey("liveKey"));
        assertTrue(smallCache.containsKey("newKey"));
        assertFalse(smallCache.containsKey("expiredKey"));
    }
}
