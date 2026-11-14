package com.cse309.cache.eviction;

import java.util.Objects;

/**
 * Immutable audit record capturing details of an eviction event.
 */
public final class EvictionRecord {

    public enum EvictionReason {
        CAPACITY("Evicted due to cache reaching maximum capacity"),
        CAPACITY_REDUCED("Evicted due to manual reduction in cache capacity");

        private final String description;

        EvictionReason(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private final Object key;
    private final EvictionPolicyType policy;
    private final EvictionReason reason;
    private final long timestamp;
    private final long frequency;
    private final long lastAccessTick;
    private final String explanation;

    public EvictionRecord(Object key,
                          EvictionPolicyType policy,
                          EvictionReason reason,
                          long timestamp,
                          long frequency,
                          long lastAccessTick,
                          String explanation) {
        this.key = Objects.requireNonNull(key, "key cannot be null");
        this.policy = Objects.requireNonNull(policy, "policy cannot be null");
        this.reason = Objects.requireNonNull(reason, "reason cannot be null");
        this.timestamp = timestamp;
        this.frequency = frequency;
        this.lastAccessTick = lastAccessTick;
        this.explanation = Objects.requireNonNull(explanation, "explanation cannot be null");
    }

    public Object getKey() {
        return key;
    }

    public EvictionPolicyType getPolicy() {
        return policy;
    }

    public EvictionReason getReason() {
        return reason;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getFrequency() {
        return frequency;
    }

    public long getLastAccessTick() {
        return lastAccessTick;
    }

    public String getExplanation() {
        return explanation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EvictionRecord that)) return false;
        return timestamp == that.timestamp &&
                frequency == that.frequency &&
                lastAccessTick == that.lastAccessTick &&
                key.equals(that.key) &&
                policy == that.policy &&
                reason == that.reason &&
                explanation.equals(that.explanation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, policy, reason, timestamp, frequency, lastAccessTick, explanation);
    }

    @Override
    public String toString() {
        return String.format("[%s] Key '%s' evicted by %s (%s): %s",
                reason, key, policy, frequency > 0 ? "freq=" + frequency : "tick=" + lastAccessTick, explanation);
    }
}
