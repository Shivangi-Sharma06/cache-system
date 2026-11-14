package com.cse309.cache.threading;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Generic thread-safe bounded buffer implementing producer-consumer synchronization.
 * Uses core Java concurrency primitives: synchronized, wait(), and notifyAll() inside while loops (Unit 3).
 *
 * @param <T> element type
 */
public final class BoundedBuffer<T> {

    private final Queue<T> queue;
    private final int capacity;
    private boolean closed = false;

    public BoundedBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.queue = new ArrayDeque<>(capacity);
    }

    public synchronized void put(T item) throws InterruptedException {
        while (queue.size() >= capacity && !closed) {
            wait();
        }
        if (closed) {
            throw new IllegalStateException("Cannot put into a closed BoundedBuffer");
        }
        queue.add(item);
        notifyAll();
    }

    public synchronized T take() throws InterruptedException {
        while (queue.isEmpty() && !closed) {
            wait();
        }
        if (queue.isEmpty() && closed) {
            return null; // End of stream indicator
        }
        T item = queue.poll();
        notifyAll();
        return item;
    }

    public synchronized void close() {
        if (!closed) {
            closed = true;
            notifyAll();
        }
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    public synchronized int size() {
        return queue.size();
    }

    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }

    public int getCapacity() {
        return capacity;
    }

    public synchronized void clear() {
        queue.clear();
        notifyAll();
    }
}
