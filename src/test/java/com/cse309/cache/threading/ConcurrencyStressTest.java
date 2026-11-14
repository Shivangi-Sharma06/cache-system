package com.cse309.cache.threading;

import com.cse309.cache.core.CacheEntrySnapshot;
import com.cse309.cache.core.ThreadSafeCache;
import com.cse309.cache.eviction.EvictionPolicyType;
import com.cse309.cache.stats.StatisticsSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConcurrencyStressTest {

    @Test
    void testConcurrentMixedStressLoad() throws InterruptedException {
        final int capacity = 100;
        final int numThreads = 16;
        final int opsPerThread = 20_000;
        final int keySpace = 250;

        ThreadSafeCache<String, String> cache = new ThreadSafeCache<>(capacity, EvictionPolicyType.LRU);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numThreads);
        AtomicBoolean capacityExceeded = new AtomicBoolean(false);
        AtomicInteger unhandledExceptions = new AtomicInteger(0);

        // Observer thread continuously checking size <= capacity
        AtomicBoolean running = new AtomicBoolean(true);
        Thread observer = new Thread(() -> {
            while (running.get()) {
                if (cache.size() > capacity) {
                    capacityExceeded.set(true);
                }
                Thread.yield();
            }
        }, "stress-observer");
        observer.start();

        List<Thread> workers = new ArrayList<>(numThreads);
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            Thread worker = new Thread(() -> {
                try {
                    startLatch.await();
                    Random rnd = new Random(threadId * 31L + 17);

                    for (int i = 0; i < opsPerThread; i++) {
                        int keyNum = rnd.nextInt(keySpace);
                        String key = "key:" + keyNum;
                        int action = rnd.nextInt(100);

                        try {
                            if (action < 50) { // 50% GET
                                cache.get(key);
                            } else if (action < 80) { // 30% PUT
                                cache.put(key, "v-" + keyNum + "-" + i);
                            } else if (action < 90) { // 10% UPDATE
                                try {
                                    cache.update(key, "upd-" + i);
                                } catch (Exception ignored) {
                                    // Update on missing key throws expected CacheKeyNotFoundException
                                }
                            } else if (action < 95) { // 5% REMOVE
                                cache.remove(key);
                            } else { // 5% SEARCH
                                cache.search(k -> k.endsWith(String.valueOf(keyNum % 10)));
                            }
                        } catch (Exception e) {
                            unhandledExceptions.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            }, "stress-worker-" + t);
            worker.start();
            workers.add(worker);
        }

        // Fire all workers simultaneously
        startLatch.countDown();
        finishLatch.await();

        running.set(false);
        observer.join(1000);

        for (Thread w : workers) {
            w.join(1000);
        }

        // Assertions
        assertFalse(capacityExceeded.get(), "Cache size exceeded capacity during stress test!");
        assertEquals(0, unhandledExceptions.get(), "Unexpected exceptions escaped during execution!");

        StatisticsSnapshot stats = cache.getStatistics().snapshot(cache.size(), cache.getCapacity());

        // 1. Invariant: hits + misses == GET requests
        assertEquals(stats.getGetRequests(), stats.getHits() + stats.getMisses(),
                "hits + misses must equal total GET requests");

        // 2. Conservation invariant: inserted - evicted - expired - removed - cleared == current size
        long expectedSize = stats.getEntriesInserted() - stats.getEvictions() - stats.getExpirations()
                - stats.getEntriesRemoved() - stats.getEntriesCleared();
        assertEquals(cache.size(), expectedSize,
                "Conservation invariant violated: inserted - evicted - expired - removed - cleared == size");

        // 3. Invariant: no duplicate keys in final entry snapshot
        List<CacheEntrySnapshot<String, String>> finalEntries = cache.getAllEntries();
        Set<String> uniqueKeys = new HashSet<>();
        for (CacheEntrySnapshot<String, String> entry : finalEntries) {
            assertTrue(uniqueKeys.add(entry.getKey()), "Duplicate key found in final entries: " + entry.getKey());
        }

        assertTrue(cache.size() <= capacity, "Final cache size exceeds capacity");
    }

    @Test
    void testFiveHundredThousandOperationsSimulation() throws Exception {
        com.cse309.cache.core.StringCache stringCache = new com.cse309.cache.core.StringCache(200, EvictionPolicyType.LRU);
        com.cse309.cache.simulation.SimulationController controller =
                new com.cse309.cache.simulation.SimulationController(stringCache, null);

        com.cse309.cache.simulation.SimulationConfig config = new com.cse309.cache.simulation.SimulationConfig(
                8,
                500_000,
                com.cse309.cache.simulation.OperationMix.READ_HEAVY,
                com.cse309.cache.simulation.KeyDistribution.HOTSPOT_80_20,
                500,
                512,
                999L,
                false,
                false
        );

        controller.start(config);

        long start = System.currentTimeMillis();
        while (controller.isActive() && (System.currentTimeMillis() - start) < 30_000) {
            com.cse309.cache.simulation.SimulationProgress progress = controller.getProgress();
            if (progress.getState() == com.cse309.cache.simulation.SimulationProgress.State.COMPLETED) {
                break;
            }
            Thread.sleep(50);
        }

        controller.stop();

        com.cse309.cache.simulation.SimulationProgress finalProgress = controller.getProgress();
        assertTrue(finalProgress.getCompleted() > 0, "Simulation must have processed operations");
        assertTrue(stringCache.size() <= 200, "Cache size must respect capacity");
        assertFalse(finalProgress.getFailed() > finalProgress.getCompleted() / 2, "Failures should not dominate run");
    }
}
