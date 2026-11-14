package com.cse309.cache.logging;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Thread-safe bounded in-memory log handler retaining the most recent log entries.
 * Feeds the JavaFX UI live log viewer.
 */
public final class InMemoryLogHandler extends Handler {

    private final int capacity;
    private final Deque<String> buffer;
    private final List<Consumer<String>> listeners;
    private final Object lock = new Object();

    public InMemoryLogHandler(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.buffer = new ArrayDeque<>(capacity);
        this.listeners = new ArrayList<>();
        setFormatter(new SingleLineFormatter());
    }

    public InMemoryLogHandler() {
        this(500);
    }

    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }

        String formatted = getFormatter().format(record);
        List<Consumer<String>> callbacks;

        synchronized (lock) {
            if (buffer.size() >= capacity) {
                buffer.pollFirst();
            }
            buffer.addLast(formatted);
            callbacks = new ArrayList<>(listeners);
        }

        for (Consumer<String> listener : callbacks) {
            try {
                listener.accept(formatted);
            } catch (Exception ignored) {
                // Ignore listener exceptions to avoid breaking logging
            }
        }
    }

    @Override
    public void flush() {
        // No-op for in-memory buffer
    }

    @Override
    public void close() throws SecurityException {
        synchronized (lock) {
            buffer.clear();
            listeners.clear();
        }
    }

    public List<String> getRecentLogs() {
        synchronized (lock) {
            return new ArrayList<>(buffer);
        }
    }

    public void addListener(Consumer<String> listener) {
        if (listener == null) {
            return;
        }
        synchronized (lock) {
            listeners.add(listener);
        }
    }

    public void removeListener(Consumer<String> listener) {
        if (listener == null) {
            return;
        }
        synchronized (lock) {
            listeners.remove(listener);
        }
    }

    public void clear() {
        synchronized (lock) {
            buffer.clear();
        }
    }
}
