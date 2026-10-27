package org.thesandbox.core.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.thesandbox.core.TheSandboxCore;

public class PlayerDataListener implements Listener {

    private final TheSandboxCore plugin;
    private final DataManager dataManager;
    private final Map<UUID, Map<String, Object>> playerCache =
        new ConcurrentHashMap<>();

    public PlayerDataListener(TheSandboxCore plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    private static final Map<String, Object> DEFAULTS = new LinkedHashMap<>();

    static {
        DEFAULTS.put(PlayerDataKeys.FIRST_TIME_JOINING, true);
        DEFAULTS.put(PlayerDataKeys.COINS, 0);
        DEFAULTS.put(PlayerDataKeys.JUMPPADS_MODE, "disabled");
        DEFAULTS.put(PlayerDataKeys.LIGHTNING_ROD, "not_owned");
        DEFAULTS.put(PlayerDataKeys.LOGIN_MESSAGE, "");
        DEFAULTS.put(PlayerDataKeys.LOGIN_MESSAGES_STATE, "not_owned");
        DEFAULTS.put(PlayerDataKeys.CLOWN_FISH, "not_owned");
        DEFAULTS.put(PlayerDataKeys.RIDEABLE_ENDER_PEARL, "not_owned");
        DEFAULTS.put(PlayerDataKeys.GRAPPLING_HOOK, "not_owned");
        DEFAULTS.put(PlayerDataKeys.Stacking_Potato, "not_owned");
        DEFAULTS.put(PlayerDataKeys.WIND_ROD, "not_owned");
        DEFAULTS.put(PlayerDataKeys.FLOAT_BOAT, "not_owned");
        DEFAULTS.put(PlayerDataKeys.MARRIAGE_STATUS, "Single");
        DEFAULTS.put(PlayerDataKeys.MARRIAGE_SPOUSE, "");
        DEFAULTS.put(PlayerDataKeys.GENDER, "male");
        DEFAULTS.put(PlayerDataKeys.VANISHED, false);
        DEFAULTS.put(PlayerDataKeys.CHATFILTER, true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncLogin(AsyncPlayerPreLoginEvent event) {
        if (
            event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED
        ) return;

        UUID uuid = event.getUniqueId();
        Map<String, Object> data = dataManager.loadAllData(uuid, DEFAULTS);
        playerCache.put(uuid, new ConcurrentHashMap<>(data));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        Map<String, Object> data = playerCache.remove(uuid);
        if (data == null) return;

        Map<String, Object> snapshot = new HashMap<>(data);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            dataManager.saveAllData(uuid, snapshot);
        });
    }

    public void saveAll() {
        for (UUID uuid : new HashSet<>(playerCache.keySet())) {
            Map<String, Object> data = playerCache.get(uuid);
            if (data != null) {
                dataManager.saveAllData(uuid, new HashMap<>(data));
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
        playerCache
            .computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
            .put(key, value);
    }


    public int getCoins(UUID uuid) {
        return get(uuid, PlayerDataKeys.COINS, 0);
    }
    public void setCoins(UUID uuid, int coins) {
        set(uuid, PlayerDataKeys.COINS, coins);
    }
    public void addCoins(UUID uuid, int amount) {
        set(uuid, PlayerDataKeys.COINS, getCoins(uuid) + amount);
    }
    public void removeCoins(UUID uuid, int amount) {
        set(uuid, PlayerDataKeys.COINS, getCoins(uuid) - amount);
    }

    public String getMarriageStatus(UUID uuid) {
        return get(uuid, PlayerDataKeys.MARRIAGE_STATUS, "divorced");
    }
    public String getMarriageSpouse(UUID uuid) {
        return get(uuid, PlayerDataKeys.MARRIAGE_SPOUSE, "");
    }
    public void Marry(Player p1, Player p2) {
        UUID uuid = p1.getUniqueId();
        UUID uuid2 = p2.getUniqueId();
        set(uuid, PlayerDataKeys.MARRIAGE_STATUS, "married");
        set(uuid, PlayerDataKeys.MARRIAGE_SPOUSE, p2.getName());

        set(uuid2, PlayerDataKeys.MARRIAGE_STATUS, "married");
        set(uuid2, PlayerDataKeys.MARRIAGE_SPOUSE, p1.getName());
    }

    public void Divorce(Player p1, Player p2) {
        UUID uuid = p1.getUniqueId();
        UUID uuid2 = p2.getUniqueId();
        set(uuid, PlayerDataKeys.MARRIAGE_STATUS, "divorced");
        set(uuid, PlayerDataKeys.MARRIAGE_SPOUSE, "");
        set(uuid2, PlayerDataKeys.MARRIAGE_SPOUSE, "");
        set(uuid2, PlayerDataKeys.MARRIAGE_STATUS, "divorced");
    }

    public void OfflineDivorce(Player p1, OfflinePlayer p2) {
        UUID uuid = p1.getUniqueId();
        UUID uuid2 = p2.getUniqueId();
        set(uuid, PlayerDataKeys.MARRIAGE_STATUS, "divorced");
        set(uuid, PlayerDataKeys.MARRIAGE_SPOUSE, "");
        set(uuid2, PlayerDataKeys.MARRIAGE_SPOUSE, "");
        set(uuid2, PlayerDataKeys.MARRIAGE_STATUS, "divorced");
    }
    public void setGender(UUID uuid, String gender ) {
        set(uuid, PlayerDataKeys.GENDER, gender);
    }
    public String getGender(UUID uuid) {
        return get(uuid, PlayerDataKeys.GENDER, "male");
    }

}
