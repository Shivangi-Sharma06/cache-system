package com.cse309.cache.app;

import com.cse309.cache.config.CacheConfiguration;
import com.cse309.cache.persistence.PersistenceMode;
import com.cse309.cache.simulation.KeyDistribution;
import com.cse309.cache.simulation.OperationMix;
import com.cse309.cache.simulation.SimulationConfig;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies Phase 9 hardening requirement:
 * Confirms that after CacheManager.shutdown(), no non-daemon application threads
 * (or worker threads) remain alive.
 */
class ShutdownTest {

    @Test
    void testCleanShutdownWithNoActiveSimulation() {
        CacheConfiguration config = new CacheConfiguration.Builder()
                .capacity(100)
                .cleanupInterval(Duration.ofMillis(200))
                .persistenceMode(PersistenceMode.IN_MEMORY_ONLY)
                .build();

        CacheManager manager = new CacheManager(config, Clock.systemUTC());
        assertTrue(manager.getExpirationService().isRunning(), "Expiration service should be running initially");

        manager.shutdown();

        assertFalse(manager.getExpirationService().isRunning(), "Expiration service should be stopped");

        // Verify thread states
        Set<Thread> liveAppThreads = Thread.getAllStackTraces().keySet().stream()
                .filter(Thread::isAlive)
                .filter(t -> t.getName().startsWith("cache-"))
                .collect(Collectors.toSet());

        // Any remaining thread starting with "cache-" must be non-existent or terminated
        assertTrue(liveAppThreads.isEmpty(), "No cache-* threads should remain alive after shutdown: " + liveAppThreads);
    }

    @Test
    void testCleanShutdownDuringActiveSimulation() throws Exception {
        CacheConfiguration config = new CacheConfiguration.Builder()
                .capacity(200)
                .cleanupInterval(Duration.ofMillis(300))
                .persistenceMode(PersistenceMode.IN_MEMORY_ONLY)
                .build();

        CacheManager manager = new CacheManager(config, Clock.systemUTC());

        SimulationConfig simConfig = new SimulationConfig(
                8,
                50_000,
                OperationMix.MIXED,
                KeyDistribution.HOTSPOT_80_20,
                500,
                256,
                12345L,
                false,
                false
        );

        manager.getSimulationController().start(simConfig);
        assertTrue(manager.getSimulationController().isActive(), "Simulation should be active");

        // Let simulation run for a moment so workers are processing
        Thread.sleep(80);

        // Initiate shutdown
        manager.shutdown();

        assertFalse(manager.getSimulationController().isActive(), "Simulation must be inactive after shutdown");

        // Verify that no worker or producer threads remain alive
        Set<Thread> liveAppThreads = Thread.getAllStackTraces().keySet().stream()
                .filter(Thread::isAlive)
                .filter(t -> t.getName().startsWith("cache-"))
                .collect(Collectors.toSet());

        assertTrue(liveAppThreads.isEmpty(),
                "No cache-* threads (workers, producer, expiry, persistence) should remain alive after shutdown: " + liveAppThreads);
    }

    @Test
    void testNoNonDaemonAppThreadsRemain() throws Exception {
        CacheConfiguration config = new CacheConfiguration.Builder()
                .capacity(50)
                .persistenceMode(PersistenceMode.IN_MEMORY_ONLY)
                .build();

        CacheManager manager = new CacheManager(config, Clock.systemUTC());
        manager.getCache().put("alpha", "1");
        manager.getCache().put("beta", "2");

        manager.shutdown();

        Set<Thread> nonDaemonAppThreads = Thread.getAllStackTraces().keySet().stream()
                .filter(Thread::isAlive)
                .filter(t -> !t.isDaemon())
                .filter(t -> t.getName().startsWith("cache-"))
                .collect(Collectors.toSet());

        assertTrue(nonDaemonAppThreads.isEmpty(),
                "No non-daemon cache application threads should remain: " + nonDaemonAppThreads);
    }
}
