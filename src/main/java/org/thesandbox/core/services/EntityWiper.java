package org.thesandbox.core.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class EntityWiper implements org.bukkit.event.Listener
{
    public static final int AUTO_WIPE_INTERVAL_DEFAULT = 15;
    public static final long ITEM_DESPAWN_RATE = 20L * 20L;
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
        wipables.add(ArmorStand.class);
        wipables.add(Mannequin.class);
    }



    public void start(long intervalTicks)
    {
        stop();

        wipeTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                () -> wipeEntities(false),
                intervalTicks,
                intervalTicks
        );
    }

    public void stop()
    {
        if (wipeTask != null)
        {
            wipeTask.cancel();
            wipeTask = null;
        }
    }

    public boolean isWipeable(Entity entity)
    {
        for (Class<? extends Entity> type : wipables)
        {
            if (type.isAssignableFrom(entity.getClass()))
            {
                return true;
            }
        }

        return false;
    }

    public int wipeEntities(boolean force)
    {
        int removed = 0;

        for (World world : Bukkit.getWorlds())
        {
            removed += wipeEntities(world, force);
        }

        return removed;
    }

    public int wipeEntities(World world, boolean force)
    {
        int removed = 0;

        boolean wipeExplosions = plugin.getConfig().getBoolean("allow-explosions", true);
        Iterator<Entity> entities = world.getEntities().iterator();

        Map<Chunk, List<Entity>> chunkEntities = new HashMap<>();

        while (entities.hasNext())
        {
            Entity entity = entities.next();

            if (wipeExplosions && Explosive.class.isAssignableFrom(entity.getClass()))
            {
                entity.remove();
                removed++;
                continue;
            }

            if (!isWipeable(entity))
            {
                continue;
            }

            Chunk chunk = entity.getLocation().getChunk();
            List<Entity> list = chunkEntities.computeIfAbsent(chunk, k -> new ArrayList<>());

            list.add(entity);
        }

        for (Chunk chunk : chunkEntities.keySet())
        {
            List<Entity> entitiesInChunk = chunkEntities.get(chunk);

            if (!force && entitiesInChunk.size() < CHUNK_ENTITY_MAX)
            {
                continue;
            }

            for (Entity entity : entitiesInChunk)
            {
                if (entity instanceof Item item)
                {
                    removed += item.getItemStack().getAmount();
                }
                else
                {
                    removed++;
                }

                entity.remove();
            }
        }

        return removed;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event)
    {
        Item entity = event.getEntity();

        Bukkit.getScheduler().runTaskLater(plugin, () ->
        {
            if (entity.isValid())
            {
                entity.remove();
            }
        }, ITEM_DESPAWN_RATE);
    }
}