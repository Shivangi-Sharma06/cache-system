package com.cse309.cache.config;

import com.cse309.cache.eviction.EvictionPolicyType;
import com.cse309.cache.persistence.PersistenceMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void testSaveAndLoadConfiguration() throws Exception {
        Path configFile = tempDir.resolve("config.properties");

        CacheConfiguration original = CacheConfiguration.builder()
                .capacity(250)
                .evictionPolicy(EvictionPolicyType.LFU)
                .defaultTtl(Duration.ofSeconds(600))
                .cleanupInterval(Duration.ofSeconds(10))
                .workerThreads(8)
                .persistenceMode(PersistenceMode.PERSISTENT)
                .historyCapacity(5000)
                .build();

        ConfigurationStore.save(configFile, original);
        assertTrue(Files.exists(configFile));

        CacheConfiguration loaded = ConfigurationStore.load(configFile);
        assertNotNull(loaded);
        assertEquals(250, loaded.getCapacity());
        assertEquals(EvictionPolicyType.LFU, loaded.getEvictionPolicy());
        assertEquals(Duration.ofSeconds(600), loaded.getDefaultTtl().orElse(null));
        assertEquals(Duration.ofSeconds(10), loaded.getCleanupInterval());
        assertEquals(8, loaded.getWorkerThreads());
        assertEquals(PersistenceMode.PERSISTENT, loaded.getPersistenceMode());
        assertEquals(5000, loaded.getHistoryCapacity());
    }

    @Test
    void testMissingConfigFileReturnsDefaults() {
        Path missing = tempDir.resolve("non_existent.properties");
        CacheConfiguration config = ConfigurationStore.load(missing);
        assertNotNull(config);
        assertEquals(100, config.getCapacity());
        assertEquals(EvictionPolicyType.LRU, config.getEvictionPolicy());
    }

    @Test
    void testCorruptConfigFileFallsBackToDefaults() throws Exception {
        Path corrupt = tempDir.resolve("corrupt.properties");
        Files.writeString(corrupt, "cache.capacity=NOT_A_NUMBER\ncache.eviction.policy=INVALID_POLICY");

        CacheConfiguration config = ConfigurationStore.load(corrupt);
        assertNotNull(config);
        assertEquals(100, config.getCapacity());
        assertEquals(EvictionPolicyType.LRU, config.getEvictionPolicy());
    }
}
