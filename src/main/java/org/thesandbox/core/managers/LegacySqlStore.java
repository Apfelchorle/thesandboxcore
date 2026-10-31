package org.thesandbox.core.managers;

import org.bukkit.plugin.java.JavaPlugin;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

/**
 * Ensures the legacy command-spy / staff-chat tables exist inside the shared
 * {@link Database}. The pool itself lives in {@link Database}.
 */
public class LegacySqlStore {

    private final JavaPlugin plugin;
    private final Database database;

    public LegacySqlStore(JavaPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    /**
     * Called on startup and after /thesandboxcore reload; the pool is managed by {@link Database}.
     */
    public void setup() {
        DataSource ds = database.getDataSource();
        if (ds == null) return;

        try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS commandspy (" +
                    "uuid VARCHAR(36) PRIMARY KEY," +
                    "enabled BOOLEAN NOT NULL)");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS staffchat (" +
                    "uuid VARCHAR(36) PRIMARY KEY," +
                    "enabled BOOLEAN NOT NULL)");

            try {
                stmt.executeUpdate("ALTER TABLE staffchat ADD COLUMN IF NOT EXISTS hidden BOOLEAN NOT NULL DEFAULT FALSE");
            } catch (SQLException e) {
                try {
                    stmt.executeUpdate("ALTER TABLE staffchat ADD COLUMN hidden BOOLEAN NOT NULL DEFAULT FALSE");
                } catch (SQLException ignore) {
                    // Column already exists on servers without "ADD COLUMN IF NOT EXISTS" support.
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[SQL] Could not initialize database tables", e);
        }
    }

    /** Kept for shutdown ordering; the shared pool is closed by {@link Database}. */
    public void close() {
        // no-op
    }

    /**
     * The live pool, or null when the database is unavailable.
     */
    public DataSource getDataSource() {
        return database.getDataSource();
    }

    /** A DataSource that always forwards to the current shared pool. */
    public DataSource getDelegatingDataSource() {
        return database.getDelegatingDataSource();
    }
}
