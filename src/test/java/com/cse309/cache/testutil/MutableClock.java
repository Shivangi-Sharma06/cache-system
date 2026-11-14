package com.cse309.cache.testutil;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Controllable, mutable clock for deterministic test execution.
 * Eliminates the need for Thread.sleep in expiration and timing tests.
 */
public final class MutableClock extends Clock {

    private Instant currentInstant;
    private final ZoneId zone;

    public MutableClock(Instant initialInstant, ZoneId zone) {
        this.currentInstant = initialInstant != null ? initialInstant : Instant.ofEpochMilli(1_700_000_000_000L);
        this.zone = zone != null ? zone : ZoneId.of("UTC");
    }

    public MutableClock(long initialEpochMillis) {
        this(Instant.ofEpochMilli(initialEpochMillis), ZoneId.of("UTC"));
    }

    public MutableClock() {
        this(Instant.ofEpochMilli(1_700_000_000_000L), ZoneId.of("UTC"));
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(currentInstant, zone);
    }

    @Override
    public Instant instant() {
        return currentInstant;
    }

    @Override
    public long millis() {
        return currentInstant.toEpochMilli();
    }

    public void advance(Duration duration) {
        if (duration != null) {
            this.currentInstant = this.currentInstant.plus(duration);
        }
    }

    public void advanceMillis(long millis) {
        this.currentInstant = this.currentInstant.plusMillis(millis);
    }

    public void set(Instant newInstant) {
        if (newInstant != null) {
            this.currentInstant = newInstant;
        }
    }

    public void setMillis(long epochMillis) {
        this.currentInstant = Instant.ofEpochMilli(epochMillis);
    }
}
