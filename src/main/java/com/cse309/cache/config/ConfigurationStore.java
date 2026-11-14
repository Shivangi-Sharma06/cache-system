package com.cse309.cache.config;

import com.cse309.cache.app.AppPaths;
import com.cse309.cache.eviction.EvictionPolicyType;
import com.cse309.cache.logging.AppLogging;
import com.cse309.cache.persistence.PersistenceMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Reads and writes CacheConfiguration using java.util.Properties with
 * FileInputStream and FileOutputStream byte streams (Unit 3, Unit 4 Properties).
 */
public final class ConfigurationStore {

    private static final Logger LOGGER = AppLogging.getLogger(ConfigurationStore.class);

    private static final String KEY_CAPACITY = "cache.capacity";
    private static final String KEY_POLICY = "cache.eviction.policy";
    private static final String KEY_DEFAULT_TTL = "cache.default.ttl.seconds";
    private static final String KEY_CLEANUP_INTERVAL = "cache.cleanup.interval.millis";
    private static final String KEY_WORKER_THREADS = "cache.workers.count";
    private static final String KEY_PERSISTENCE_MODE = "cache.persistence.mode";
    private static final String KEY_SNAPSHOT_DIR = "cache.snapshot.directory";
    private static final String KEY_HISTORY_CAPACITY = "cache.history.capacity";

    private ConfigurationStore() {
        // Utility class
    }

    public static CacheConfiguration load(Path configFile) {
        CacheConfiguration defaultConfig = new CacheConfiguration();
        if (configFile == null || !Files.exists(configFile)) {
            LOGGER.info("Config file not found at " + configFile + "; using default configuration.");
            return defaultConfig;
        }

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile.toFile())) {
            props.load(fis);

            CacheConfiguration.Builder builder = CacheConfiguration.builder();

            if (props.containsKey(KEY_CAPACITY)) {
                builder.capacity(Integer.parseInt(props.getProperty(KEY_CAPACITY)));
            }
            if (props.containsKey(KEY_POLICY)) {
                builder.evictionPolicy(EvictionPolicyType.valueOf(props.getProperty(KEY_POLICY)));
            }
            if (props.containsKey(KEY_DEFAULT_TTL)) {
                long ttlSec = Long.parseLong(props.getProperty(KEY_DEFAULT_TTL));
                builder.defaultTtl(ttlSec > 0 ? Duration.ofSeconds(ttlSec) : null);
            }
            if (props.containsKey(KEY_CLEANUP_INTERVAL)) {
                long intervalMillis = Long.parseLong(props.getProperty(KEY_CLEANUP_INTERVAL));
                builder.cleanupInterval(Duration.ofMillis(intervalMillis));
            }
            if (props.containsKey(KEY_WORKER_THREADS)) {
                builder.workerThreads(Integer.parseInt(props.getProperty(KEY_WORKER_THREADS)));
            }
            if (props.containsKey(KEY_PERSISTENCE_MODE)) {
                builder.persistenceMode(PersistenceMode.valueOf(props.getProperty(KEY_PERSISTENCE_MODE)));
            }
            if (props.containsKey(KEY_SNAPSHOT_DIR)) {
                builder.snapshotDirectory(Path.of(props.getProperty(KEY_SNAPSHOT_DIR)));
            }
            if (props.containsKey(KEY_HISTORY_CAPACITY)) {
                builder.historyCapacity(Integer.parseInt(props.getProperty(KEY_HISTORY_CAPACITY)));
            }

            CacheConfiguration loaded = builder.build();
            ConfigurationValidator.validate(loaded);
            LOGGER.info("Loaded configuration successfully from " + configFile);
            return loaded;

        } catch (Exception e) {
            LOGGER.warning("Failed to load or parse config file " + configFile + " (" + e.getMessage() + "); falling back to defaults.");
            return defaultConfig;
        }
    }

    public static void save(Path configFile, CacheConfiguration config) throws IOException {
        if (configFile == null || config == null) return;

        Path parent = configFile.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        Properties props = new Properties();
        props.setProperty(KEY_CAPACITY, String.valueOf(config.getCapacity()));
        props.setProperty(KEY_POLICY, config.getEvictionPolicy().name());
        props.setProperty(KEY_DEFAULT_TTL, String.valueOf(config.getDefaultTtl().map(Duration::getSeconds).orElse(-1L)));
        props.setProperty(KEY_CLEANUP_INTERVAL, String.valueOf(config.getCleanupInterval().toMillis()));
        props.setProperty(KEY_WORKER_THREADS, String.valueOf(config.getWorkerThreads()));
        props.setProperty(KEY_PERSISTENCE_MODE, config.getPersistenceMode().name());
        props.setProperty(KEY_SNAPSHOT_DIR, config.getSnapshotDirectory().toString());
        props.setProperty(KEY_HISTORY_CAPACITY, String.valueOf(config.getHistoryCapacity()));

        try (FileOutputStream fos = new FileOutputStream(configFile.toFile())) {
            props.store(fos, "Multithreaded In-Memory Caching System Configuration (CSE 309)");
            LOGGER.info("Saved configuration defaults to " + configFile);
        }
    }
}
