package com.cse309.cache.expiry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Ordered index of expiring entries ordered by (expiresAt, insertionSequence).
 * Enables efficient O(k log n) active purge sweeps proportional to the number of expired entries (k),
 * rather than scanning the entire cache (Unit 4 TreeSet and Iterator usage).
 *
 * @param <K> the type of key
 */
public final class ExpiryIndex<K> {

    public static final class ExpiryEntry<K> implements Comparable<ExpiryEntry<K>> {
        private final K key;
        private final long expiresAt;
        private final long insertionSequence;

        public ExpiryEntry(K key, long expiresAt, long insertionSequence) {
            this.key = key;
            this.expiresAt = expiresAt;
            this.insertionSequence = insertionSequence;
        }

        public K getKey() {
            return key;
        }

        public long getExpiresAt() {
            return expiresAt;
        }

        public long getInsertionSequence() {
            return insertionSequence;
        }

        @Override
        public int compareTo(ExpiryEntry<K> other) {
            if (this == other) return 0;
            int timeComp = Long.compare(this.expiresAt, other.expiresAt);
            if (timeComp != 0) return timeComp;
            int seqComp = Long.compare(this.insertionSequence, other.insertionSequence);
            if (seqComp != 0) return seqComp;
            if (Objects.equals(this.key, other.key)) return 0;
            return Integer.compare(System.identityHashCode(this.key), System.identityHashCode(other.key));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ExpiryEntry<?> that)) return false;
            return expiresAt == that.expiresAt &&
                    insertionSequence == that.insertionSequence &&
                    Objects.equals(key, that.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, expiresAt, insertionSequence);
        }
    }

    private final TreeSet<ExpiryEntry<K>> treeSet = new TreeSet<>();
    private final Map<K, ExpiryEntry<K>> indexMap = new HashMap<>();

    public void track(K key, long expiresAt, long insertionSequence) {
        if (key == null) return;
        untrack(key);
        ExpiryEntry<K> entry = new ExpiryEntry<>(key, expiresAt, insertionSequence);
        indexMap.put(key, entry);
        treeSet.add(entry);
    }

    public void untrack(K key) {
        if (key == null) return;
        ExpiryEntry<K> existing = indexMap.remove(key);
        if (existing != null) {
            treeSet.remove(existing);
        }
    }

    /**
     * Purges and returns all expired keys up to the given wall-clock epoch timestamp.
     * Uses explicit Iterator with Iterator.remove() as required by Syllabus U4.
     *
     * @param nowCurrentMillis current epoch milliseconds
     * @return list of expired keys that were removed
     */
    public List<K> purgeExpired(long nowCurrentMillis) {
        List<K> expiredKeys = new ArrayList<>();
        Iterator<ExpiryEntry<K>> iterator = treeSet.iterator();

        while (iterator.hasNext()) {
            ExpiryEntry<K> entry = iterator.next();
            if (nowCurrentMillis >= entry.getExpiresAt()) {
                expiredKeys.add(entry.getKey());
                iterator.remove();
                indexMap.remove(entry.getKey());
            } else {
                // Entries are ordered by expiresAt ascending, so subsequent entries are not expired yet
                break;
            }
        }

        return expiredKeys;
    }

    public void clear() {
        treeSet.clear();
        indexMap.clear();
    }

    public int size() {
        return treeSet.size();
    }

    public boolean isTracked(K key) {
        return indexMap.containsKey(key);
    }
}
