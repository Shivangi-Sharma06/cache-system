package com.cse309.cache.core;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheEntrySnapshotTest {

    @Test
    void testSnapshotMetadataAndTtl() {
        long now = 1_000_000L;
        long expires = now + 5000L;
        Clock clock = Clock.fixed(Instant.ofEpochMilli(now), ZoneId.of("UTC"));

        CacheEntrySnapshot<String, String> snapshot = new CacheEntrySnapshot<>(
                "keyA", "valA", now, now, expires, 3L, 42L
        );

        assertEquals("keyA", snapshot.getKey());
        assertEquals("valA", snapshot.getValue());
        assertEquals(3L, snapshot.getAccessCount());
        assertEquals(42L, snapshot.getInsertionSequence());
        assertTrue(snapshot.hasExpiry());
        assertFalse(snapshot.isExpired(clock));
        assertEquals(Duration.ofMillis(5000), snapshot.remainingTtl(clock).orElseThrow());

        // Advance clock past expiration
        Clock expiredClock = Clock.fixed(Instant.ofEpochMilli(expires + 1), ZoneId.of("UTC"));
        assertTrue(snapshot.isExpired(expiredClock));
        assertEquals(Duration.ZERO, snapshot.remainingTtl(expiredClock).orElseThrow());
    }

    @Test
    void testNoExpirySnapshot() {
        Clock clock = Clock.systemUTC();
        CacheEntrySnapshot<String, String> snapshot = new CacheEntrySnapshot<>(
                "keyB", "valB", 1000L, 1000L, CacheDefaults.NO_EXPIRY_SENTINEL, 1L, 1L
        );

        assertFalse(snapshot.hasExpiry());
        assertFalse(snapshot.isExpired(clock));
        assertTrue(snapshot.remainingTtl(clock).isEmpty());
    }

    @Test
    void testEqualityAndComparison() {
        CacheEntrySnapshot<String, String> s1 = new CacheEntrySnapshot<>(
                "A", "1", 100L, 100L, -1L, 1L, 1L
        );
        CacheEntrySnapshot<String, String> s2 = new CacheEntrySnapshot<>(
                "A", "1", 100L, 100L, -1L, 1L, 1L
        );
        CacheEntrySnapshot<String, String> s3 = new CacheEntrySnapshot<>(
                "B", "2", 200L, 200L, -1L, 1L, 2L
        );

        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
        assertTrue(s1.compareTo(s3) < 0);
    }
}
