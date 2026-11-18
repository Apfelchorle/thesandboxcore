package org.thesandbox.core.services.world;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * Loads known worlds during startup instead of on the first teleport.
 *
 * <p>{@link WorldCreator#createWorld()} blocks the calling thread, so doing it
 * from a command handler freezes the server mid-play. Worlds that already
 * exist on disk are loaded here while the server is still starting up. Missing
 * worlds are never generated implicitly.</p>
 */
public final class WorldPreloadService {

    public static final List<String> DEFAULT_WORLDS = List.of(
            "world",
            "world_nether",
            "world_the_end",
            "masterbuilderworld",
            "flatlands"
    );

    private WorldPreloadService() {
    }

    public static void preload(JavaPlugin plugin) {
        if (!plugin.getConfig().getBoolean("worlds.preload-enabled", true)) {
            return;
        }

        List<String> configured = plugin.getConfig().getStringList("worlds.preload");
        Collection<String> names = configured.isEmpty() ? DEFAULT_WORLDS : configured;

        for (String raw : names) {
            if (raw == null || raw.isBlank()) continue;
            String name = raw.trim();
            if (Bukkit.getWorld(name) != null) continue;

            File folder = new File(Bukkit.getWorldContainer(), name);
            if (!folder.isDirectory()) continue;

            try {
                World world = new WorldCreator(name).createWorld();
                if (world != null) {
                    plugin.getLogger().info("[worlds] Pre-loaded '" + name + "'.");
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("[worlds] Could not pre-load '" + name + "': " + t.getMessage());
            }
        }
    }
}
