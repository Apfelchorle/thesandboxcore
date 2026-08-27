package org.thesandbox.core.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class PlayerDataListener implements Listener {

    private final DataManager dataManager;
    private final Map<UUID, Map<String, Object>> playerCache = new HashMap<>();
    public PlayerDataListener(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    private Map<String, Object> loadFromDisk(UUID uuid) {
        Map<String, Object> data = new HashMap<>();

        data.put(PlayerDataKeys.COINS, dataManager.loadData(uuid, PlayerDataKeys.COINS, 0));
        data.put(PlayerDataKeys.JUMPPADS_MODE, dataManager.loadData(uuid, PlayerDataKeys.JUMPPADS_MODE, "disabled"));
        data.put(PlayerDataKeys.LIGHTNING_ROD, dataManager.loadData(uuid, PlayerDataKeys.LIGHTNING_ROD, "not_owned"));
        data.put(PlayerDataKeys.LOGIN_MESSAGE, dataManager.loadData(uuid, PlayerDataKeys.LOGIN_MESSAGE, ""));
        data.put(PlayerDataKeys.LOGIN_MESSAGES_STATE, dataManager.loadData(uuid, PlayerDataKeys.LOGIN_MESSAGES_STATE, "not_owned"));
        data.put(PlayerDataKeys.CLOWN_FISH, dataManager.loadData(uuid, PlayerDataKeys.CLOWN_FISH, "not_owned"));
//        data.put("coins", dataManager.loadData(uuid, "coins", 0));
//        data.put("jumppadmode", dataManager.loadData(uuid, "jumppadmode", "disabled"));
//        data.put("Lightning Rod", dataManager.loadData(uuid, "Lightning Rod", "no"));
//        data.put("LoginMessage", dataManager.loadData(uuid, "LoginMessage", "")); // login message (the text that will appear when a player joins DONT TOUCH NO TOUCHIE, GET UR GRUBBY HAND SOFF)
//        data.put("LoginMessages", dataManager.loadData(uuid, "LoginMessages", "not_owned")); // later this can be in 3 states Disabled, Enabled, not_owned
        return data;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        playerCache.put(uuid, loadFromDisk(uuid));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        Map<String, Object> data = playerCache.remove(uuid);
        if (data == null) return;

        data.forEach((key, value) -> dataManager.saveData(uuid, key, value));
    }

    public void loadAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (!playerCache.containsKey(uuid)) {
                playerCache.put(uuid, loadFromDisk(uuid));
            }
        }
    }

    public void saveAll() {
        for (UUID uuid : new HashSet<>(playerCache.keySet())) {
            Map<String, Object> data = playerCache.get(uuid);
            if (data != null) {
                data.forEach((key, value) -> dataManager.saveData(uuid, key, value));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T get(UUID uuid, String key, T defaultValue) {
        Map<String, Object> data = playerCache.get(uuid);
        if (data == null || !data.containsKey(key)) return defaultValue;
        try {
            return (T) data.get(key);
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    public void set(UUID uuid, String key, Object value) {
        playerCache.computeIfAbsent(uuid, k -> new HashMap<>()).put(key, value);
    }

    public int getCoins(UUID uuid) {
        return get(uuid, "coins", 0);
    }

    public void addCoins(UUID uuid, int amount) {
        set(uuid, "coins", getCoins(uuid) + amount);
    }

    public void removeCoins(UUID uuid, int amount) {
        set(uuid, "coins", getCoins(uuid) - amount);
    }

    public void setCoins(UUID uuid, int amount) {
        set(uuid, "coins", amount);
    }
}