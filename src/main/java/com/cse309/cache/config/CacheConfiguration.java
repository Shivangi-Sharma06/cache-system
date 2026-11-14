package com.cse309.cache.config;

import com.cse309.cache.app.AppPaths;
import com.cse309.cache.core.CacheDefaults;
import com.cse309.cache.eviction.EvictionPolicyType;
import com.cse309.cache.persistence.PersistenceMode;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable configuration for the caching system.
 * Features constructor overloading and constructor chaining via this(...),
 * as well as a fluent nested static Builder.
 */
public final class CacheConfiguration {

    private final int capacity;
    private final EvictionPolicyType evictionPolicy;
    private final Duration defaultTtl; // nullable, null means never expires by default
    private final Duration cleanupInterval;
    private final int workerThreads;
    private final PersistenceMode persistenceMode;
    private final Path snapshotDirectory;
    private final int historyCapacity;

    /**
     * Primary full constructor.
     */
    public CacheConfiguration(int capacity,
                              EvictionPolicyType evictionPolicy,
                              Duration defaultTtl,
                              Duration cleanupInterval,
                              int workerThreads,
                              PersistenceMode persistenceMode,
                              Path snapshotDirectory,
                              int historyCapacity) {
        this.capacity = capacity;
        this.evictionPolicy = Objects.requireNonNull(evictionPolicy, "evictionPolicy cannot be null");
        this.defaultTtl = defaultTtl;
        this.cleanupInterval = Objects.requireNonNull(cleanupInterval, "cleanupInterval cannot be null");
        this.workerThreads = workerThreads;
        this.persistenceMode = Objects.requireNonNull(persistenceMode, "persistenceMode cannot be null");
        this.snapshotDirectory = Objects.requireNonNull(snapshotDirectory, "snapshotDirectory cannot be null");
        this.historyCapacity = historyCapacity;
    }

    /**
     * Overloaded constructor chaining with default persistence, directory, and history.
     */
    public CacheConfiguration(int capacity,
                              EvictionPolicyType evictionPolicy,
                              Duration defaultTtl,
                              Duration cleanupInterval,
                              int workerThreads) {
        this(capacity,
             evictionPolicy,
             defaultTtl,
             cleanupInterval,
             workerThreads,
             PersistenceMode.IN_MEMORY_ONLY,
             AppPaths.getSnapshotsDir(),
             CacheDefaults.DEFAULT_HISTORY_CAPACITY);
    }

    /**
     * Overloaded constructor chaining with default workers and persistence settings.
     */
    public CacheConfiguration(int capacity, EvictionPolicyType evictionPolicy, Duration defaultTtl) {
        this(capacity,
             evictionPolicy,
             defaultTtl,
             CacheDefaults.DEFAULT_CLEANUP_INTERVAL,
             CacheDefaults.DEFAULT_SIMULATOR_WORKERS);
    }

    /**
     * Overloaded constructor chaining with default TTL.
     */
    public CacheConfiguration(int capacity, EvictionPolicyType evictionPolicy) {
        this(capacity, evictionPolicy, CacheDefaults.DEFAULT_TTL);
    }

    /**
     * Default constructor chaining with all standard defaults.
     */
    public CacheConfiguration() {
        this(CacheDefaults.DEFAULT_CAPACITY, EvictionPolicyType.LRU);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(CacheConfiguration prototype) {
        return new Builder(prototype);
    }

    public int getCapacity() {
        return capacity;
    }

    public EvictionPolicyType getEvictionPolicy() {
        return evictionPolicy;
    }

    public Optional<Duration> getDefaultTtl() {
        return Optional.ofNullable(defaultTtl);
    }

    public Duration getDefaultTtlOrNull() {
        return defaultTtl;
    }

    public Duration getCleanupInterval() {
        return cleanupInterval;
    }

    public int getWorkerThreads() {
        return workerThreads;
    }

    public PersistenceMode getPersistenceMode() {
        return persistenceMode;
    }

    public Path getSnapshotDirectory() {
        return snapshotDirectory;
    }

    public int getHistoryCapacity() {
        return historyCapacity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CacheConfiguration that)) return false;
        return capacity == that.capacity &&
                workerThreads == that.workerThreads &&
                historyCapacity == that.historyCapacity &&
                evictionPolicy == that.evictionPolicy &&
                Objects.equals(defaultTtl, that.defaultTtl) &&
                cleanupInterval.equals(that.cleanupInterval) &&
                persistenceMode == that.persistenceMode &&
                snapshotDirectory.equals(that.snapshotDirectory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(capacity, evictionPolicy, defaultTtl, cleanupInterval,
                workerThreads, persistenceMode, snapshotDirectory, historyCapacity);
    }

    @Override
    public String toString() {
        return String.format("CacheConfiguration{capacity=%d, policy=%s, defaultTtl=%s, cleanup=%s, workers=%d, mode=%s}",
                capacity, evictionPolicy, defaultTtl != null ? defaultTtl.getSeconds() + "s" : "none",
                cleanupInterval.toMillis() + "ms", workerThreads, persistenceMode);
    }

    /**
     * Fluent nested builder for CacheConfiguration.
     */
    public static final class Builder {
        private int capacity = CacheDefaults.DEFAULT_CAPACITY;
        private EvictionPolicyType evictionPolicy = EvictionPolicyType.LRU;
        private Duration defaultTtl = CacheDefaults.DEFAULT_TTL;
        private Duration cleanupInterval = CacheDefaults.DEFAULT_CLEANUP_INTERVAL;
        private int workerThreads = CacheDefaults.DEFAULT_SIMULATOR_WORKERS;
        private PersistenceMode persistenceMode = PersistenceMode.IN_MEMORY_ONLY;
        private Path snapshotDirectory = AppPaths.getSnapshotsDir();
        private int historyCapacity = CacheDefaults.DEFAULT_HISTORY_CAPACITY;

        public Builder() {
        }

        public Builder(CacheConfiguration prototype) {
            if (prototype != null) {
                this.capacity = prototype.getCapacity();
                this.evictionPolicy = prototype.getEvictionPolicy();
                this.defaultTtl = prototype.getDefaultTtlOrNull();
                this.cleanupInterval = prototype.getCleanupInterval();
                this.workerThreads = prototype.getWorkerThreads();
                this.persistenceMode = prototype.getPersistenceMode();
                this.snapshotDirectory = prototype.getSnapshotDirectory();
                this.historyCapacity = prototype.getHistoryCapacity();
            }
        }

        public Builder capacity(int capacity) {
            this.capacity = capacity;
            return this;
        }

        public Builder evictionPolicy(EvictionPolicyType evictionPolicy) {
            this.evictionPolicy = evictionPolicy;
            return this;
        }

        public Builder defaultTtl(Duration defaultTtl) {
            this.defaultTtl = defaultTtl;
            return this;
        }

        public Builder cleanupInterval(Duration cleanupInterval) {
            this.cleanupInterval = cleanupInterval;
            return this;
        }

        public Builder workerThreads(int workerThreads) {
            this.workerThreads = workerThreads;
            return this;
        }

        public Builder persistenceMode(PersistenceMode persistenceMode) {
            this.persistenceMode = persistenceMode;
            return this;
        }

        public Builder snapshotDirectory(Path snapshotDirectory) {
            this.snapshotDirectory = snapshotDirectory;
            return this;
        }

        public Builder historyCapacity(int historyCapacity) {
            this.historyCapacity = historyCapacity;
            return this;
        }

        public CacheConfiguration build() {
            return new CacheConfiguration(capacity, evictionPolicy, defaultTtl,
                    cleanupInterval, workerThreads, persistenceMode, snapshotDirectory, historyCapacity);
        }
    }
}
