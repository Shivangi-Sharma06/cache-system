package com.cse309.cache.persistence;

import com.cse309.cache.exception.CachePersistenceException;
import com.cse309.cache.history.OperationRecord;
import com.cse309.cache.logging.AppLogging;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * SQLite repository for storing and querying operation history records.
 * Uses batch execution and transactions (Unit 4).
 */
public final class OperationHistoryRepository implements Repository<OperationRecord, Long> {

    private static final Logger LOGGER = AppLogging.getLogger(OperationHistoryRepository.class);

    private final DatabaseManager databaseManager;

    public OperationHistoryRepository(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager cannot be null");
    }

    @Override
    public OperationRecord save(OperationRecord record) throws CachePersistenceException {
        if (record == null) return null;
        saveBatch(List.of(record));
        return record;
    }

    public void saveBatch(List<OperationRecord> records) throws CachePersistenceException {
        if (records == null || records.isEmpty()) return;

        String sql = """
            INSERT INTO operation_history (ts, thread_name, operation, cache_key, outcome, latency_nanos, details)
            VALUES (?, ?, ?, ?, ?, ?, ?);
            """;

        try (Connection conn = databaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (OperationRecord r : records) {
                    ps.setLong(1, r.getTimestamp());
                    ps.setString(2, r.getThreadName());
                    ps.setString(3, r.getOperation());
                    ps.setString(4, r.getCacheKey());
                    ps.setString(5, r.getOutcome());
                    ps.setLong(6, r.getLatencyNanos());
                    ps.setString(7, r.getDetails());
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException ex) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    LOGGER.warning("History batch rollback failed: " + rollbackEx.getMessage());
                }
                throw new CachePersistenceException("Failed to save history batch: " + ex.getMessage(), ex);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new CachePersistenceException("Connection failed while saving history batch: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<OperationRecord> findById(Long id) throws CachePersistenceException {
        if (id == null) return Optional.empty();
        String sql = "SELECT ts, thread_name, operation, cache_key, outcome, latency_nanos, details FROM operation_history WHERE id = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new OperationRecord(
                            rs.getLong("ts"),
                            rs.getString("thread_name"),
                            rs.getString("operation"),
                            rs.getString("cache_key"),
                            rs.getString("outcome"),
                            rs.getLong("latency_nanos"),
                            rs.getString("details")
                    ));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new CachePersistenceException("Failed to find operation record #" + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<OperationRecord> findAll() throws CachePersistenceException {
        return findRecent(500);
    }

    public List<OperationRecord> findRecent(int limit) throws CachePersistenceException {
        String sql = "SELECT ts, thread_name, operation, cache_key, outcome, latency_nanos, details FROM operation_history ORDER BY id DESC LIMIT ?;";
        List<OperationRecord> records = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit > 0 ? limit : 500);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(new OperationRecord(
                            rs.getLong("ts"),
                            rs.getString("thread_name"),
                            rs.getString("operation"),
                            rs.getString("cache_key"),
                            rs.getString("outcome"),
                            rs.getLong("latency_nanos"),
                            rs.getString("details")
                    ));
                }
            }
            return records;
        } catch (SQLException e) {
            throw new CachePersistenceException("Failed to query recent operation history: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(OperationRecord entity) {
        // Operation history is append-only audit trail
    }

    @Override
    public boolean deleteById(Long id) throws CachePersistenceException {
        if (id == null) return false;
        String sql = "DELETE FROM operation_history WHERE id = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new CachePersistenceException("Failed to delete operation record #" + id + ": " + e.getMessage(), e);
        }
    }

    public void clearAll() throws CachePersistenceException {
        String sql = "DELETE FROM operation_history;";
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            throw new CachePersistenceException("Failed to clear operation history table: " + e.getMessage(), e);
        }
    }
}
