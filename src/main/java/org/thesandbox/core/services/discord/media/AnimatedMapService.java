package org.thesandbox.core.services.discord.media;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Manages the lifecycle of animated GIF maps.
 *
 * <p>Creates virtual MapView instances from GIF bytes, gives them to players
 * as filled map items, and cleans up when players disconnect or the plugin stops.</p>
 *
 * <p>Each animated map is tracked by its MapView ID. When a player quits, any
 * maps they hold are removed from the active set and their renderers are disposed.</p>
 */
public class AnimatedMapService implements AutoCloseable {

    private static final int MAX_CONCURRENT_MAPS = 64;
    private static final int MAX_FRAMES_PER_GIF = 999999999;
    private static final int MAX_GIF_BYTES = 8 * 1024 * 1024; // 8 MB

    private final Plugin plugin;
    private final Map<Integer, AnimatedMapEntry> activeMaps = new ConcurrentHashMap<>();
    private final ThreadPoolExecutor executor;
    private final AtomicBoolean closed = new AtomicBoolean();

    public AnimatedMapService(Plugin plugin) {
        this.plugin = plugin;
        this.executor = new ThreadPoolExecutor(
                1, 2,
                30L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(16),
                runnable -> {
                    Thread thread = new Thread(runnable, "Sandbox-GifMap");
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.AbortPolicy()
        );

        // Register for player quit events to clean up maps
        Bukkit.getPluginManager().registerEvents(
                new org.bukkit.event.Listener() {
                    @org.bukkit.event.EventHandler
                    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
                        cleanupPlayerMaps(event.getPlayer());
                    }
                },
                plugin
        );

        // Periodic cleanup of abandoned maps (no holders for 10+ minutes)
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::evictAbandoned, 600L, 600L);
    }

    /**
     * Creates an animated map from GIF bytes and gives it to the player.
     * This is an async operation; the item is given when ready.
     *
     * @param player   the player to give the map to
     * @param gifBytes the raw GIF data
     * @return a future that completes when the map is given (or fails)
     */
    public CompletableFuture<ItemStack> createAndGiveMap(Player player, byte[] gifBytes) {
        if (closed.get() || player == null || !player.isOnline()) {
            return CompletableFuture.completedFuture(null);
        }

        if (gifBytes == null || gifBytes.length == 0) {
            return CompletableFuture.completedFuture(null);
        }

        if (gifBytes.length > MAX_GIF_BYTES) {
            player.sendMessage(Component.text(
                    "[GifMap] GIF is too large (max 8MB).", NamedTextColor.RED));
            return CompletableFuture.completedFuture(null);
        }

        // Check concurrent map limit
        if (activeMaps.size() >= MAX_CONCURRENT_MAPS) {
            player.sendMessage(Component.text(
                    "[GifMap] Too many active animated maps. Please wait.", NamedTextColor.RED));
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<ItemStack> future = new CompletableFuture<>();

        try {
            executor.submit(() -> {
                try {
                    // Decode the GIF (off the main thread)
                    GifFrameDecoder.Result decoded = GifFrameDecoder.decode(gifBytes);
                    List<GifFrameDecoder.Frame> frames = decoded.frames();
                    if (frames.isEmpty()) {
                        debug("No frames decoded from GIF");
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (player.isOnline()) {
                                player.sendMessage(Component.text(
                                        "[GifMap] Failed to decode GIF.", NamedTextColor.RED));
                            }
                        });
                        future.complete(null);
                        return;
                    }
                    if (frames.size() > MAX_FRAMES_PER_GIF) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (player.isOnline()) {
                                player.sendMessage(Component.text(
                                        "[GifMap] GIF has too many frames (max " + MAX_FRAMES_PER_GIF + ").", NamedTextColor.RED));
                            }
                        });
                        future.complete(null);
                        return;
                    }

                    // Create the map on the main thread (Bukkit API requirement)
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            ItemStack mapItem = createMapFromFrames(player, frames);
                            if (mapItem != null) {
                                if (player.isOnline()) {
                                    player.getInventory().addItem(mapItem);
                                    player.sendMessage(Component.text(
                                            "[GifMap] Animated map created! Hold it to view.", NamedTextColor.GREEN));
                                }
                                future.complete(mapItem);
                            } else {
                                future.complete(null);
                            }
                        } catch (Throwable t) {
                            plugin.getLogger().log(Level.SEVERE, "Failed to create animated map for " + player.getName() + ": " + t.getMessage(), t);
                            if (player.isOnline()) {
                                player.sendMessage(Component.text(
                                        "[GifMap] Failed to create animated map.", NamedTextColor.RED));
                            }
                            future.completeExceptionally(t);
                        }
                    });
                } catch (Throwable t) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to decode GIF for " + player.getName() + ": " + t.getMessage(), t);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (player.isOnline()) {
                            player.sendMessage(Component.text(
                                    "[GifMap] Failed to create animated map.", NamedTextColor.RED));
                        }
                    });
                    future.completeExceptionally(t);
                }
            });
        } catch (RejectedExecutionException e) {
            debug("GIF map queue is full");
            future.complete(null);
        }

        return future;
    }

    private void debug(String message) {
        plugin.getLogger().log(Level.FINE, "[GifMap] " + message);
    }

    /**
     * Creates an animated map from already-decoded frames. Must be called on the main thread.
     */
    private ItemStack createMapFromFrames(Player player, List<GifFrameDecoder.Frame> frames) {
        // Create the animated renderer
        AnimatedMapRenderer renderer = new AnimatedMapRenderer(frames);

        // Create a virtual MapView
        MapView mapView = Bukkit.createMap(null); // null = virtual, not tied to a world
        mapView.setLocked(true);
        mapView.setScale(MapView.Scale.CLOSEST);
        mapView.setTrackingPosition(false);

        // Remove default renderer and add ours
        mapView.getRenderers().clear();
        mapView.addRenderer(renderer);

        // Track the map
        AnimatedMapEntry entry = new AnimatedMapEntry(mapView, renderer);
        entry.holders.add(player.getUniqueId());
        activeMaps.put(mapView.getId(), entry);

        // Dispose original frame images to save memory (renderer has resized copies)
        for (GifFrameDecoder.Frame frame : frames) {
            frame.image().flush();
        }

        // Create the map item
        ItemStack mapItem = new ItemStack(Material.MAP);
        MapMeta meta = (MapMeta) mapItem.getItemMeta();
        if (meta == null) return null;

        meta.setMapView(mapView);
        meta.displayName(Component.text("Animated GIF", NamedTextColor.GOLD));
        meta.lore(List.of(
                Component.text("Frames: " + frames.size(), NamedTextColor.GRAY),
                Component.text("Right-click to view animation", NamedTextColor.GRAY)
        ));
        mapItem.setItemMeta(meta);

        debug("Created animated map " + mapView.getId() + " with " + frames.size() + " frames for " + player.getName());
        return mapItem;
    }

    /**
     * Removes all maps held by a player when they quit.
     */
    void cleanupPlayerMaps(Player player) {
        UUID uuid = player.getUniqueId();

        // Find all maps held by this player
        for (ItemStack hand : player.getInventory().getContents()) {
            if (hand == null || hand.getType() != Material.MAP) continue;
            if (!hand.hasItemMeta()) continue;

            MapMeta meta = (MapMeta) hand.getItemMeta();
            if (meta == null || !meta.hasMapView()) continue;

            MapView mapView = meta.getMapView();
            removeHolder(mapView.getId(), uuid);
        }
    }

    /**
     * Removes a player as a holder of a map. If no holders remain, the map is destroyed.
     */
    private void removeHolder(int mapId, UUID playerUuid) {
        AnimatedMapEntry entry = activeMaps.get(mapId);
        if (entry == null) return;

        entry.holders.remove(playerUuid);
        if (entry.holders.isEmpty()) {
            destroyMap(entry);
        }
    }

    /**
     * Destroys a map and releases all resources.
     */
    private void destroyMap(AnimatedMapEntry entry) {
        int mapId = entry.mapView.getId();
        activeMaps.remove(mapId);

        try {
            entry.renderer.dispose();
            entry.mapView.getRenderers().clear();
        } catch (Throwable t) {
            debug("Error destroying map " + mapId + ": " + t.getMessage());
        }

        debug("Destroyed animated map " + mapId);
    }

    /**
     * Periodically evicts maps that have no holders for too long.
     */
    private void evictAbandoned() {
        long now = System.currentTimeMillis();
        long maxAge = 10 * 60 * 1000L; // 10 minutes

        activeMaps.entrySet().removeIf(entry -> {
            if (entry.getValue().holders.isEmpty() && now - entry.getValue().createdAt > maxAge) {
                destroyMap(entry.getValue());
                return true;
            }
            return false;
        });
    }

    /**
     * Returns the number of currently active animated maps.
     */
    public int getActiveMapCount() {
        return activeMaps.size();
    }

    /**
     * Returns the active maps tracking map.
     */
    Map<Integer, AnimatedMapEntry> getActiveMaps() {
        return activeMaps;
    }

    private void debug(String message, Throwable throwable) {
        plugin.getLogger().log(Level.FINE, "[GifMap] " + message, throwable);
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;

        executor.shutdownNow();

        // Destroy all active maps
        List<AnimatedMapEntry> entries = new ArrayList<>(activeMaps.values());
        activeMaps.clear();

        for (AnimatedMapEntry entry : entries) {
            try {
                entry.renderer.dispose();
                entry.mapView.getRenderers().clear();
            } catch (Throwable ignored) {
            }
        }

        plugin.getLogger().info("[GifMap] Cleaned up " + entries.size() + " animated maps");
    }

    /**
     * Entry that tracks an active animated map.
     */
    private static final class AnimatedMapEntry {
        final MapView mapView;
        final AnimatedMapRenderer renderer;
        final Set<UUID> holders = Collections.synchronizedSet(new HashSet<>());
        final long createdAt;

        AnimatedMapEntry(MapView mapView, AnimatedMapRenderer renderer) {
            this.mapView = mapView;
            this.renderer = renderer;
            this.createdAt = System.currentTimeMillis();
        }
    }
}
