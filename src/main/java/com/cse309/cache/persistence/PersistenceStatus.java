package com.cse309.cache.persistence;

import java.time.Instant;
import java.util.Optional;

/**
 * Status representation of persistence health and database connectivity.
 */
public final class PersistenceStatus {

    public enum State {
        AVAILABLE,
        UNAVAILABLE
    }

    private final State state;
    private final String lastError;
    private final long lastSuccessTimestamp;

    public PersistenceStatus(State state, String lastError, long lastSuccessTimestamp) {
        this.state = state;
        this.lastError = lastError;
        this.lastSuccessTimestamp = lastSuccessTimestamp;
    }

    public static PersistenceStatus available(long lastSuccessTimestamp) {
        return new PersistenceStatus(State.AVAILABLE, null, lastSuccessTimestamp);
    }

    public static PersistenceStatus unavailable(String errorMessage) {
        return new PersistenceStatus(State.UNAVAILABLE, errorMessage, 0L);
    }

    public boolean isAvailable() {
        return state == State.AVAILABLE;
    }

    public State getState() {
        return state;
    }

    public Optional<String> getLastError() {
        return Optional.ofNullable(lastError);
    }

    public long getLastSuccessTimestamp() {
        return lastSuccessTimestamp;
    }

    public Optional<Instant> getLastSuccessInstant() {
        return lastSuccessTimestamp > 0 ? Optional.of(Instant.ofEpochMilli(lastSuccessTimestamp)) : Optional.empty();
    }

    @Override
    public String toString() {
        return String.format("[%s]%s", state, lastError != null ? " Error: " + lastError : "");
    }
}
