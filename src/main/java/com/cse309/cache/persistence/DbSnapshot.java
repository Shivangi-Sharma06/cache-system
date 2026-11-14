package com.cse309.cache.persistence;

import com.cse309.cache.core.CacheEntrySnapshot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representation of a database snapshot header and its contained entry snapshots.
 */
public final class DbSnapshot {

    private final Long id;
    private final String name;
    private final long createdAt;
    private final String evictionPolicy;
    private final int capacity;
    private final int entryCount;
    private final List<CacheEntrySnapshot<String, String>> entries;

    public DbSnapshot(Long id,
                      String name,
                      long createdAt,
                      String evictionPolicy,
                      int capacity,
                      int entryCount,
                      List<CacheEntrySnapshot<String, String>> entries) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.evictionPolicy = evictionPolicy;
        this.capacity = capacity;
        this.entryCount = entryCount;
        this.entries = entries != null
                ? Collections.unmodifiableList(new ArrayList<>(entries))
                : Collections.emptyList();
    }

    public static DbSnapshot createNew(String name,
                                       String evictionPolicy,
                                       int capacity,
                                       List<CacheEntrySnapshot<String, String>> entries) {
        return new DbSnapshot(null, name, System.currentTimeMillis(), evictionPolicy,
                capacity, entries != null ? entries.size() : 0, entries);
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public long getCreatedAt() { return createdAt; }
    public Instant getCreatedInstant() { return Instant.ofEpochMilli(createdAt); }
    public String getEvictionPolicy() { return evictionPolicy; }
    public int getCapacity() { return capacity; }
    public int getEntryCount() { return entryCount; }
    public List<CacheEntrySnapshot<String, String>> getEntries() { return entries; }

    @Override
    public String toString() {
        return String.format("Snapshot#%d '%s' (%s, cap=%d, entries=%d)",
                id, name, evictionPolicy, capacity, entryCount);
    }
}
