package org.thesandbox.core.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * Owns the MySQL connection pool used for coin balances.
 *
 * Reads the dedicated {@code mysql.balances} block and falls back to the shared
 * {@code mysql.sandbox} block when the balances database is not configured, so a
 * server can keep everything in one database if it wants to.
 */
public class BalanceSqlStore {

    private final JavaPlugin plugin;
    private volatile HikariDataSource dataSource;

    public BalanceSqlStore(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Returns the balances value, falling back to the sandbox value when unset. */
    private String setting(String key) {
        String value = plugin.getConfig().getString("mysql.balances." + key);
        if (value == null || value.isBlank()) {
            value = plugin.getConfig().getString("mysql.sandbox." + key);
        }
        return value;
    }

    private int port() {
        int port = plugin.getConfig().getInt("mysql.balances.port", 0);
        return port > 0 ? port : plugin.getConfig().getInt("mysql.sandbox.port", 3306);
    }

    public void setup() {
        close();

        String host = setting("host");
        String database = setting("database");
        if (host == null || host.isBlank() || database == null || database.isBlank()) {
            plugin.getLogger().warning("[Balances] No MySQL configuration found - coin balances will use YAML only.");
            return;
        }
        int port = port();
        String user = setting("user");
        String password = setting("password");

        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true";
        HikariConfig config = new HikariConfig();
        config.setPoolName("TheSandboxCore-Balances");
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setIdleTimeout(60000);
        config.setMaxLifetime(1800000);
        config.setConnectionTimeout(10000);

        // A database outage must not stop the plugin from enabling (Hikari throws on a failed first connection).
        try {
            dataSource = new HikariDataSource(config);
        } catch (Exception e) {
            dataSource = null;
            plugin.getLogger().severe("[Balances] Could not connect to the balances database: " + e.getMessage());
        }
    }

    public void close() {
        HikariDataSource ds = dataSource;
        dataSource = null;
        if (ds != null && !ds.isClosed()) ds.close();
    }

    /**
     * A DataSource that always forwards to the CURRENT pool, so a /thesandboxcore reload
     * (which recreates the pool) does not leave the BalanceManager pointing at a closed pool.
     */
    public DataSource getDelegatingDataSource() {
        return delegating;
    }

    private final DataSource delegating = new DataSource() {
        private DataSource current() throws SQLException {
            HikariDataSource ds = dataSource;
            if (ds == null || ds.isClosed()) throw new SQLException("Balances database is not available");
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
