package com.cse309.cache.eviction;

import com.cse309.cache.core.CacheEntrySnapshot;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * First In First Out (FIFO) eviction policy.
 * Backed by a LinkedHashSet maintaining strict insertion sequence.
 * Reading or overwriting an existing key preserves its original position in the queue (O(1)).
 *
 * @param <K> the key type
 */
public final class FirstInFirstOutPolicy<K> extends AbstractEvictionPolicy<K> {

    private final LinkedHashSet<K> insertionOrder;

    public FirstInFirstOutPolicy() {
        super(EvictionPolicyType.FIFO);
        this.insertionOrder = new LinkedHashSet<>();
    }

    @Override
    public void onInsert(K key, long sequence) {
        if (key != null) {
            // In FIFO, if key is already present (e.g. update/overwrite), it retains original position!
            insertionOrder.add(key);
        }
    }

    @Override
    public void onAccess(K key) {
        // In FIFO, read operations have no effect on queue position
    }

    @Override
    public void onRemove(K key) {
        if (key != null) {
            insertionOrder.remove(key);
        }
    }

    @Override
    public K selectVictim() {
        Iterator<K> iterator = insertionOrder.iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    @Override
    public void rebuild(List<CacheEntrySnapshot<K, ?>> entries) {
        insertionOrder.clear();
        if (entries == null || entries.isEmpty()) {
            return;
        }

        // Sort by insertionSequence ascending
        List<CacheEntrySnapshot<K, ?>> sorted = entries.stream()
                .sorted(Comparator.comparingLong(CacheEntrySnapshot<K, ?>::getInsertionSequence))
                .toList();

        for (CacheEntrySnapshot<K, ?> entry : sorted) {
            insertionOrder.add(entry.getKey());
        }
    }

    @Override
    public String describeVictim(K key) {
        return "earliest inserted";
    }

    @Override
    public void clear() {
        insertionOrder.clear();
    }

    @Override
    public int size() {
        return insertionOrder.size();
    }
}
