package com.cse309.cache.eviction;

import com.cse309.cache.core.CacheEntrySnapshot;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Least Recently Used (LRU) eviction policy.
 * Backed by a LinkedHashSet where accessing an element moves it to the tail.
 * The head element is the least recently accessed victim (O(1)).
 *
 * @param <K> the key type
 */
public final class LeastRecentlyUsedPolicy<K> extends AbstractEvictionPolicy<K> {

    private final LinkedHashSet<K> accessOrder;

    public LeastRecentlyUsedPolicy() {
        super(EvictionPolicyType.LRU);
        this.accessOrder = new LinkedHashSet<>();
    }

    @Override
    public void onInsert(K key, long sequence) {
        if (key != null) {
            accessOrder.remove(key);
            accessOrder.add(key);
        }
    }

    @Override
    public void onAccess(K key) {
        if (key != null && accessOrder.remove(key)) {
            accessOrder.add(key);
        }
    }

    @Override
    public void onRemove(K key) {
        if (key != null) {
            accessOrder.remove(key);
        }
    }

    @Override
    public K selectVictim() {
        Iterator<K> iterator = accessOrder.iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    @Override
    public void rebuild(List<CacheEntrySnapshot<K, ?>> entries) {
        accessOrder.clear();
        if (entries == null || entries.isEmpty()) {
            return;
        }

        // Sort by lastAccessAt ascending, then insertionSequence ascending
        List<CacheEntrySnapshot<K, ?>> sorted = entries.stream()
                .sorted(Comparator.comparingLong(CacheEntrySnapshot<K, ?>::getLastAccessAt)
                        .thenComparingLong(CacheEntrySnapshot<K, ?>::getInsertionSequence))
                .toList();

        for (CacheEntrySnapshot<K, ?> entry : sorted) {
            accessOrder.add(entry.getKey());
        }
    }

    @Override
    public String describeVictim(K key) {
        return "least recently used";
    }

    @Override
    public void clear() {
        accessOrder.clear();
    }

    @Override
    public int size() {
        return accessOrder.size();
    }
}
