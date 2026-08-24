package org.thesandbox.core.util;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataListener implements Listener {

    private final DataManager dataManager;
    // Memory cache: Keeps track of online players' coins
    private final Map<UUID, Integer> coinCache = new HashMap<>();

    public PlayerDataListener(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // 1. Load from file
        int coins = dataManager.loadData(uuid, "coins");

        // 2. Put into memory cache
        coinCache.put(uuid, coins);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // 1. Get from memory cache
        Integer coins = coinCache.get(uuid);

        if (coins != null) {
            // 2. Save to file
            dataManager.saveData(uuid,"coins" , coins);
            // 3. Clean memory to prevent memory leaks
            coinCache.remove(uuid);
        }
    }

    // Helper methods to modify coins from other files/commands
    public int getCoins(UUID uuid) {
        return coinCache.getOrDefault(uuid, 0);
    }

    public void addCoins(UUID uuid, int amount) {
        int current = getCoins(uuid);
        coinCache.put(uuid, current + amount);
    }

    public void removeCoins(UUID uuid, int amount) {
        int current = getCoins(uuid);
        coinCache.put(uuid, current - amount);
    }

    public void setCoins(UUID uuid, int amount) {
        coinCache.put(uuid, amount);
    }
}

