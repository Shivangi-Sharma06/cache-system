package com.cse309.cache.eviction;

import java.util.Objects;

/**
 * Abstract base class holding common state and metadata for eviction policies.
 *
 * @param <K> the type of key
 */
public abstract class AbstractEvictionPolicy<K> implements EvictionPolicy<K> {

    protected final EvictionPolicyType type;

    protected AbstractEvictionPolicy(EvictionPolicyType type) {
        this.type = Objects.requireNonNull(type, "EvictionPolicyType cannot be null");
    }

    @Override
    public final EvictionPolicyType getType() {
        return type;
    }

    @Override
    public String toString() {
        return type.getDisplayName() + " (tracked=" + size() + ")";
    }
}
