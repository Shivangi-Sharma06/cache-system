package com.cse309.cache.persistence;

import com.cse309.cache.core.CacheDefaults;
import com.cse309.cache.core.CacheEntrySnapshot;
import com.cse309.cache.exception.CachePersistenceException;
import com.cse309.cache.logging.AppLogging;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * SQLite repository for saving and restoring cache snapshots and their entry records.
 * Uses transactional batch processing, PreparedStatement, and rollback on failure (Unit 4).
 */
public final class SnapshotRepository implements Repository<DbSnapshot, Long> {

    private static final Logger LOGGER = AppLogging.getLogger(SnapshotRepository.class);
    private static final int MAX_SNAPSHOTS_RETAINED = 10;

    private final DatabaseManager databaseManager;

    public SnapshotRepository(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager cannot be null");
    }

    @Override
    public DbSnapshot save(DbSnapshot snapshot) throws CachePersistenceException {
        Objects.requireNonNull(snapshot, "snapshot cannot be null");

        String insertSnapshotSql = """
            INSERT INTO snapshots (name, created_at, eviction_policy, capacity, entry_count)
            VALUES (?, ?, ?, ?, ?);
            """;

        String insertEntrySql = """
            INSERT INTO snapshot_entries (snapshot_id, cache_key, cache_value, created_at, last_access_at, expires_at, access_count, insertion_seq)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?);
            """;

        String pruneSql = """
            DELETE FROM snapshots
            WHERE id NOT IN (SELECT id FROM snapshots ORDER BY created_at DESC LIMIT ?);
            """;

        try (Connection conn = databaseManager.getConnection()) {
            conn.setAutoCommit(false);
            long snapshotId;

            try {
                // 1. Insert snapshot metadata
                try (PreparedStatement ps = conn.prepareStatement(insertSnapshotSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, snapshot.getName());
                    ps.setLong(2, snapshot.getCreatedAt());
                    ps.setString(3, snapshot.getEvictionPolicy());
                    ps.setInt(4, snapshot.getCapacity());
                    ps.setInt(5, snapshot.getEntries().size());
                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            snapshotId = rs.getLong(1);
                        } else {
                            throw new SQLException("Failed to retrieve generated snapshot ID");
                        }
                    }
                }

                // 2. Batch insert entries
                if (!snapshot.getEntries().isEmpty()) {
                    try (PreparedStatement psEntries = conn.prepareStatement(insertEntrySql)) {
                        for (CacheEntrySnapshot<String, String> entry : snapshot.getEntries()) {
                            psEntries.setLong(1, snapshotId);
                            psEntries.setString(2, entry.getKey());
                            psEntries.setString(3, entry.getValue());
                            psEntries.setLong(4, entry.getCreatedAt());
                            psEntries.setLong(5, entry.getLastAccessAt());

                            if (entry.hasExpiry()) {
                                psEntries.setLong(6, entry.getExpiresAt());
                            } else {
                                psEntries.setNull(6, Types.BIGINT);
                            }

                            psEntries.setLong(7, entry.getAccessCount());
                            psEntries.setLong(8, entry.getInsertionSequence());
                            psEntries.addBatch();
                        }
                        psEntries.executeBatch();
                    }
                }

                // 3. Prune older snapshots
                try (PreparedStatement psPrune = conn.prepareStatement(pruneSql)) {
                    psPrune.setInt(1, MAX_SNAPSHOTS_RETAINED);
                    psPrune.executeUpdate();
                }

                conn.commit();
                LOGGER.info("Saved snapshot #" + snapshotId + " with " + snapshot.getEntries().size() + " entries");
                return new DbSnapshot(snapshotId, snapshot.getName(), snapshot.getCreatedAt(),
                        snapshot.getEvictionPolicy(), snapshot.getCapacity(), snapshot.getEntries().size(), snapshot.getEntries());

            } catch (SQLException ex) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    LOGGER.warning("Rollback failed: " + rollbackEx.getMessage());
                }
                throw new CachePersistenceException("Transaction failed while saving snapshot: " + ex.getMessage(), ex);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new CachePersistenceException("Failed to acquire connection for saving snapshot: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<DbSnapshot> findById(Long id) throws CachePersistenceException {
        if (id == null) return Optional.empty();

        String selectSql = "SELECT id, name, created_at, eviction_policy, capacity, entry_count FROM snapshots WHERE id = ?;";
        String selectEntriesSql = """
            SELECT cache_key, cache_value, created_at, last_access_at, expires_at, access_count, insertion_seq
            FROM snapshot_entries WHERE snapshot_id = ? ORDER BY insertion_seq ASC;
            """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                long snapId = rs.getLong("id");
                String name = rs.getString("name");
                long createdAt = rs.getLong("created_at");
                String policy = rs.getString("eviction_policy");
                int capacity = rs.getInt("capacity");
                int count = rs.getInt("entry_count");

                List<CacheEntrySnapshot<String, String>> entries = new ArrayList<>();
                try (PreparedStatement psEntries = conn.prepareStatement(selectEntriesSql)) {
                    psEntries.setLong(1, snapId);
                    try (ResultSet rsEntries = psEntries.executeQuery()) {
                        while (rsEntries.next()) {
                            long expires = rsEntries.getLong("expires_at");
                            if (rsEntries.wasNull()) {
                                expires = CacheDefaults.NO_EXPIRY_SENTINEL;
                            }
                            entries.add(new CacheEntrySnapshot<>(
                                    rsEntries.getString("cache_key"),
                                    rsEntries.getString("cache_value"),
                                    rsEntries.getLong("created_at"),
                                    rsEntries.getLong("last_access_at"),
                                    expires,
                                    rsEntries.getLong("access_count"),
                                    rsEntries.getLong("insertion_seq")
                            ));
                        }
                    }
                }
                return Optional.of(new DbSnapshot(snapId, name, createdAt, policy, capacity, count, entries));
            }
        } catch (SQLException e) {
            throw new CachePersistenceException("Failed to load snapshot #" + id + ": " + e.getMessage(), e);
        }
    }

    public Optional<DbSnapshot> findLatest() throws CachePersistenceException {
        String query = "SELECT id FROM snapshots ORDER BY created_at DESC LIMIT 1;";
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return findById(rs.getLong("id"));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new CachePersistenceException("Failed to find latest snapshot: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DbSnapshot> findAll() throws CachePersistenceException {
        String selectSql = "SELECT id, name, created_at, eviction_policy, capacity, entry_count FROM snapshots ORDER BY created_at DESC;";
        List<DbSnapshot> snapshots = new ArrayList<>();

        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSql)) {
            while (rs.next()) {
                snapshots.add(new DbSnapshot(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getLong("created_at"),
                        rs.getString("eviction_policy"),
                        rs.getInt("capacity"),
                        rs.getInt("entry_count"),
                        Collections.emptyList()
                ));
            }
            return snapshots;
        } catch (SQLException e) {
            throw new CachePersistenceException("Failed to load snapshots list: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(DbSnapshot snapshot) throws CachePersistenceException {
        if (snapshot == null || snapshot.getId() == null) return;
        String sql = "UPDATE snapshots SET name = ? WHERE id = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, snapshot.getName());
            ps.setLong(2, snapshot.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new CachePersistenceException("Failed to update snapshot: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(Long id) throws CachePersistenceException {
        if (id == null) return false;
        String sql = "DELETE FROM snapshots WHERE id = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new CachePersistenceException("Failed to delete snapshot #" + id + ": " + e.getMessage(), e);
        }
    }
}
