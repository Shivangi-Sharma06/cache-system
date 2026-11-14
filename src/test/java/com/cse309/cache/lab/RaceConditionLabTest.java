package com.cse309.cache.lab;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaceConditionLabTest {

    @Test
    void testSafeCounterIsExact() throws InterruptedException {
        int threads = 8;
        int increments = 10_000;
        long expected = (long) threads * increments;

        RaceConditionLab.RaceLabResult result = RaceConditionLab.runCounterRace(threads, increments);

        assertEquals(expected, result.expectedTotal());
        assertEquals(expected, result.safeActual(), "Synchronized safe counter must be exact!");
        assertTrue(result.safeElapsedNanos() > 0);
        assertTrue(result.unsafeElapsedNanos() > 0);
        // Note: As specified in Section 14, unsafe counter is NOT asserted to fail because race conditions are non-deterministic.
    }

    @Test
    void testMapRaceRunsSafely() throws InterruptedException {
        RaceConditionLab.MapRaceResult result = RaceConditionLab.runUnsynchronizedMapRace(4, 500);
        assertEquals(2000, result.expectedEntries());
        assertTrue(result.actualEntries() > 0);
    }
}
