package com.cse309.cache.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Centralized, immutable path provider for application directories and files.
 */
public final class AppPaths {

    private static final Path BASE_DIR = Paths.get("").toAbsolutePath().normalize();
    private static final Path DATA_DIR = BASE_DIR.resolve("data");
    private static final Path SNAPSHOTS_DIR = DATA_DIR.resolve("snapshots");
    private static final Path LOGS_DIR = BASE_DIR.resolve("logs");
    private static final Path DATABASE_FILE = DATA_DIR.resolve("cache.db");
    private static final Path CONFIG_FILE = DATA_DIR.resolve("config.properties");

    private AppPaths() {
        // Prevent instantiation
    }

    public static Path getBaseDir() {
        return BASE_DIR;
    }

    public static Path getDataDir() {
        return DATA_DIR;
    }

    public static Path getSnapshotsDir() {
        return SNAPSHOTS_DIR;
    }

    public static Path getLogsDir() {
        return LOGS_DIR;
    }

    public static Path getDatabaseFile() {
        return DATABASE_FILE;
    }

    public static Path getConfigFile() {
        return CONFIG_FILE;
    }

    /**
     * Ensures all essential directories exist on the filesystem.
     *
     * @throws IOException if directory creation fails
     */
    public static void ensureDirectoriesExist() throws IOException {
        if (!Files.exists(DATA_DIR)) {
            Files.createDirectories(DATA_DIR);
        }
        if (!Files.exists(SNAPSHOTS_DIR)) {
            Files.createDirectories(SNAPSHOTS_DIR);
        }
        if (!Files.exists(LOGS_DIR)) {
            Files.createDirectories(LOGS_DIR);
        }
    }
}
