package com.cse309.cache.persistence;

/**
 * Operating mode for cache persistence.
 */
public enum PersistenceMode {
    IN_MEMORY_ONLY("In-Memory Only", "No automatic database snapshots or background writes."),
    PERSISTENT("Persistent", "Warm start from latest snapshot, periodic autosaves, and history logging.");

    private final String displayName;
    private final String description;

    PersistenceMode(String displayName, String description) {
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
