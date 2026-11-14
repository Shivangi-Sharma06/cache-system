package com.cse309.cache.eviction;

import com.cse309.cache.core.CacheEntrySnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Least Frequently Used (LFU) eviction policy.
 * Maintained via a TreeSet ordered by (frequency, insertion sequence) and a key index HashMap.
 * All operations run in O(log n) time.
 *
 * @param <K> the key type
 */
public final class LeastFrequentlyUsedPolicy<K> extends AbstractEvictionPolicy<K> {

    private static final class LfuNode<K> implements Comparable<LfuNode<K>> {
        private final K key;
        private long frequency;
        private final long insertionSequence;

        LfuNode(K key, long frequency, long insertionSequence) {
            this.key = key;
            this.frequency = frequency;
            this.insertionSequence = insertionSequence;
        }

        @Override
        public int compareTo(LfuNode<K> other) {
            if (this == other) return 0;
            int freqComp = Long.compare(this.frequency, other.frequency);
            if (freqComp != 0) return freqComp;
            int seqComp = Long.compare(this.insertionSequence, other.insertionSequence);
            if (seqComp != 0) return seqComp;
            if (Objects.equals(this.key, other.key)) return 0;
            return Integer.compare(System.identityHashCode(this.key), System.identityHashCode(other.key));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LfuNode<?> that)) return false;
            return frequency == that.frequency &&
                    insertionSequence == that.insertionSequence &&
                    Objects.equals(key, that.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, frequency, insertionSequence);
        }
    }

    private final Map<K, LfuNode<K>> nodeMap;
    private final TreeSet<LfuNode<K>> treeSet;

    public LeastFrequentlyUsedPolicy() {
        super(EvictionPolicyType.LFU);
        this.nodeMap = new HashMap<>();
        this.treeSet = new TreeSet<>();
    }

    @Override
    public void onInsert(K key, long sequence) {
        if (key == null) return;
        LfuNode<K> existing = nodeMap.get(key);
        if (existing != null) {
            // Overwriting existing key increments frequency
            treeSet.remove(existing);
            existing.frequency++;
            treeSet.add(existing);
        } else {
            LfuNode<K> newNode = new LfuNode<>(key, 1L, sequence);
            nodeMap.put(key, newNode);
            treeSet.add(newNode);
        }
    }

    @Override
    public void onAccess(K key) {
        if (key == null) return;
        LfuNode<K> node = nodeMap.get(key);
        if (node != null) {
            treeSet.remove(node);
            node.frequency++;
            treeSet.add(node);
        }
    }

    @Override
    public void onRemove(K key) {
        if (key == null) return;
        LfuNode<K> node = nodeMap.remove(key);
        if (node != null) {
            treeSet.remove(node);
        }
    }

    @Override
    public K selectVictim() {
        return treeSet.isEmpty() ? null : treeSet.first().key;
    }

    @Override
    public void rebuild(List<CacheEntrySnapshot<K, ?>> entries) {
        clear();
        if (entries == null || entries.isEmpty()) return;

        for (CacheEntrySnapshot<K, ?> entry : entries) {
            LfuNode<K> node = new LfuNode<>(
                    entry.getKey(),
                    Math.max(1L, entry.getAccessCount()),
                    entry.getInsertionSequence()
            );
            nodeMap.put(entry.getKey(), node);
            treeSet.add(node);
        }
    }

    @Override
    public String describeVictim(K key) {
        LfuNode<K> node = nodeMap.get(key);
        if (node != null) {
            return String.format("lowest frequency (%d), oldest among ties (seq=%d)",
                    node.frequency, node.insertionSequence);
        }
        return "lowest frequency";
    }

    @Override
    public void clear() {
        nodeMap.clear();
        treeSet.clear();
    }

    @Override
    public int size() {
        return nodeMap.size();
    }
}
