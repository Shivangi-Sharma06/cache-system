package com.cse309.cache.app;

import com.cse309.cache.config.CacheConfiguration;
import com.cse309.cache.config.ConfigurationStore;
import com.cse309.cache.config.ConfigurationValidator;
import com.cse309.cache.core.CacheEntrySnapshot;
import com.cse309.cache.core.CacheFactory;
import com.cse309.cache.core.StringCache;
import com.cse309.cache.event.CacheEvent;
import com.cse309.cache.event.CacheEventListener;
import com.cse309.cache.eviction.EvictionPolicyType;
import com.cse309.cache.eviction.EvictionRecord;
import com.cse309.cache.exception.CacheConfigurationException;
import com.cse309.cache.exception.CacheException;
import com.cse309.cache.exception.CachePersistenceException;
import com.cse309.cache.exception.InvalidCacheCapacityException;
import com.cse309.cache.expiry.CacheExpirationService;
import com.cse309.cache.history.OperationHistory;
import com.cse309.cache.logging.AppLogging;
import com.cse309.cache.persistence.DatabaseManager;
import com.cse309.cache.persistence.DbSnapshot;
import com.cse309.cache.persistence.PersistenceCoordinator;
import com.cse309.cache.persistence.PersistenceMode;
import com.cse309.cache.persistence.SnapshotFileManager;
import com.cse309.cache.simulation.SimulationController;
import com.cse309.cache.stats.CacheStatistics;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Composition root and central facade orchestrating cache engine, persistence,
 * expiration, statistics, history, and simulation subsystems.
 */
public final class CacheManager {

    private static final Logger LOGGER = AppLogging.getLogger(CacheManager.class);

    private final Clock clock;
    private final StringCache cache;
    private final CacheExpirationService expirationService;
    private final OperationHistory history;
    private final PersistenceCoordinator persistenceCoordinator;
    private final SimulationController simulationController;
    private volatile CacheConfiguration configuration;

    public CacheManager(CacheConfiguration config, Clock clock) {
        this.clock = clock != null ? clock : Clock.systemUTC();
        this.configuration = config != null ? config : new CacheConfiguration();

        // 1. Initialize Cache
        this.cache = CacheFactory.createStringCache(this.configuration, this.clock);

        // 2. Initialize Expiration Service
        this.expirationService = new CacheExpirationService(this.cache, this.configuration.getCleanupInterval());
        this.expirationService.start();

        // 3. Initialize History
        this.history = new OperationHistory(this.configuration.getHistoryCapacity());

        // 4. Initialize Persistence
        DatabaseManager dbManager = new DatabaseManager(AppPaths.getDatabaseFile());
        this.persistenceCoordinator = new PersistenceCoordinator(dbManager, this.configuration.getPersistenceMode());
        this.persistenceCoordinator.start();

        // 5. Initialize Simulation Controller
        this.simulationController = new SimulationController(this.cache, this.history);

        // Forward cache mutation events to history and persistence coordinator
        this.cache.addListener(new CacheEventListener() {
            @Override
            public void onEvent(CacheEvent event) {
                if (event.getType() == CacheEvent.EventType.EVICT && event.getDetails() instanceof EvictionRecord ev) {
                    history.record(com.cse309.cache.history.OperationRecord.create(
                            "EVICT", String.valueOf(ev.getKey()), ev.getReason().name(), 0L, ev.getExplanation()
                    ));
                } else if (event.getType() == CacheEvent.EventType.EXPIRE) {
                    history.record(com.cse309.cache.history.OperationRecord.create(
                            "EXPIRE", String.valueOf(event.getKey()), "EXPIRED", 0L, "Time to live elapsed"
                    ));
                }
            }
        });

        // Warm start if persistent mode
        if (this.configuration.getPersistenceMode() == PersistenceMode.PERSISTENT) {
            Optional<DbSnapshot> latest = persistenceCoordinator.loadLatestSnapshot();
            if (latest.isPresent()) {
                try {
                    persistenceCoordinator.restoreSnapshot(cache, latest.get(), this.clock);
                    LOGGER.info("Warm start: restored latest snapshot #" + latest.get().getId());
                } catch (Exception e) {
                    LOGGER.warning("Failed to restore latest snapshot on startup: " + e.getMessage());
                }
            }
        }
    }

    public CacheManager(CacheConfiguration config) {
        this(config, Clock.systemUTC());
    }

    public CacheManager() {
        this(ConfigurationStore.load(AppPaths.getConfigFile()), Clock.systemUTC());
    }

    public StringCache getCache() {
        return cache;
    }

    public CacheStatistics getStatistics() {
        return cache.getStatistics();
    }

    public CacheExpirationService getExpirationService() {
        return expirationService;
    }

    public OperationHistory getHistory() {
        return history;
    }

    public PersistenceCoordinator getPersistenceCoordinator() {
        return persistenceCoordinator;
    }

    public SimulationController getSimulationController() {
        return simulationController;
    }

    public CacheConfiguration getConfiguration() {
        return configuration;
    }

    public Clock getClock() {
        return clock;
    }

    /**
     * Applies new configuration atomically. Validates all settings first.
     */
    public synchronized void applyConfiguration(CacheConfiguration newConfig) throws CacheException {
        ConfigurationValidator.validate(newConfig);

        // 1. Capacity
        if (newConfig.getCapacity() != configuration.getCapacity()) {
            cache.setCapacity(newConfig.getCapacity());
        }

        // 2. Policy
        if (newConfig.getEvictionPolicy() != configuration.getEvictionPolicy()) {
            cache.setEvictionPolicy(newConfig.getEvictionPolicy());
        }

        // 3. Expiration Interval
        if (!newConfig.getCleanupInterval().equals(configuration.getCleanupInterval())) {
            expirationService.setInterval(newConfig.getCleanupInterval());
        }

        // 4. Persistence Mode
        if (newConfig.getPersistenceMode() != configuration.getPersistenceMode()) {
            persistenceCoordinator.setMode(newConfig.getPersistenceMode());
        }

        // 5. History Capacity
        if (newConfig.getHistoryCapacity() != configuration.getHistoryCapacity()) {
            history.setCapacity(newConfig.getHistoryCapacity());
        }

        this.configuration = newConfig;
        LOGGER.info("Configuration updated successfully: " + newConfig);
    }

    public void saveConfigurationAsStartupDefaults() throws IOException {
        ConfigurationStore.save(AppPaths.getConfigFile(), this.configuration);
    }

    public DbSnapshot saveDatabaseSnapshot(String name) throws CachePersistenceException {
        return persistenceCoordinator.saveSnapshot(cache, name);
    }

    public void saveBinarySnapshot(Path targetFile) throws CachePersistenceException {
        List<CacheEntrySnapshot<String, String>> entries = cache.getAllEntries();
        SnapshotFileManager.save(targetFile, cache.getCapacity(), cache.getEvictionPolicyType(), entries);
    }

    public void loadBinarySnapshot(Path sourceFile) throws CacheException {
        SnapshotFileManager.SnapshotFileContent content = SnapshotFileManager.load(sourceFile);
        if (content.capacity() != cache.getCapacity()) {
            cache.setCapacity(content.capacity());
        }
        if (content.policyType() != cache.getEvictionPolicyType()) {
            cache.setEvictionPolicy(content.policyType());
        }
        cache.replaceAll(content.entries());
    }

    public synchronized void shutdown() {
        LOGGER.info("Shutting down CacheManager subsystems in order...");

        // 1. Stop simulation
        try {
            if (simulationController.isActive()) {
                simulationController.stop();
            }
        } catch (Exception e) {
            LOGGER.warning("Error stopping simulation during shutdown: " + e.getMessage());
        }

        // 2. Stop expiration daemon
        try {
            expirationService.stop();
        } catch (Exception e) {
            LOGGER.warning("Error stopping expiration service during shutdown: " + e.getMessage());
        }

        // 3. Save shutdown snapshot if in persistent mode
        if (configuration.getPersistenceMode() == PersistenceMode.PERSISTENT && !cache.isEmpty()) {
            try {
                persistenceCoordinator.saveSnapshot(cache, "shutdown-snapshot-" + System.currentTimeMillis());
                LOGGER.info("Saved graceful shutdown snapshot");
            } catch (Exception e) {
                LOGGER.warning("Failed to save shutdown snapshot: " + e.getMessage());
            }
        }

        // 4. Flush and stop persistence coordinator
        try {
            persistenceCoordinator.stop();
        } catch (Exception e) {
            LOGGER.warning("Error stopping persistence coordinator during shutdown: " + e.getMessage());
        }

        LOGGER.info("CacheManager shutdown complete.");
    }
}
