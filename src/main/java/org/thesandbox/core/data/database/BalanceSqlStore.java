package org.thesandbox.core.data.database;

import org.bukkit.plugin.java.JavaPlugin;

import javax.sql.DataSource;

/**
 * Compatibility facade over the shared {@link Database} for coin balances.
 *
 * <p>The dedicated {@code mysql.balances} block is no longer used; all tables
 * live in the sandbox database so the server runs one connection pool.</p>
 */
public class BalanceSqlStore {

    private final Database database;

    public BalanceSqlStore(JavaPlugin plugin, Database database) {
        this.database = database;
    }

    /**
     * Kept for startup ordering; the shared pool is created by {@link Database}.
     */
    public void setup() {
        // no-op
    }

    /**
     * Kept for shutdown ordering; the shared pool is closed by {@link Database}.
     */
    public void close() {
        // no-op
    }

    public DataSource getDelegatingDataSource() {
        return database.getDelegatingDataSource();
    }
}
