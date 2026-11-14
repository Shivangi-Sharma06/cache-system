package com.cse309.cache.stats;

import com.cse309.cache.core.ThreadSafeCache;
import com.cse309.cache.eviction.EvictionPolicyType;
import com.cse309.cache.exception.CacheException;
import com.cse309.cache.testutil.MutableClock;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatisticsTest {

    @Test
    void testFormulasAndZeroDivision() {
        CacheStatistics stats = new CacheStatistics();
        StatisticsSnapshot snapshot = stats.snapshot(0, 100);

        assertEquals(0.0, snapshot.getHitRatio());
        assertEquals(0.0, snapshot.getMissRatio());
        assertEquals(0.0, snapshot.getUtilization());
        assertEquals(0, snapshot.getTotalRequests());
    }

    @Test
    void testHitAndMissRatios() {
        CacheStatistics stats = new CacheStatistics();
        stats.recordGet(true, false, 500, true);
        stats.recordGet(true, false, 600, true);
        stats.recordGet(true, false, 400, true);
        stats.recordGet(false, false, 300, true); // 3 hits, 1 miss = 75% hit, 25% miss

        StatisticsSnapshot snapshot = stats.snapshot(10, 50);
        assertEquals(4, snapshot.getGetRequests());
        assertEquals(3, snapshot.getHits());
        assertEquals(1, snapshot.getMisses());
        assertEquals(75.0, snapshot.getHitRatio(), 0.001);
        assertEquals(25.0, snapshot.getMissRatio(), 0.001);
        assertEquals(20.0, snapshot.getUtilization(), 0.001); // 10 / 50 = 20%
    }

    @Test
    void testConservationInvariantUnderSequentialOperations() throws CacheException {
        MutableClock clock = new MutableClock();
        ThreadSafeCache<String, String> cache = new ThreadSafeCache<>(3, EvictionPolicyType.LRU, Duration.ofSeconds(10), clock);

        cache.put("k1", "v1");
        cache.put("k2", "v2");
        cache.put("k3", "v3");
        cache.put("k4", "v4"); // Evicts k1

        cache.remove("k2"); // Removes k2

        clock.advance(Duration.ofSeconds(15));
        cache.purgeExpired(); // k3, k4 expire

        cache.put("k5", "v5");

        StatisticsSnapshot s = cache.getStatistics().snapshot(cache.size(), cache.getCapacity());
        long calculated = s.getEntriesInserted() - s.getEvictions() - s.getExpirations() - s.getEntriesRemoved() - s.getEntriesCleared();

        assertEquals(cache.size(), calculated, "Conservation invariant failed: inserted - evicted - expired - removed - cleared == current size");
    }

    @Test
    void testReset() {
        CacheStatistics stats = new CacheStatistics();
        stats.recordGet(true, false, 1000, true);
        stats.recordPut(true, 0, 2000, true);

        stats.reset();
        StatisticsSnapshot snapshot = stats.snapshot(0, 100);

        assertEquals(0, snapshot.getTotalRequests());
        assertEquals(0, snapshot.getHits());
        assertEquals(0, snapshot.getEntriesInserted());
    }

    @Test
    void testCsvExport() throws Exception {
        CacheStatistics stats = new CacheStatistics();
        stats.recordGet(true, false, 500, true);
        StatisticsSnapshot snapshot = stats.snapshot(5, 100);

        StringWriter writer = new StringWriter();
        StatisticsExporter.exportStatisticsCsv(snapshot, writer);
        String csv = writer.toString();

        assertTrue(csv.contains("Metric,Value"));
        assertTrue(csv.contains("HitRatioPercent"));
        assertTrue(csv.contains("Histogram_< 1 µs"));
    }
}
