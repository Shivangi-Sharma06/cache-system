package com.cse309.cache.simulation;

/**
 * Key access distribution strategies for cache workload simulation.
 */
public enum KeyDistribution {
    UNIFORM("Uniform Random", "Keys selected with equal probability across the entire key space."),
    HOTSPOT_80_20("80/20 Hotspot", "80% of operations access 20% of the keys, simulating skewed access."),
    SEQUENTIAL_SCAN("Sequential Scan", "Keys accessed in sequential loop order, stressing eviction policies.");

    private final String displayName;
    private final String description;

    KeyDistribution(String displayName, String description) {
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
