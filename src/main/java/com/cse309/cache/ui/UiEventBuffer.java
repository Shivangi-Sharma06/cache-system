package com.cse309.cache.ui;

import com.cse309.cache.event.CacheEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Thread-safe bounded event buffer decoupling high-throughput engine threads
 * from JavaFX Application Thread updates.
 */
public final class UiEventBuffer {

    private final Queue<CacheEvent> queue;
    private final int capacity;
    private final Object lock = new Object();

    public UiEventBuffer(int capacity) {
        this.capacity = capacity > 0 ? capacity : 1000;
        this.queue = new ArrayDeque<>(this.capacity);
    }

    public UiEventBuffer() {
        this(1000);
    }

    public void add(CacheEvent event) {
        if (event == null) return;
        synchronized (lock) {
            if (queue.size() >= capacity) {
                queue.poll(); // Drop oldest event if buffer is full
            }
            queue.add(event);
        }
    }

    public List<CacheEvent> drain() {
        synchronized (lock) {
            if (queue.isEmpty()) return List.of();
            List<CacheEvent> events = new ArrayList<>(queue);
            queue.clear();
            return events;
        }
    }

    public int size() {
        synchronized (lock) {
            return queue.size();
        }
    }
}
