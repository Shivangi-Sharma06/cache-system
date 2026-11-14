package com.cse309.cache.event;

import java.util.EventListener;

/**
 * Listener interface for cache lifecycle and mutation events.
 * Part of the Delegation Event Model implementation.
 */
public interface CacheEventListener extends EventListener {

    void onEvent(CacheEvent event);

    default void onPut(CacheEvent event) {}

    default void onUpdate(CacheEvent event) {}

    default void onRemove(CacheEvent event) {}

    default void onEvict(CacheEvent event) {}

    default void onExpire(CacheEvent event) {}

    default void onClear(CacheEvent event) {}
}
