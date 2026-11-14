package com.cse309.cache.core;

import com.cse309.cache.event.CacheEvent;
import com.cse309.cache.event.CacheEventListener;
import com.cse309.cache.exception.CacheException;
import com.cse309.cache.stats.CacheStatistics;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Abstract base class for Cache implementations.
 * Demonstrates abstract class, inheritance, protected members, and final methods (Unit 1).
 *
 * @param <K> the type of key
 * @param <V> the type of value
 */
public abstract class AbstractCache<K, V> implements Cache<K, V> {

    protected final Clock clock;
    protected final CacheStatistics statistics;
    protected final List<CacheEventListener> listeners;

    protected AbstractCache(Clock clock, CacheStatistics statistics) {
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
        this.statistics = Objects.requireNonNull(statistics, "statistics cannot be null");
        this.listeners = new CopyOnWriteArrayList<>();
    }

    protected AbstractCache(Clock clock) {
        this(clock, new CacheStatistics());
    }

    protected AbstractCache() {
        this(Clock.systemUTC(), new CacheStatistics());
    }

    @Override
    public final Clock getClock() {
        return clock;
    }

    @Override
    public final CacheStatistics getStatistics() {
        return statistics;
    }

    @Override
    public final boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public void addListener(CacheEventListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(CacheEventListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Helper to dispatch events to listeners outside any lock.
     *
     * @param events list of events to dispatch
     */
    protected void dispatchEvents(List<CacheEvent> events) {
        if (events == null || events.isEmpty() || listeners.isEmpty()) {
            return;
        }
        for (CacheEvent event : events) {
            for (CacheEventListener listener : listeners) {
                try {
                    listener.onEvent(event);
                } catch (Exception ignored) {
                    // Prevent listener exceptions from breaking cache operations
                }
            }
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> entries) throws CacheException {
        if (entries == null) {
            return;
        }
        for (Map.Entry<? extends K, ? extends V> entry : entries.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }
}
