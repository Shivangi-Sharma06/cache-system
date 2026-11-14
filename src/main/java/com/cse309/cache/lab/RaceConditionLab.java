package com.cse309.cache.lab;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Educational laboratory demonstrating concurrency race conditions vs. synchronization.
 * Labelled: "educational, not part of the cache engine" (Unit 3).
 */
public final class RaceConditionLab {

    public static final class UnsafeCounterBox {
        private long counter = 0;

        public void increment() {
            counter++; // Non-atomic read-modify-write race condition
        }

        public long get() {
            return counter;
        }
    }

    public static final class SafeCounterBox {
        private long counter = 0;

        public synchronized void increment() {
            counter++; // Synchronized atomic update
        }

        public synchronized long get() {
            return counter;
        }
    }

    public record RaceLabResult(
            long expectedTotal,
            long unsafeActual,
            long safeActual,
            long unsafeElapsedNanos,
            long safeElapsedNanos
    ) {}

    public record MapRaceResult(
            int expectedEntries,
            int actualEntries,
            int nullOrCorruptCount,
            long elapsedNanos
    ) {}

    private RaceConditionLab() {
        // Utility class
    }

    public static RaceLabResult runCounterRace(int threads, int incrementsPerThread) throws InterruptedException {
        long expected = (long) threads * incrementsPerThread;

        // 1. Unsafe run
        UnsafeCounterBox unsafe = new UnsafeCounterBox();
        CountDownLatch startUnsafe = new CountDownLatch(1);
        CountDownLatch finishUnsafe = new CountDownLatch(threads);
        List<Thread> unsafeThreads = new ArrayList<>(threads);

        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(() -> {
                try {
                    startUnsafe.await();
                    for (int j = 0; j < incrementsPerThread; j++) {
                        unsafe.increment();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishUnsafe.countDown();
                }
            });
            unsafeThreads.add(t);
            t.start();
        }

        long startNanosUnsafe = System.nanoTime();
        startUnsafe.countDown();
        finishUnsafe.await();
        long unsafeElapsed = System.nanoTime() - startNanosUnsafe;

        // 2. Safe run
        SafeCounterBox safe = new SafeCounterBox();
        CountDownLatch startSafe = new CountDownLatch(1);
        CountDownLatch finishSafe = new CountDownLatch(threads);
        List<Thread> safeThreads = new ArrayList<>(threads);

        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(() -> {
                try {
                    startSafe.await();
                    for (int j = 0; j < incrementsPerThread; j++) {
                        safe.increment();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishSafe.countDown();
                }
            });
            safeThreads.add(t);
            t.start();
        }

        long startNanosSafe = System.nanoTime();
        startSafe.countDown();
        finishSafe.await();
        long safeElapsed = System.nanoTime() - startNanosSafe;

        return new RaceLabResult(expected, unsafe.get(), safe.get(), unsafeElapsed, safeElapsed);
    }

    public static MapRaceResult runUnsynchronizedMapRace(int threads, int itemsPerThread) throws InterruptedException {
        Map<Integer, String> plainMap = new HashMap<>();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threads);
        java.util.concurrent.atomic.AtomicInteger corruptCount = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            new Thread(() -> {
                try {
                    startLatch.await();
                    int base = threadId * itemsPerThread;
                    for (int i = 0; i < itemsPerThread; i++) {
                        try {
                            plainMap.put(base + i, "val-" + (base + i));
                        } catch (Exception ex) {
                            corruptCount.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            }).start();
        }

        long start = System.nanoTime();
        startLatch.countDown();
        finishLatch.await();
        long elapsed = System.nanoTime() - start;

        int expected = threads * itemsPerThread;
        int actual = plainMap.size();

        try {
            for (int i = 0; i < expected; i++) {
                if (plainMap.get(i) == null) {
                    corruptCount.incrementAndGet();
                }
            }
        } catch (Exception ex) {
            corruptCount.incrementAndGet();
        }

        return new MapRaceResult(expected, actual, corruptCount.get(), elapsed);
    }
}
