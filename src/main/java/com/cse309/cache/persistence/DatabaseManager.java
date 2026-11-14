package com.cse309.cache.persistence;

import com.cse309.cache.exception.CachePersistenceException;
import com.cse309.cache.logging.AppLogging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * SQLite database connection and schema manager.
 * Enforces PRAGMA foreign_keys=ON, WAL journal mode, and busy timeouts (Unit 4).
 */
public final class DatabaseManager {

    private static final Logger LOGGER = AppLogging.getLogger(DatabaseManager.class);

    private final Path databasePath;
    private final String jdbcUrl;

    public DatabaseManager(Path databasePath) {
        this.databasePath = Objects.requireNonNull(databasePath, "databasePath cannot be null");
        this.jdbcUrl = "jdbc:sqlite:" + databasePath.toAbsolutePath();
    }

    public Connection getConnection() throws SQLException {
        try {
            Path parent = databasePath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new SQLException("Failed to create database directory: " + e.getMessage(), e);
        }

        Connection conn = DriverManager.getConnection(jdbcUrl);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute("PRAGMA journal_mode = WAL;");
            stmt.execute("PRAGMA busy_timeout = 5000;");
        }
        return conn;
    }

    public void initSchema() throws CachePersistenceException {
        String schemaSql = """
            CREATE TABLE IF NOT EXISTS snapshots (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              name TEXT NOT NULL,
              created_at INTEGER NOT NULL,
              eviction_policy TEXT NOT NULL,
              capacity INTEGER NOT NULL,
              entry_count INTEGER NOT NULL);

            CREATE TABLE IF NOT EXISTS snapshot_entries (
              snapshot_id INTEGER NOT NULL REFERENCES snapshots(id) ON DELETE CASCADE,
              cache_key TEXT NOT NULL,
              cache_value TEXT NOT NULL,
              created_at INTEGER NOT NULL,
              last_access_at INTEGER NOT NULL,
              expires_at INTEGER,
              access_count INTEGER NOT NULL,
              insertion_seq INTEGER NOT NULL,
              PRIMARY KEY (snapshot_id, cache_key));

            CREATE TABLE IF NOT EXISTS operation_history (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              ts INTEGER NOT NULL,
              thread_name TEXT,
              operation TEXT NOT NULL,
              cache_key TEXT,
              outcome TEXT NOT NULL,
              latency_nanos INTEGER,
              details TEXT);
            """;

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(schemaSql);
            LOGGER.info("SQLite schema initialized successfully at " + databasePath);
        } catch (SQLException e) {
            throw new CachePersistenceException("Failed to initialize SQLite database schema: " + e.getMessage(), e);
        }
    }

    public Path getDatabasePath() {
        return databasePath;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }
}
