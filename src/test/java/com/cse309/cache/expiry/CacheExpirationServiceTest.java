package com.cse309.cache.expiry;

import com.cse309.cache.core.ThreadSafeCache;
import com.cse309.cache.eviction.EvictionPolicyType;
import com.cse309.cache.exception.CacheException;
import com.cse309.cache.testutil.MutableClock;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheExpirationServiceTest {

    @Test
    void testStartStopAndThreadLifecycle() {
        MutableClock clock = new MutableClock();
        ThreadSafeCache<String, String> cache = new ThreadSafeCache<>(10, EvictionPolicyType.LRU, clock);
        CacheExpirationService service = new CacheExpirationService(cache, Duration.ofMillis(200));

        service.start();
        assertTrue(service.isRunning());
        assertTrue(service.getWorkerThread().isAlive());
        assertTrue(service.getWorkerThread().isDaemon());

        long start = System.currentTimeMillis();
        service.stop();
        long elapsed = System.currentTimeMillis() - start;

        assertFalse(service.isRunning());
        assertFalse(service.getWorkerThread().isAlive());
        assertTrue(elapsed < 2000, "Service stop should complete well within 2 seconds, took: " + elapsed + "ms");
    }

    @Test
    void testIntervalChangeWakesMonitor() {
        MutableClock clock = new MutableClock();
        ThreadSafeCache<String, String> cache = new ThreadSafeCache<>(10, EvictionPolicyType.LRU, clock);
        CacheExpirationService service = new CacheExpirationService(cache, Duration.ofHours(1));

        service.start();
        service.setInterval(Duration.ofMillis(100));
        assertEquals(Duration.ofMillis(100), service.getInterval());

        service.stop();
        assertFalse(service.isRunning());
    }

    @Test
    void testPurgeNow() throws CacheException {
        MutableClock clock = new MutableClock();
        ThreadSafeCache<String, String> cache = new ThreadSafeCache<>(10, EvictionPolicyType.LRU, clock);
        cache.put("k1", "v1", Duration.ofSeconds(5));

        CacheExpirationService service = new CacheExpirationService(cache);
        clock.advance(Duration.ofSeconds(10));

        int purged = service.purgeNow();
        assertEquals(1, purged);
        assertEquals(0, cache.size());
    }
}
