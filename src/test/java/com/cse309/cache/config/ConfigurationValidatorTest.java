package com.cse309.cache.config;

import com.cse309.cache.eviction.EvictionPolicyType;
import com.cse309.cache.exception.CacheConfigurationException;
import com.cse309.cache.persistence.PersistenceMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationValidatorTest {

    @TempDir
    Path tempDir;

    @Test
    void testDefaultConfigurationIsValid() {
        CacheConfiguration config = new CacheConfiguration();
        assertDoesNotThrow(() -> ConfigurationValidator.validate(config));
    }

    @Test
    void testValidCustomConfiguration() {
        CacheConfiguration config = CacheConfiguration.builder()
                .capacity(500)
                .evictionPolicy(EvictionPolicyType.LFU)
                .defaultTtl(Duration.ofMinutes(10))
                .cleanupInterval(Duration.ofSeconds(2))
                .workerThreads(8)
                .persistenceMode(PersistenceMode.PERSISTENT)
                .snapshotDirectory(tempDir)
                .historyCapacity(2000)
                .build();

        assertDoesNotThrow(() -> ConfigurationValidator.validate(config));
    }

    @Test
    void testNullConfigurationThrows() {
        List<String> errors = ConfigurationValidator.collectErrors(null);
        assertFalse(errors.isEmpty());
        assertThrows(CacheConfigurationException.class, () -> ConfigurationValidator.validate(null));
    }

    @Test
    void testMultipleValidationErrorsCollected() {
        CacheConfiguration badConfig = CacheConfiguration.builder()
                .capacity(0) // Invalid (< 1)
                .workerThreads(100) // Invalid (> 64)
                .historyCapacity(50) // Invalid (< 100)
                .defaultTtl(Duration.ofMillis(500)) // Invalid (< 1s)
                .cleanupInterval(Duration.ofMillis(50)) // Invalid (< 100ms)
                .build();

        List<String> errors = ConfigurationValidator.collectErrors(badConfig);
        assertTrue(errors.size() >= 5, "Expected at least 5 errors, found: " + errors.size());

        CacheConfigurationException ex = assertThrows(CacheConfigurationException.class,
                () -> ConfigurationValidator.validate(badConfig));
        assertEquals(errors.size(), ex.getValidationErrors().size());
        assertTrue(ex.getUserMessage().contains("Capacity"));
    }

    @Test
    void testBoundaryCapacities() {
        // Min capacity
        CacheConfiguration minCap = CacheConfiguration.builder().capacity(1).build();
        assertDoesNotThrow(() -> ConfigurationValidator.validate(minCap));

        // Max capacity
        CacheConfiguration maxCap = CacheConfiguration.builder().capacity(1_000_000).build();
        assertDoesNotThrow(() -> ConfigurationValidator.validate(maxCap));

        // Over max
        CacheConfiguration overMax = CacheConfiguration.builder().capacity(1_000_001).build();
        assertThrows(CacheConfigurationException.class, () -> ConfigurationValidator.validate(overMax));
    }
}
