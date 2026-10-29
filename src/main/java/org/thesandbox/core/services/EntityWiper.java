package org.thesandbox.core.services;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.thesandbox.core.fun.Utils;

import java.util.*;

/**
 * Removes entity clutter (dropped items, arrows, minecarts, ...) per chunk.
 *
 * Config (all optional, shown with defaults):
 * <pre>
 * entity-wiper:
 *   enabled: false              # periodic automatic wipe
 *   interval-minutes: 15
 *   chunk-threshold: 20         # an automatic wipe only touches chunks holding at least this many wipeable entities
 *   remove-explosives: false    # also remove primed TNT / fireballs etc.
 *   include-armor-stands: false # armor stands are usually player builds, so they're opt-in
 *   item-despawn-seconds: 0     # >0 removes every dropped item this many seconds after it spawns
 *   broadcast: true             # announce automatic wipes that removed something
 * </pre>
 * Previously this class was never instantiated, so none of it ever ran.
 */
public class EntityWiper implements Listener
{
    public static final int AUTO_WIPE_INTERVAL_DEFAULT = 15;
    public static final int CHUNK_ENTITY_MAX = 20;

    private final JavaPlugin plugin;
    private final List<Class<? extends Entity>> wipables = new ArrayList<>();
    private BukkitTask wipeTask;

    public EntityWiper(JavaPlugin plugin)
    {
        this.plugin = plugin;

        wipables.add(EnderCrystal.class);
        wipables.add(EnderSignal.class);
        wipables.add(ExperienceOrb.class);
        wipables.add(Projectile.class);
        wipables.add(FallingBlock.class);
        wipables.add(Firework.class);
        wipables.add(Item.class);
        wipables.add(ThrownPotion.class);
        wipables.add(ThrownExpBottle.class);
        wipables.add(AreaEffectCloud.class);
        wipables.add(Minecart.class);
        wipables.add(Boat.class);
    }

    private FileConfiguration cfg()
    {
        return plugin.getConfig();
    }

    /** (Re)starts the periodic wipe according to config. Safe to call repeatedly (e.g. on reload). */
    public void startFromConfig()
    {
        stop();
        if (!cfg().getBoolean("entity-wiper.enabled", false))
        {
            return;
        }
        long minutes = Math.max(1, cfg().getLong("entity-wiper.interval-minutes", AUTO_WIPE_INTERVAL_DEFAULT));
        long ticks = minutes * 60L * 20L;

        wipeTask = Bukkit.getScheduler().runTaskTimer(plugin, () ->
        {
            int removed = wipeEntities(false);
            if (removed > 0 && cfg().getBoolean("entity-wiper.broadcast", true))
            {
                Bukkit.broadcast(net.kyori.adventure.text.Component.text(
                        "Cleared " + removed + " entities to reduce lag.",
                        net.kyori.adventure.text.format.NamedTextColor.GRAY));
            }
        }, ticks, ticks);
        Utils.log("entitywiper started (every " + minutes + " min)");
    }

    public void stop()
    {
        if (wipeTask != null)
        {
            wipeTask.cancel();
            wipeTask = null;
            Utils.log("entitywiper stopped");
        }
    }

    public boolean isWipeable(Entity entity)
    {
        if (entity instanceof Player) return false;
        // Never remove something a player is sitting in, or a named entity.
        if (entity.customName() != null) return false;
        for (Entity passenger : entity.getPassengers())
        {
            if (passenger instanceof Player) return false;
        }

        if (cfg().getBoolean("entity-wiper.include-armor-stands", false) && entity instanceof ArmorStand)
        {
            return true;
        }
        for (Class<? extends Entity> type : wipables)
        {
            if (type.isInstance(entity))
            {
                return true;
            }
        }
        return false;
    }

    /** Wipes every world. @param force ignore the per-chunk threshold. @return amount removed. */
    public int wipeEntities(boolean force)
    {
        int removed = 0;
        for (World world : Bukkit.getWorlds())
        {
            removed += wipeEntities(world, force);
        }
        Utils.log("entitywiper removed " + removed + " entities");
        return removed;
    }

    public int wipeEntities(World world, boolean force)
    {
        int removed = 0;
        boolean removeExplosives = cfg().getBoolean("entity-wiper.remove-explosives", false);
        int threshold = Math.max(1, cfg().getInt("entity-wiper.chunk-threshold", CHUNK_ENTITY_MAX));

        Map<Chunk, List<Entity>> chunkEntities = new HashMap<>();

        for (Entity entity : world.getEntities())
        {
            if (entity instanceof Player) continue;

            if (removeExplosives && entity instanceof Explosive)
            {
                entity.remove();
                removed++;
                continue;
            }

            if (!isWipeable(entity))
            {
                continue;
            }
            chunkEntities.computeIfAbsent(entity.getLocation().getChunk(), k -> new ArrayList<>()).add(entity);
        }

        for (List<Entity> entitiesInChunk : chunkEntities.values())
        {
            if (!force && entitiesInChunk.size() < threshold)
            {
                continue;
            }

            for (Entity entity : entitiesInChunk)
            {
                removed += (entity instanceof Item item) ? item.getItemStack().getAmount() : 1;
                entity.remove();
            }
        }
        return removed;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event)
    {
        long seconds = cfg().getLong("entity-wiper.item-despawn-seconds", 0);
        if (seconds <= 0)
        {
            return;
        }

        Item entity = event.getEntity();
        Bukkit.getScheduler().runTaskLater(plugin, () ->
        {
            if (entity.isValid())
            {
                entity.remove();
            }
        }, seconds * 20L);
    }
}
