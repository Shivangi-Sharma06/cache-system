package com.cse309.cache.event;

/**
 * Adapter class providing default no-op implementations of CacheEventListener methods.
 * Demonstrates the Adapter Class pattern from Unit 3.
 */
public class CacheEventAdapter implements CacheEventListener {

    @Override
    public void onEvent(CacheEvent event) {
        if (event == null) return;
        switch (event.getType()) {
            case PUT -> onPut(event);
            case UPDATE -> onUpdate(event);
            case REMOVE -> onRemove(event);
            case EVICT -> onEvict(event);
            case EXPIRE -> onExpire(event);
            case CLEAR -> onClear(event);
            default -> { /* No-op for other event types */ }
        }
    }

    @Override
    public void onPut(CacheEvent event) {
        // Subclasses may override
    }

    @Override
    public void onUpdate(CacheEvent event) {
        // Subclasses may override
    }

    @Override
    public void onRemove(CacheEvent event) {
        // Subclasses may override
    }

    @Override
    public void onEvict(CacheEvent event) {
        // Subclasses may override
    }

    @Override
    public void onExpire(CacheEvent event) {
        // Subclasses may override
    }

    @Override
    public void onClear(CacheEvent event) {
        // Subclasses may override
    }
}
