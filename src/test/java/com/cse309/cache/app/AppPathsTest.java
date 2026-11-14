package com.cse309.cache.app;

import com.cse309.cache.logging.AppLogging;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppPathsTest {

    @Test
    void testDirectoriesCreation() throws IOException {
        AppPaths.ensureDirectoriesExist();
        assertTrue(Files.exists(AppPaths.getDataDir()), "Data dir should exist");
        assertTrue(Files.exists(AppPaths.getSnapshotsDir()), "Snapshots dir should exist");
        assertTrue(Files.exists(AppPaths.getLogsDir()), "Logs dir should exist");
        assertNotNull(AppPaths.getDatabaseFile());
        assertNotNull(AppPaths.getConfigFile());
    }

    @Test
    void testAppLogging() {
        AppLogging.configure(Level.INFO);
        Logger logger = AppLogging.getLogger(AppPathsTest.class);
        logger.info("Test logging message for AppPathsTest");
        assertFalse(AppLogging.getInMemoryLogHandler().getRecentLogs().isEmpty());
    }
}
