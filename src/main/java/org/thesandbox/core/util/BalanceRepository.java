package org.thesandbox.core.util;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * SQL persistence for player coin balances.
 *
 * Table (created in the database configured under {@code mysql.balances}, falling
 * back to {@code mysql.sandbox}):
 *   player_balances (
 *     uuid CHAR(36) NOT NULL PRIMARY KEY,
 *     coins INT NOT NULL DEFAULT 0,
 *     updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 *     KEY idx_player_balances_coins (coins)
 *   )
 */
public class BalanceRepository {

    private final DataSource dataSource;

    public BalanceRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void ensureSchema() throws SQLException {
        final String sql = """
                CREATE TABLE IF NOT EXISTS player_balances (
                  uuid CHAR(36) NOT NULL PRIMARY KEY,
                  coins INT NOT NULL DEFAULT 0,
                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  KEY idx_player_balances_coins (coins)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    public long count() throws SQLException {
        final String sql = "SELECT COUNT(*) FROM player_balances";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getLong(1) : 0L;
        }
    }

    /** Returns null when the player has no balance row yet. */
    public Integer getCoins(UUID uuid) throws SQLException {
        final String sql = "SELECT coins FROM player_balances WHERE uuid = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : null;
            }
        }
    }

    public Map<UUID, Integer> loadAll() throws SQLException {
        final String sql = "SELECT uuid, coins FROM player_balances";
        Map<UUID, Integer> balances = new HashMap<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                try {
                    balances.put(UUID.fromString(resultSet.getString(1)), resultSet.getInt(2));
                } catch (IllegalArgumentException ignored) {
                    // Skip malformed UUIDs instead of failing the whole leaderboard.
                }
            }
        }
        return balances;
    }

    /**
     * The highest balances, largest first, read with a SQL {@code ORDER BY ... LIMIT}
     * instead of pulling the whole table into memory.
     */
    public Map<UUID, Integer> top(int limit) throws SQLException {
        final String sql = "SELECT uuid, coins FROM player_balances WHERE coins > 0 ORDER BY coins DESC LIMIT ?";
        Map<UUID, Integer> balances = new LinkedHashMap<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, Math.max(1, limit));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    try {
                        balances.put(UUID.fromString(resultSet.getString(1)), resultSet.getInt(2));
                    } catch (IllegalArgumentException ignored) {
                        // Skip malformed UUIDs instead of failing the whole leaderboard.
                    }
                }
            }
        }
        return balances;
    }

    public void upsert(UUID uuid, int coins) throws SQLException {
        final String sql = """
                INSERT INTO player_balances (uuid, coins, updated_at)
                VALUES (?, ?, NOW())
                ON DUPLICATE KEY UPDATE coins = VALUES(coins), updated_at = NOW()
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setInt(2, Math.max(0, coins));
            statement.executeUpdate();
        }
    }

    /**
     * Imports a legacy YAML balance only if the player has no SQL row yet.
     * Returns true when a row was inserted.
     */
    public boolean importIfMissing(UUID uuid, int coins) throws SQLException {
        final String sql = "INSERT IGNORE INTO player_balances (uuid, coins) VALUES (?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setInt(2, Math.max(0, coins));
            return statement.executeUpdate() > 0;
        }
    }
}
