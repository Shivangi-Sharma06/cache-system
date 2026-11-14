package com.cse309.cache.config;

import com.cse309.cache.core.CacheDefaults;
import com.cse309.cache.exception.CacheConfigurationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Plain Java validator for CacheConfiguration.
 * Evaluates all configuration fields atomically and aggregates all validation errors.
 */
public final class ConfigurationValidator {

    private ConfigurationValidator() {
        // Utility class
    }

    /**
     * Validates the configuration and throws CacheConfigurationException if any errors are found.
     *
     * @param config the configuration to validate
     * @throws CacheConfigurationException if one or more validation errors exist
     */
    public static void validate(CacheConfiguration config) throws CacheConfigurationException {
        List<String> errors = collectErrors(config);
        if (!errors.isEmpty()) {
            throw new CacheConfigurationException(errors);
        }
    }

    /**
     * Collects all validation errors for a configuration without throwing.
     *
     * @param config the configuration to inspect
     * @return list of error description strings, empty if completely valid
     */
    public static List<String> collectErrors(CacheConfiguration config) {
        List<String> errors = new ArrayList<>();
        if (config == null) {
            errors.add("Configuration cannot be null");
            return errors;
        }

        // 1. Capacity
        if (config.getCapacity() < CacheDefaults.MIN_CAPACITY || config.getCapacity() > CacheDefaults.MAX_CAPACITY) {
            errors.add(String.format("Capacity must be between %d and %,d (was %d)",
                    CacheDefaults.MIN_CAPACITY, CacheDefaults.MAX_CAPACITY, config.getCapacity()));
        }

        // 2. Eviction policy
        if (config.getEvictionPolicy() == null) {
            errors.add("Eviction policy cannot be null");
        }

        // 3. Default TTL
        Duration defaultTtl = config.getDefaultTtlOrNull();
        if (defaultTtl != null) {
            if (defaultTtl.compareTo(CacheDefaults.MIN_TTL) < 0 || defaultTtl.compareTo(CacheDefaults.MAX_TTL) > 0) {
                errors.add(String.format("Default TTL must be between %d second and %d days (was %s)",
                        CacheDefaults.MIN_TTL.getSeconds(),
                        CacheDefaults.MAX_TTL.toDays(),
                        defaultTtl));
            }
        }

        // 4. Cleanup interval
        Duration cleanupInterval = config.getCleanupInterval();
        if (cleanupInterval == null) {
            errors.add("Cleanup interval cannot be null");
        } else if (cleanupInterval.compareTo(CacheDefaults.MIN_CLEANUP_INTERVAL) < 0 ||
                   cleanupInterval.compareTo(CacheDefaults.MAX_CLEANUP_INTERVAL) > 0) {
            errors.add(String.format("Cleanup interval must be between %d ms and %d hour (was %s)",
                    CacheDefaults.MIN_CLEANUP_INTERVAL.toMillis(),
                    CacheDefaults.MAX_CLEANUP_INTERVAL.toHours(),
                    cleanupInterval));
        }

        // 5. Worker threads
        if (config.getWorkerThreads() < CacheDefaults.MIN_WORKERS || config.getWorkerThreads() > CacheDefaults.MAX_WORKERS) {
            errors.add(String.format("Worker threads count must be between %d and %d (was %d)",
                    CacheDefaults.MIN_WORKERS, CacheDefaults.MAX_WORKERS, config.getWorkerThreads()));
        }

        // 6. Persistence mode
        if (config.getPersistenceMode() == null) {
            errors.add("Persistence mode cannot be null");
        }

        // 7. Snapshot directory
        Path snapshotDir = config.getSnapshotDirectory();
        if (snapshotDir == null) {
            errors.add("Snapshot directory cannot be null");
        } else {
            try {
                if (Files.exists(snapshotDir)) {
                    if (!Files.isDirectory(snapshotDir)) {
                        errors.add("Snapshot path exists but is not a directory: " + snapshotDir);
                    } else if (!Files.isWritable(snapshotDir)) {
                        errors.add("Snapshot directory is not writable: " + snapshotDir);
                    }
                } else {
                    // Test if parent is writable or creatable
                    Path parent = snapshotDir.getParent();
                    if (parent != null && Files.exists(parent) && !Files.isWritable(parent)) {
                        errors.add("Cannot create snapshot directory (parent unwritable): " + snapshotDir);
                    }
                }
            } catch (Exception e) {
                errors.add("Unable to access snapshot directory path: " + e.getMessage());
            }
        }

        // 8. History capacity
        if (config.getHistoryCapacity() < CacheDefaults.MIN_HISTORY_CAPACITY ||
            config.getHistoryCapacity() > CacheDefaults.MAX_HISTORY_CAPACITY) {
            errors.add(String.format("History capacity must be between %d and %,d (was %d)",
                    CacheDefaults.MIN_HISTORY_CAPACITY, CacheDefaults.MAX_HISTORY_CAPACITY, config.getHistoryCapacity()));
        }

        return errors;
    }
}
