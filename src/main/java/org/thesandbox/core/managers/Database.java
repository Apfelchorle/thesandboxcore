package org.thesandbox.core.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Owns the single MySQL connection pool used by every persistence feature.
 *
 * <p>Before this class existed, balances, guilds, tags and command-spy each
 * created their own pool. On a busy server that meant four independent pools,
 * four sets of credentials and four failure modes. Consumers now receive the
 * delegating {@link #getDelegatingDataSource()} view, which keeps working after
 * a config reload swaps the underlying pool.</p>
 */
public final class Database {

    private final JavaPlugin plugin;
    private final DataSource delegating = new DataSource() {
        @Override
        public Connection getConnection() throws SQLException {
            return current().getConnection();
        }

        private DataSource current() throws SQLException {
            HikariDataSource ds = dataSource;
            if (ds == null || ds.isClosed()) throw new SQLException("Database is not available");
            return ds;
        }

        @Override
        public Connection getConnection(String u, String p) throws SQLException {
            return current().getConnection(u, p);
        }

        @Override
        public PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) {
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return current().unwrap(iface);
        }        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return current().isWrapperFor(iface);
        }




    };
    private volatile HikariDataSource dataSource;

    public Database(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        close();

        String host = plugin.getConfig().getString("mysql.sandbox.host");
        String database = plugin.getConfig().getString("mysql.sandbox.database");
        if (host == null || host.isBlank() || database == null || database.isBlank()) {
            plugin.getLogger().warning("[SQL] mysql.sandbox.host / mysql.sandbox.database not configured - database features are disabled.");
            return;
        }

        int port = plugin.getConfig().getInt("mysql.sandbox.port", 3306);
        String user = plugin.getConfig().getString("mysql.sandbox.user");
        String password = plugin.getConfig().getString("mysql.sandbox.password");

        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&autoReconnect=true&useUnicode=true&characterEncoding=utf8&allowPublicKeyRetrieval=true";

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

        // A database outage must not stop the plugin from enabling (Hikari throws on a failed first connection).
        try {
            dataSource = new HikariDataSource(config);
        } catch (Exception e) {
            dataSource = null;
            plugin.getLogger().log(Level.SEVERE, "[SQL] Could not connect to the database: " + e.getMessage());
        }
    }

    public void close() {
        HikariDataSource ds = dataSource;
        dataSource = null;
        if (ds != null && !ds.isClosed()) ds.close();
    }

    /**
     * The live pool, or null when the database is unavailable.
     */
    public HikariDataSource getDataSource() {
        return dataSource;
    }

    /**
     * A DataSource that always forwards to the current pool, even after a reload.
     */
    public DataSource getDelegatingDataSource() {
        return delegating;
    }
}
