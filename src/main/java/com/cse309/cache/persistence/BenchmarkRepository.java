package com.cse309.cache.persistence;

import com.cse309.cache.benchmark.BenchmarkResult;
import com.cse309.cache.exception.CachePersistenceException;
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
 * SQLite repository for storing and querying benchmark runs.
 * Uses PreparedStatement and ResultSet (Unit 4).
 */
public final class BenchmarkRepository implements Repository<BenchmarkResult, Long> {

    private static final Logger LOGGER = AppLogging.getLogger(BenchmarkRepository.class);

    private final DatabaseManager databaseManager;

    public BenchmarkRepository(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager cannot be null");
    }

    @Override
    public BenchmarkResult save(BenchmarkResult result) throws CachePersistenceException {
        if (result == null) return null;

        String sql = """
            INSERT INTO benchmark_runs (ran_at, eviction_policy, capacity, threads, key_distribution, operation_mix, total_ops, duration_nanos, ops_per_second, hit_ratio, evictions)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
            """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, result.getRanAt());
            ps.setString(2, result.getEvictionPolicy());
            ps.setInt(3, result.getCapacity());
            ps.setInt(4, result.getThreads());
            ps.setString(5, result.getKeyDistribution());
            ps.setString(6, result.getOperationMix());
            ps.setInt(7, result.getTotalOps());
            ps.setLong(8, result.getDurationNanos());
            ps.setDouble(9, result.getOpsPerSecond());
            ps.setDouble(10, result.getHitRatio());
            ps.setInt(11, result.getEvictions());
            ps.executeUpdate();

            long id;
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    id = rs.getLong(1);
                } else {
                    throw new SQLException("Failed to retrieve generated benchmark ID");
                }
            }

            return new BenchmarkResult(
                    id, result.getRanAt(), result.getEvictionPolicy(), result.getCapacity(),
                    result.getThreads(), result.getKeyDistribution(), result.getOperationMix(),
                    result.getTotalOps(), result.getDurationNanos(), result.getOpsPerSecond(),
                    result.getHitRatio(), result.getEvictions(), result.getJdkVersion(),
                    result.getOsName(), result.getAvailableProcessors(), result.getRunMatrix()
            );
        } catch (SQLException e) {
            throw new CachePersistenceException("Failed to save benchmark result: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<BenchmarkResult> findById(Long id) throws CachePersistenceException {
        if (id == null) return Optional.empty();
        String sql = "SELECT * FROM benchmark_runs WHERE id = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new CachePersistenceException("Failed to find benchmark run #" + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<BenchmarkResult> findAll() throws CachePersistenceException {
        String sql = "SELECT * FROM benchmark_runs ORDER BY ran_at DESC;";
        List<BenchmarkResult> results = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                results.add(mapRow(rs));
            }
            return results;
        } catch (SQLException e) {
            throw new CachePersistenceException("Failed to query benchmark history: " + e.getMessage(), e);
        }
    }

    private BenchmarkResult mapRow(ResultSet rs) throws SQLException {
        return new BenchmarkResult(
                rs.getLong("id"),
                rs.getLong("ran_at"),
                rs.getString("eviction_policy"),
                rs.getInt("capacity"),
                rs.getInt("threads"),
                rs.getString("key_distribution"),
                rs.getString("operation_mix"),
                rs.getInt("total_ops"),
                rs.getLong("duration_nanos"),
                rs.getDouble("ops_per_second"),
                rs.getDouble("hit_ratio"),
                rs.getInt("evictions"),
                System.getProperty("java.version"),
                System.getProperty("os.name"),
                Runtime.getRuntime().availableProcessors(),
                new double[0][0]
        );
    }

    @Override
    public void update(BenchmarkResult entity) {
        // Benchmark results are immutable
    }

    @Override
    public boolean deleteById(Long id) throws CachePersistenceException {
        if (id == null) return false;
        String sql = "DELETE FROM benchmark_runs WHERE id = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new CachePersistenceException("Failed to delete benchmark run #" + id + ": " + e.getMessage(), e);
        }
    }
}
