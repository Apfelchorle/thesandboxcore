package org.thesandbox.core.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Owns the legacy MySQL connection pool used for commandspy/staffchat persistence.
 */
public class LegacySqlStore {

    private final JavaPlugin plugin;
    private volatile HikariDataSource dataSource;

    public LegacySqlStore(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        close();

        String host = plugin.getConfig().getString("mysql.sandbox.host");
        String database = plugin.getConfig().getString("mysql.sandbox.database");
        if (host == null || host.isBlank() || database == null || database.isBlank()) {
            plugin.getLogger().warning("[SQL] mysql.host / mysql.database not configured - database features are disabled.");
            return;
        }
        int port = plugin.getConfig().getInt("mysql.sandbox.port", 3306);
        String user = plugin.getConfig().getString("mysql.sandbox.user");
        String password = plugin.getConfig().getString("mysql.sandbox.password");

        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true";
        HikariConfig config = new HikariConfig();
        config.setPoolName("TheSandboxCore-SQL");
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(60000);
        config.setMaxLifetime(1800000);
        config.setConnectionTimeout(10000);

        // A database outage must not stop the whole plugin from enabling (Hikari throws on a failed first connection).
        try {
            dataSource = new HikariDataSource(config);
        } catch (Exception e) {
            dataSource = null;
            plugin.getLogger().severe("[SQL] Could not connect to the database: " + e.getMessage());
            return;
        }

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
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

    public void close() {
        HikariDataSource ds = dataSource;
        dataSource = null;
        if (ds != null && !ds.isClosed()) ds.close();
    }

    /** The live pool, or null when the database is unavailable. Prefer {@link #getDelegatingDataSource()} for long-lived holders. */
    public HikariDataSource getDataSource() {
        return dataSource;
    }

    /**
     * A DataSource that always forwards to the CURRENT pool.
     * Services that cached {@link #getDataSource()} kept pointing at a closed pool after /thesandboxcore reload
     * (setup() recreates the pool); this wrapper survives reloads and fails with an SQLException instead of an NPE
     * while the database is down.
     */
    public DataSource getDelegatingDataSource() {
        return delegating;
    }

    private final DataSource delegating = new DataSource() {
        private DataSource current() throws SQLException {
            HikariDataSource ds = dataSource;
            if (ds == null || ds.isClosed()) throw new SQLException("Database is not available");
            return ds;
        }

        @Override public Connection getConnection() throws SQLException { return current().getConnection(); }
        @Override public Connection getConnection(String u, String p) throws SQLException { return current().getConnection(u, p); }
        @Override public PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(PrintWriter out) { }
        @Override public void setLoginTimeout(int seconds) { }
        @Override public int getLoginTimeout() { return 0; }
        @Override public Logger getParentLogger() throws SQLFeatureNotSupportedException { throw new SQLFeatureNotSupportedException(); }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { return current().unwrap(iface); }
        @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return current().isWrapperFor(iface); }
    };
}
