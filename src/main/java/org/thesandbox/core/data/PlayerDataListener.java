package org.thesandbox.core.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.util.Utils;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public class PlayerDataListener implements Listener {

    private static final Map<String, Object> DEFAULTS = new LinkedHashMap<>();

    static {
        DEFAULTS.put(PlayerDataKeys.FIRST_TIME_JOINING, true);
        DEFAULTS.put(PlayerDataKeys.COINS, 0);
        DEFAULTS.put(PlayerDataKeys.LAST_LOCATION, "");
        DEFAULTS.put(PlayerDataKeys.JUMPPADS_MODE, "disabled");
        DEFAULTS.put(PlayerDataKeys.LIGHTNING_ROD, "not_owned");
        DEFAULTS.put(PlayerDataKeys.LOGIN_MESSAGE, "");
        DEFAULTS.put(PlayerDataKeys.LOGIN_MESSAGES_STATE, "not_owned");
        DEFAULTS.put(PlayerDataKeys.CLOWN_FISH, "not_owned");
        DEFAULTS.put(PlayerDataKeys.RIDEABLE_ENDER_PEARL, "not_owned");
        DEFAULTS.put(PlayerDataKeys.GRAPPLING_HOOK, "not_owned");
        DEFAULTS.put(PlayerDataKeys.Stacking_Potato, "not_owned");
        DEFAULTS.put(PlayerDataKeys.Stacking_Potato_Mode, "stack");
        DEFAULTS.put(PlayerDataKeys.WIND_ROD, "not_owned");
        DEFAULTS.put(PlayerDataKeys.FLOAT_BOAT, "not_owned");
        DEFAULTS.put(PlayerDataKeys.MARRIAGE_STATUS, "Single");
        DEFAULTS.put(PlayerDataKeys.MARRIAGE_SPOUSE, "");
        DEFAULTS.put(PlayerDataKeys.GENDER, "male");
        DEFAULTS.put(PlayerDataKeys.VANISHED, false);
        DEFAULTS.put(PlayerDataKeys.CHATFILTER, true);
        DEFAULTS.put(PlayerDataKeys.LAST_IP, "");
        DEFAULTS.put(PlayerDataKeys.LAST_RANK, "");
    }

    private final TheSandboxCore plugin;
    private final DataManager dataManager;
    private final Map<UUID, Map<String, Object>> playerCache =
            new ConcurrentHashMap<>();
    /**
     * All player-data writes are serialized through this single worker. Full-file
     * snapshots must never race: the last submitted snapshot has to win.
     */
    private final ExecutorService saveExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Sandbox-PlayerData");
        thread.setDaemon(true);
        return thread;
    });
    private BukkitTask autoSaveTask;

    public PlayerDataListener(TheSandboxCore plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String ip = Utils.censorIp(getPlayerIp(player)); // fix horrible doxxing issue.
        set(uuid, PlayerDataKeys.LAST_IP, ip);
        // Save the player's current rank so offline lookups show the last-known rank
        String rank = getRankName(player);
        if (!rank.isEmpty()) {
            set(uuid, PlayerDataKeys.LAST_RANK, rank);
        }
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
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        Map<String, Object> data = playerCache.remove(uuid);
        if (data == null) return;

        // Remember the logout spot so /player tp can reach offline players.
        data.put(PlayerDataKeys.LAST_LOCATION, encodeLocation(player.getLocation()));
        queueSave(uuid, new HashMap<>(data));
    }

    private static String encodeLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return "";
        }
        String world = URLEncoder.encode(location.getWorld().getName(), StandardCharsets.UTF_8);
        return world
                + ";" + location.getX()
                + ";" + location.getY()
                + ";" + location.getZ()
                + ";" + location.getYaw()
                + ";" + location.getPitch();
    }

    /**
     * Legacy YAML balance; used only to migrate old data into SQL and as an outage fallback.
     */
    public int getCoins(UUID uuid) {
        return get(uuid, PlayerDataKeys.COINS, 0);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(UUID uuid, String key, T defaultValue) {
        Map<String, Object> data = ensureLoaded(uuid);
        if (!data.containsKey(key)) return defaultValue;
        try {
            return (T) data.get(key);
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    private Map<String, Object> ensureLoaded(UUID uuid) {
        return playerCache.computeIfAbsent(uuid, id ->
                new ConcurrentHashMap<>(dataManager.loadAllData(id, DEFAULTS))
        );
    }

    /**
     * Every player UUID we know about, including online players whose data has not been
     * flushed to disk yet. Safe to call from an async thread.
     */
    public Set<UUID> knownPlayerIds() {
        Set<UUID> ids = new HashSet<>(dataManager.listPlayerIds());
        ids.addAll(playerCache.keySet());
        return ids;
    }

    public String getMarriageStatus(UUID uuid) {
        return get(uuid, PlayerDataKeys.MARRIAGE_STATUS, "Single");
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

    public void set(UUID uuid, String key, Object value) {
        ensureLoaded(uuid).put(key, value);
        saveIfOffline(uuid);
    }

    public void saveIfOffline(UUID uuid) {
        if (uuid != null && Bukkit.getPlayer(uuid) == null) {
            Map<String, Object> data = playerCache.get(uuid);
            if (data != null) {
                queueSave(uuid, new HashMap<>(data));
            }
        }
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

    public void setGender(UUID uuid, String gender) {
        set(uuid, PlayerDataKeys.GENDER, gender);
    }

    public String getGender(UUID uuid) {
        return get(uuid, PlayerDataKeys.GENDER, "male");
    }

    /**
     * Starts the periodic safety-net save. Runs on the main thread so reading
     * online player locations is legal; the disk write is queued on the single
     * player-data worker.
     */
    public void startAutoSave() {
        if (autoSaveTask != null) return;

        long seconds = plugin.getConfig().getLong("player-data.save-interval-seconds", 300L);
        seconds = Math.max(30L, Math.min(3600L, seconds));
        long ticks = seconds * 20L;
        autoSaveTask = Bukkit.getScheduler().runTaskTimer(plugin, this::periodicSave, ticks, ticks);
    }

    public void stopAutoSave() {
        BukkitTask task = autoSaveTask;
        autoSaveTask = null;
        if (task != null) task.cancel();
    }

    private void periodicSave() {
        Map<UUID, Map<String, Object>> snapshots = snapshotAll();
        if (!snapshots.isEmpty()) {
            queueSnapshots(snapshots);
        }
    }

    /**
     * Main-thread snapshot of every cached player. The returned maps are copies,
     * so the async writer never observes live cache mutations.
     */
    private Map<UUID, Map<String, Object>> snapshotAll() {
        Map<UUID, Map<String, Object>> snapshots = new HashMap<>();
        for (Map.Entry<UUID, Map<String, Object>> entry : playerCache.entrySet()) {
            Map<String, Object> data = new HashMap<>(entry.getValue());
            Player online = Bukkit.getPlayer(entry.getKey());
            if (online != null) {
                data.put(PlayerDataKeys.LAST_LOCATION, encodeLocation(online.getLocation()));
            }
            snapshots.put(entry.getKey(), data);
        }
        return snapshots;
    }

    private void queueSave(UUID uuid, Map<String, Object> snapshot) {
        try {
            saveExecutor.execute(() -> dataManager.saveAllData(uuid, snapshot));
        } catch (RejectedExecutionException rejected) {
            // Plugin is shutting down; write synchronously rather than lose the save.
            dataManager.saveAllData(uuid, snapshot);
        }
    }

    private void queueSnapshots(Map<UUID, Map<String, Object>> snapshots) {
        try {
            saveExecutor.execute(() -> snapshots.forEach(dataManager::saveAllData));
        } catch (RejectedExecutionException rejected) {
            snapshots.forEach(dataManager::saveAllData);
        }
    }

    /**
     * Flushes every known player synchronously (from the caller's perspective)
     * and stops the writer. Called during plugin shutdown.
     */
    public void saveAll() {
        stopAutoSave();

        Map<UUID, Map<String, Object>> snapshots = snapshotAll();
        if (snapshots.isEmpty()) {
            saveExecutor.shutdown();
            return;
        }

        if (saveExecutor.isShutdown()) {
            snapshots.forEach(dataManager::saveAllData);
            return;
        }

        try {
            saveExecutor.execute(() -> snapshots.forEach(dataManager::saveAllData));
        } catch (RejectedExecutionException rejected) {
            snapshots.forEach(dataManager::saveAllData);
        }
        saveExecutor.shutdown();

        try {
            if (!saveExecutor.awaitTermination(15, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Player-data writer did not finish within 15 seconds.");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            plugin.getLogger().warning("Interrupted while waiting for player-data save.");
        }
    }

    /**
     * Last known location, captured when the player quits. Returns null when the
     * player has no stored location yet or the stored world is not loaded.
     */
    public Location getLastLocation(UUID uuid) {
        return decodeLocation(get(uuid, PlayerDataKeys.LAST_LOCATION, ""));
    }

    private static Location decodeLocation(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }

        String[] parts = encoded.split(";", 6);
        if (parts.length != 6) {
            return null;
        }

        final String worldName;
        try {
            worldName = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }

        try {
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = Float.parseFloat(parts[4]);
            float pitch = Float.parseFloat(parts[5]);
            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * Extracts the IP address string from a player's socket connection.
     */
    private static String getPlayerIp(Player player) {
        try {
            java.net.InetSocketAddress socketAddress = player.getAddress();
            if (socketAddress == null) return null;
            java.net.InetAddress address = socketAddress.getAddress();
            return address != null ? address.getHostAddress() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets the rank name string for a player based on their permissions.
     */
    private static String getRankName(Player player) {
        if (player == null) return "";
        if (player.hasPermission("sandbox.operator")) return "OPERATOR";
        if (player.hasPermission("sandbox.admin")) return "ADMIN";
        if (player.hasPermission("sandbox.staff")) return "STAFF";
        if (player.hasPermission("sandbox.mb")) return "MB";
        if (player.hasPermission("sandbox.vip")) return "VIP";
        return "DEFAULT";
    }

    /**
     * Returns the last known IP address for a player.
     * For online players, returns the current connection IP.
     * For offline players, returns the last saved IP from data store.
     */
    public String getLastIp(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return getPlayerIp(online);
        }
        return get(uuid, PlayerDataKeys.LAST_IP, "");
    }

    /**
     * Returns the last known rank name for a player.
     * For online players, returns the current rank name.
     * For offline players, returns the last saved rank from data store.
     */
    public String getLastRank(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return getRankName(online);
        }
        return get(uuid, PlayerDataKeys.LAST_RANK, "");
    }

}
