package com.cse309.cache.eviction;

/**
 * Supported eviction policy algorithms.
 */
public enum EvictionPolicyType {
    LRU("Least Recently Used", "Evicts the entry that has not been accessed for the longest time."),
    FIFO("First In First Out", "Evicts the entry that was inserted earliest, maintaining queue order."),
    LFU("Least Frequently Used", "Evicts the entry with the fewest accesses, tie-breaking by earliest insertion.");

    private final String displayName;
    private final String description;

    EvictionPolicyType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
