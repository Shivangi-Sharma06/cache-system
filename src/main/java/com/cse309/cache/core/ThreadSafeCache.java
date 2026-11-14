package com.cse309.cache.core;

import com.cse309.cache.config.CacheConfiguration;
import com.cse309.cache.event.CacheEvent;
import com.cse309.cache.eviction.EvictionPolicy;
import com.cse309.cache.eviction.EvictionPolicyType;
import com.cse309.cache.eviction.EvictionRecord;
import com.cse309.cache.eviction.FirstInFirstOutPolicy;
import com.cse309.cache.eviction.LeastFrequentlyUsedPolicy;
import com.cse309.cache.eviction.LeastRecentlyUsedPolicy;
import com.cse309.cache.exception.CacheException;
import com.cse309.cache.exception.CacheKeyNotFoundException;
import com.cse309.cache.exception.InvalidCacheCapacityException;
import com.cse309.cache.exception.InvalidCacheOperationException;
import com.cse309.cache.expiry.ExpiryIndex;
import com.cse309.cache.stats.CacheStatistics;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Thread-safe core cache engine implementing all operation semantics.
 * Governed by a single private lock and leaf-lock discipline (no callbacks under lock).
 *
 * @param <K> key type
 * @param <V> value type
 */
public class ThreadSafeCache<K, V> extends AbstractCache<K, V> {

    private final Object lock = new Object();

    private final Map<K, CacheEntry<V>> map;
    private final ExpiryIndex<K> expiryIndex;
    private EvictionPolicy<K> evictionPolicy;

    private int capacity;
    private Duration defaultTtl;

    private long accessTickCounter;
    private long sequenceCounter;
    private volatile long modificationCount;

    public ThreadSafeCache(CacheConfiguration config, Clock clock, CacheStatistics statistics) {
        super(clock, statistics);
        Objects.requireNonNull(config, "config cannot be null");
        this.capacity = config.getCapacity();
        this.defaultTtl = config.getDefaultTtlOrNull();
        this.map = new HashMap<>();
        this.expiryIndex = new ExpiryIndex<>();
        this.evictionPolicy = createPolicyInstance(config.getEvictionPolicy());
        this.accessTickCounter = 0L;
        this.sequenceCounter = 0L;
        this.modificationCount = 0L;
    }

    public ThreadSafeCache(int capacity, EvictionPolicyType policyType, Duration defaultTtl, Clock clock) {
        this(CacheConfiguration.builder()
                .capacity(capacity)
                .evictionPolicy(policyType)
                .defaultTtl(defaultTtl)
                .build(), clock, new CacheStatistics());
    }

    public ThreadSafeCache(int capacity, EvictionPolicyType policyType, Clock clock) {
        this(capacity, policyType, CacheDefaults.DEFAULT_TTL, clock);
    }

    public ThreadSafeCache(int capacity, EvictionPolicyType policyType) {
        this(capacity, policyType, Clock.systemUTC());
    }

    public ThreadSafeCache() {
        this(CacheDefaults.DEFAULT_CAPACITY, EvictionPolicyType.LRU, Clock.systemUTC());
    }

    private static <K> EvictionPolicy<K> createPolicyInstance(EvictionPolicyType type) {
        return switch (type != null ? type : EvictionPolicyType.LRU) {
            case LRU -> new LeastRecentlyUsedPolicy<>();
            case FIFO -> new FirstInFirstOutPolicy<>();
            case LFU -> new LeastFrequentlyUsedPolicy<>();
        };
    }

    protected void validateKeyAndValue(K key, V value) throws CacheException {
        if (key == null) {
            throw new InvalidCacheOperationException("Cache key cannot be null", "Key cannot be null.");
        }
        if (value == null) {
            throw new InvalidCacheOperationException("Cache value cannot be null", "Value cannot be null.");
        }
    }

    @Override
    public CacheWriteResult<K> put(K key, V value) throws CacheException {
        return put(key, value, defaultTtl);
    }

    @Override
    public CacheWriteResult<K> putWithoutExpiry(K key, V value) throws CacheException {
        return putInternal(key, value, CacheDefaults.NO_EXPIRY_SENTINEL);
    }

    @Override
    public CacheWriteResult<K> put(K key, V value, Duration ttl) throws CacheException {
        validateTtl(ttl);
        long now = clock.millis();
        long expiresAt = ttl != null ? now + ttl.toMillis() : CacheDefaults.NO_EXPIRY_SENTINEL;
        return putInternal(key, value, expiresAt);
    }

    private void validateTtl(Duration ttl) throws InvalidCacheOperationException {
        if (ttl != null && (ttl.isNegative() || ttl.isZero())) {
            throw new InvalidCacheOperationException("TTL duration must be positive", "TTL must be positive.");
        }
    }

    private CacheWriteResult<K> putInternal(K key, V value, long expiresAt) throws CacheException {
        validateKeyAndValue(key, value);
        long startNanos = System.nanoTime();
        List<EvictionRecord> evictions = new ArrayList<>();
        List<CacheEvent> events = new ArrayList<>();
        CacheWriteResult.Status status;

        synchronized (lock) {
            long now = clock.millis();
            purgeExpiredLocked(now, events);

            CacheEntry<V> existing = map.get(key);
            if (existing != null) {
                existing.update(value, now, ++accessTickCounter, expiresAt);
                evictionPolicy.onInsert(key, existing.getInsertionSequence());
                updateExpiryIndexLocked(key, expiresAt, existing.getInsertionSequence());
                status = CacheWriteResult.Status.REPLACED;
                events.add(new CacheEvent(CacheEvent.EventType.UPDATE, key, "Replaced value"));
            } else {
                if (map.size() >= capacity) {
                    EvictionRecord eviction = evictVictimLocked(now, events, EvictionRecord.EvictionReason.CAPACITY);
                    if (eviction != null) {
                        evictions.add(eviction);
                    }
                }

                long sequence = ++sequenceCounter;
                CacheEntry<V> newEntry = new CacheEntry<>(value, now, expiresAt, sequence, ++accessTickCounter);
                map.put(key, newEntry);
                evictionPolicy.onInsert(key, sequence);
                updateExpiryIndexLocked(key, expiresAt, sequence);
                status = CacheWriteResult.Status.INSERTED;
                events.add(new CacheEvent(CacheEvent.EventType.PUT, key, "Inserted new entry"));
            }
            modificationCount++;
        }

        long latencyNanos = System.nanoTime() - startNanos;
        statistics.recordPut(status == CacheWriteResult.Status.INSERTED, evictions.size(), latencyNanos, true);
        dispatchEvents(events);

        return new CacheWriteResult<>(status, key, evictions);
    }

    private void updateExpiryIndexLocked(K key, long expiresAt, long sequence) {
        if (expiresAt != CacheDefaults.NO_EXPIRY_SENTINEL) {
            expiryIndex.track(key, expiresAt, sequence);
        } else {
            expiryIndex.untrack(key);
        }
    }

    private EvictionRecord evictVictimLocked(long now, List<CacheEvent> events, EvictionRecord.EvictionReason reason) {
        K victimKey = evictionPolicy.selectVictim();
        if (victimKey == null) {
            return null;
        }

        CacheEntry<V> victim = map.remove(victimKey);
        if (victim == null) {
            return null;
        }

        expiryIndex.untrack(victimKey);
        evictionPolicy.onRemove(victimKey);

        String explanation = evictionPolicy.describeVictim(victimKey);
        EvictionRecord record = new EvictionRecord(
                victimKey, evictionPolicy.getType(), reason, now,
                victim.getAccessCount(), victim.getLastAccessTick(), explanation);

        events.add(new CacheEvent(CacheEvent.EventType.EVICT, victimKey, record));
        return record;
    }

    @Override
    public CacheLookupResult<V> get(K key) {
        if (key == null) {
            statistics.recordGet(false, false, 0L, true);
            return CacheLookupResult.miss();
        }

        long startNanos = System.nanoTime();
        List<CacheEvent> events = new ArrayList<>();
        CacheLookupResult<V> result;

        synchronized (lock) {
            long now = clock.millis();
            CacheEntry<V> entry = map.get(key);

            if (entry == null) {
                result = CacheLookupResult.miss();
            } else if (entry.isExpired(now)) {
                map.remove(key);
                expiryIndex.untrack(key);
                evictionPolicy.onRemove(key);
                modificationCount++;
                events.add(new CacheEvent(CacheEvent.EventType.EXPIRE, key, "Lazy expiration on get"));
                result = CacheLookupResult.expired();
            } else {
                entry.recordAccess(now, ++accessTickCounter);
                evictionPolicy.onAccess(key);
                CacheEntrySnapshot<K, V> snapshot = createSnapshot(key, entry);
                result = CacheLookupResult.hit(entry.getValue(), snapshot);
            }
        }

        long latency = System.nanoTime() - startNanos;
        statistics.recordGet(result.isHit(), result.isExpired(), latency, true);
        dispatchEvents(events);

        return result;
    }

    @Override
    public CacheWriteResult<K> update(K key, V value) throws CacheException {
        return update(key, value, null);
    }

    @Override
    public CacheWriteResult<K> update(K key, V value, Duration ttl) throws CacheException {
        validateKeyAndValue(key, value);
        validateTtl(ttl);
        long startNanos = System.nanoTime();
        List<CacheEvent> events = new ArrayList<>();

        synchronized (lock) {
            long now = clock.millis();
            CacheEntry<V> entry = map.get(key);

            if (entry == null) {
                long latency = System.nanoTime() - startNanos;
                statistics.recordUpdate(latency, false);
                throw new CacheKeyNotFoundException(key);
            }

            if (entry.isExpired(now)) {
                map.remove(key);
                expiryIndex.untrack(key);
                evictionPolicy.onRemove(key);
                modificationCount++;
                events.add(new CacheEvent(CacheEvent.EventType.EXPIRE, key, "Expired on update attempt"));
                long latency = System.nanoTime() - startNanos;
                statistics.recordUpdate(latency, false);
                dispatchEvents(events);
                throw new CacheKeyNotFoundException(key);
            }

            Long newExpiresAt = ttl != null ? now + ttl.toMillis() : null;
            entry.update(value, now, ++accessTickCounter, newExpiresAt);
            evictionPolicy.onAccess(key);
            if (newExpiresAt != null) {
                updateExpiryIndexLocked(key, newExpiresAt, entry.getInsertionSequence());
            }
            modificationCount++;
            events.add(new CacheEvent(CacheEvent.EventType.UPDATE, key, "Updated value"));
        }

        long latency = System.nanoTime() - startNanos;
        statistics.recordUpdate(latency, true);
        dispatchEvents(events);

        return CacheWriteResult.replaced(key);
    }

    @Override
    public CacheLookupResult<V> remove(K key) {
        if (key == null) {
            statistics.recordRemove(false, 0L, true);
            return CacheLookupResult.miss();
        }

        long startNanos = System.nanoTime();
        List<CacheEvent> events = new ArrayList<>();
        CacheLookupResult<V> result;

        synchronized (lock) {
            long now = clock.millis();
            CacheEntry<V> entry = map.remove(key);

            if (entry == null) {
                result = CacheLookupResult.miss();
            } else {
                expiryIndex.untrack(key);
                evictionPolicy.onRemove(key);
                modificationCount++;

                if (entry.isExpired(now)) {
                    events.add(new CacheEvent(CacheEvent.EventType.EXPIRE, key, "Expired entry removed"));
                    result = CacheLookupResult.expired();
                } else {
                    events.add(new CacheEvent(CacheEvent.EventType.REMOVE, key, "Entry removed"));
                    CacheEntrySnapshot<K, V> snapshot = createSnapshot(key, entry);
                    result = CacheLookupResult.hit(entry.getValue(), snapshot);
                }
            }
        }

        long latency = System.nanoTime() - startNanos;
        statistics.recordRemove(result.isHit(), latency, true);
        dispatchEvents(events);

        return result;
    }

    @Override
    public boolean containsKey(K key) {
        if (key == null) return false;
        synchronized (lock) {
            long now = clock.millis();
            CacheEntry<V> entry = map.get(key);
            if (entry == null) return false;
            if (entry.isExpired(now)) {
                map.remove(key);
                expiryIndex.untrack(key);
                evictionPolicy.onRemove(key);
                modificationCount++;
                return false;
            }
            return true;
        }
    }

    @Override
    public CacheLookupResult<V> peek(K key) {
        if (key == null) return CacheLookupResult.miss();
        synchronized (lock) {
            long now = clock.millis();
            CacheEntry<V> entry = map.get(key);
            if (entry == null) return CacheLookupResult.miss();
            if (entry.isExpired(now)) return CacheLookupResult.expired();
            return CacheLookupResult.hit(entry.getValue(), createSnapshot(key, entry));
        }
    }

    @Override
    public List<CacheEntrySnapshot<K, V>> search(Predicate<? super K> keyFilter) {
        long startNanos = System.nanoTime();
        List<CacheEntrySnapshot<K, V>> results = new ArrayList<>();
        synchronized (lock) {
            long now = clock.millis();
            for (Map.Entry<K, CacheEntry<V>> entry : map.entrySet()) {
                if (!entry.getValue().isExpired(now)) {
                    if (keyFilter == null || keyFilter.test(entry.getKey())) {
                        results.add(createSnapshot(entry.getKey(), entry.getValue()));
                    }
                }
            }
        }
        statistics.recordSearch(System.nanoTime() - startNanos, true);
        return Collections.unmodifiableList(results);
    }

    @Override
    public List<CacheEntrySnapshot<K, V>> getAllEntries() {
        return search(null);
    }

    @Override
    public void clear() {
        List<CacheEvent> events = new ArrayList<>();
        int count;
        synchronized (lock) {
            count = map.size();
            map.clear();
            expiryIndex.clear();
            evictionPolicy.clear();
            modificationCount++;
            events.add(new CacheEvent(CacheEvent.EventType.CLEAR, null, "Cleared " + count + " entries"));
        }
        statistics.recordClear(count);
        dispatchEvents(events);
    }

    @Override
    public int size() {
        synchronized (lock) {
            long now = clock.millis();
            purgeExpiredLocked(now, null);
            return map.size();
        }
    }

    @Override
    public int getCapacity() {
        synchronized (lock) {
            return capacity;
        }
    }

    @Override
    public List<EvictionRecord> setCapacity(int newCapacity) throws InvalidCacheCapacityException {
        if (newCapacity < CacheDefaults.MIN_CAPACITY || newCapacity > CacheDefaults.MAX_CAPACITY) {
            throw new InvalidCacheCapacityException("Capacity must be between 1 and 1,000,000",
                    "Capacity must be between 1 and 1,000,000.");
        }

        List<EvictionRecord> evictions = new ArrayList<>();
        List<CacheEvent> events = new ArrayList<>();

        synchronized (lock) {
            long now = clock.millis();
            purgeExpiredLocked(now, events);

            while (map.size() > newCapacity) {
                EvictionRecord ev = evictVictimLocked(now, events, EvictionRecord.EvictionReason.CAPACITY_REDUCED);
                if (ev == null) break;
                evictions.add(ev);
            }

            this.capacity = newCapacity;
            modificationCount++;
            events.add(new CacheEvent(CacheEvent.EventType.CAPACITY_CHANGE, null, "New capacity: " + newCapacity));
        }

        statistics.recordEvictions(evictions.size());
        dispatchEvents(events);

        return Collections.unmodifiableList(evictions);
    }

    @Override
    public int previewCapacityChange(int newCapacity) {
        if (newCapacity <= 0) return 0;
        synchronized (lock) {
            long now = clock.millis();
            purgeExpiredLocked(now, null);
            return Math.max(0, map.size() - newCapacity);
        }
    }

    @Override
    public void setEvictionPolicy(EvictionPolicyType type) {
        Objects.requireNonNull(type, "EvictionPolicyType cannot be null");
        List<CacheEvent> events = new ArrayList<>();

        synchronized (lock) {
            if (this.evictionPolicy.getType() == type) {
                return;
            }

            EvictionPolicy<K> newPolicy = createPolicyInstance(type);
            List<CacheEntrySnapshot<K, ?>> snapshots = new ArrayList<>(map.size());
            for (Map.Entry<K, CacheEntry<V>> entry : map.entrySet()) {
                snapshots.add(createSnapshot(entry.getKey(), entry.getValue()));
            }

            newPolicy.rebuild(snapshots);
            this.evictionPolicy = newPolicy;
            modificationCount++;
            events.add(new CacheEvent(CacheEvent.EventType.POLICY_CHANGE, null, "Switched to " + type));
        }

        dispatchEvents(events);
    }

    @Override
    public EvictionPolicyType getEvictionPolicyType() {
        synchronized (lock) {
            return evictionPolicy.getType();
        }
    }

    @Override
    public List<K> purgeExpired() {
        List<K> expiredKeys;
        List<CacheEvent> events = new ArrayList<>();
        synchronized (lock) {
            expiredKeys = purgeExpiredLocked(clock.millis(), events);
        }
        statistics.recordExpirations(expiredKeys.size());
        dispatchEvents(events);
        return expiredKeys;
    }

    private List<K> purgeExpiredLocked(long now, List<CacheEvent> events) {
        List<K> expired = expiryIndex.purgeExpired(now);
        for (K key : expired) {
            map.remove(key);
            evictionPolicy.onRemove(key);
            if (events != null) {
                events.add(new CacheEvent(CacheEvent.EventType.EXPIRE, key, "Expired"));
            }
        }
        if (!expired.isEmpty()) {
            modificationCount++;
        }
        return expired;
    }

    @Override
    public void replaceAll(List<CacheEntrySnapshot<K, V>> entries) throws CacheException {
        if (entries == null) {
            clear();
            return;
        }

        // Validate outside lock
        Set<K> seenKeys = new HashSet<>();
        for (CacheEntrySnapshot<K, V> entry : entries) {
            if (entry == null) {
                throw new InvalidCacheOperationException("Snapshot entry cannot be null", "Invalid snapshot entry.");
            }
            validateKeyAndValue(entry.getKey(), entry.getValue());
            if (!seenKeys.add(entry.getKey())) {
                throw new InvalidCacheOperationException("Duplicate key in restore list: " + entry.getKey(),
                        "Duplicate key encountered: " + entry.getKey());
            }
        }

        List<CacheEvent> events = new ArrayList<>();

        synchronized (lock) {
            map.clear();
            expiryIndex.clear();
            evictionPolicy.clear();

            for (CacheEntrySnapshot<K, V> snapshot : entries) {
                CacheEntry<V> entry = new CacheEntry<>(
                        snapshot.getValue(),
                        snapshot.getCreatedAt(),
                        snapshot.getLastAccessAt(),
                        snapshot.getExpiresAt(),
                        snapshot.getAccessCount(),
                        snapshot.getLastAccessAt(),
                        snapshot.getInsertionSequence()
                );
                map.put(snapshot.getKey(), entry);
                updateExpiryIndexLocked(snapshot.getKey(), snapshot.getExpiresAt(), snapshot.getInsertionSequence());
            }

            // Rebuild policy with restored snapshots
            List<CacheEntrySnapshot<K, ?>> snapshots = new ArrayList<>(entries);
            evictionPolicy.rebuild(snapshots);

            // Synchronize counters
            for (CacheEntrySnapshot<K, V> s : entries) {
                this.sequenceCounter = Math.max(this.sequenceCounter, s.getInsertionSequence());
            }
            modificationCount++;
            events.add(new CacheEvent(CacheEvent.EventType.CLEAR, null, "Replaced with " + entries.size() + " entries"));
        }

        dispatchEvents(events);
    }

    @Override
    public long getModificationCount() {
        return modificationCount;
    }

    private CacheEntrySnapshot<K, V> createSnapshot(K key, CacheEntry<V> entry) {
        return new CacheEntrySnapshot<>(
                key,
                entry.getValue(),
                entry.getCreatedAt(),
                entry.getLastAccessAt(),
                entry.getExpiresAt(),
                entry.getAccessCount(),
                entry.getInsertionSequence()
        );
    }
}
