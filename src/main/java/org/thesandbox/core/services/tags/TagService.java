package org.thesandbox.core.services.tags;

import org.bukkit.plugin.java.JavaPlugin;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores one saved tag per username (cracked server friendly).
 * Table schema:
 * CREATE TABLE IF NOT EXISTS sandbox_tags (
 * username VARCHAR(32) PRIMARY KEY,
 * tag TEXT NOT NULL,
 * updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
 * );
 */
public class TagService {
    private final JavaPlugin plugin;
    private final DataSource dataSource;

    public TagService(JavaPlugin plugin, DataSource dataSource) {
        this.plugin = plugin;
        this.dataSource = dataSource;
    }

    // ---- lifecycle ----
    public void init() {
        if (dataSource == null) return;
        try {
            ensureTable();
        } catch (Throwable t) {
            plugin.getLogger().warning("[TagService] init failed: " + t.getMessage());
        }
    }

    private void ensureTable() throws SQLException {
        try (Connection c = dataSource.getConnection();
             Statement st = c.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS sandbox_tags (" +
                    "username VARCHAR(32) PRIMARY KEY," +
                    "tag TEXT NOT NULL," +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)");
        }
    }

    public void shutdown() {
        // The shared Database owns the pool.
    }

    // ---- CRUD ----

    /**
     * Returns the saved tag for this exact username (or null).
     */
    public String getSavedTag(String username) {
        if (username == null || dataSource == null) return null;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT tag FROM sandbox_tags WHERE LOWER(username)=LOWER(?)")) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[TagService] getSavedTag error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Upsert saved tag for username.
     */
    public void saveTag(String username, String tag) {
        if (username == null || dataSource == null) return;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO sandbox_tags(username, tag) VALUES (?, ?) " +
                             "ON DUPLICATE KEY UPDATE tag=VALUES(tag), updated_at=CURRENT_TIMESTAMP")) {
            ps.setString(1, username);
            ps.setString(2, tag);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("[TagService] saveTag error: " + e.getMessage());
        }
    }

    /**
     * Remove saved tag for username.
     */
    public void deleteTag(String username) {
        if (username == null || dataSource == null) return;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM sandbox_tags WHERE LOWER(username)=LOWER(?)")) {
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("[TagService] deleteTag error: " + e.getMessage());
        }
    }

    /**
     * Delete all saved tags.
     */
    public int clearAll() {
        if (dataSource == null) return 0;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM sandbox_tags")) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("[TagService] clearAll error: " + e.getMessage());
            return 0;
        }
    }

    /**
     * For moderators: list up to 'limit' rows ordered by newest first.
     */
    public Map<String, String> listAll(int limit) {
        Map<String, String> out = new LinkedHashMap<>();
        if (dataSource == null) return out;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT username, tag FROM sandbox_tags ORDER BY updated_at DESC LIMIT ?")) {
            ps.setInt(1, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.put(rs.getString(1), rs.getString(2));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[TagService] listAll error: " + e.getMessage());
        }
        return out;
    }

    /**
     * For clearall: list only usernames.
     */
    public List<String> listUsernames() {
        List<String> out = new ArrayList<>();
        if (dataSource == null) return out;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT username FROM sandbox_tags")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rs.getString(1));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[TagService] listUsernames error: " + e.getMessage());
        }
        return out;
    }
}
