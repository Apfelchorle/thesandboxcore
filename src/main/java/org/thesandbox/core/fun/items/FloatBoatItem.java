package org.thesandbox.core.fun.items;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.fun.items.itemUTILS.Item;
import org.thesandbox.core.fun.items.itemUTILS.ItemKeys;
import org.thesandbox.core.util.PlayerDataKeys;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FloatBoatItem implements Item, Listener {

    private final Map<UUID, Vector> lastBoatVelocity = new HashMap<>();
    private static final String NAME = PlayerDataKeys.FLOAT_BOAT;
    private static final double GRAVITY_THRESHOLD = 0.01;

    private final TheSandboxCore plugin;
    private final ItemKeys keys;

    public FloatBoatItem(TheSandboxCore plugin, ItemKeys keys) {
        this.plugin = plugin;
        this.keys = keys;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startVelocityMonitor();
    }

    @Override
    public ItemStack create() {
        ItemStack item = new ItemStack(Material.OAK_BOAT);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(NAME, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("A boat that has no gravity.", NamedTextColor.DARK_GRAY)
        ));
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(keys.Float_Boat, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(keys.Float_Boat, PersistentDataType.BYTE);
    }

    @Override
    public void onInteract(PlayerInteractEvent e) {
        // Vanilla boat placement must proceed so EntityPlaceEvent can mark it as no-gravity.
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getPrice() {
        return plugin.getConfig().getInt("items." + NAME + ".price", 30);
    }

    @EventHandler
    public void onBoatPlace(EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Boat boat)) {
            return;
        }

        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        ItemStack used = player.getInventory().getItem(event.getHand());
        if (!matches(used)) {
            return;
        }

        applyNoGravity(boat);
        lastBoatVelocity.put(boat.getUniqueId(), new Vector(0, 0, 0));
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof Boat boat && isFloatBoat(boat)) {
                boat.setGravity(false);
                lastBoatVelocity.put(boat.getUniqueId(), new Vector(0, 0, 0));
            }
        }
    }

    @EventHandler
    public void onSneakToggle(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!(player.getVehicle() instanceof Boat boat) || !isFloatBoat(boat)) return;
        if (event.isSneaking()) {
            boat.setVelocity(boat.getVelocity().setY(-0.2));
            boat.setGravity(true);
            lastBoatVelocity.put(boat.getUniqueId(), boat.getVelocity().clone());
        } else {
            boat.setGravity(false);
            lastBoatVelocity.put(boat.getUniqueId(), boat.getVelocity().clone());
        }
    }

    @EventHandler
    public void onJump(PlayerJumpEvent event) {
        Player player = event.getPlayer();
        if (!(player.getVehicle() instanceof Boat boat) || !isFloatBoat(boat)) return;
        boat.setVelocity(boat.getVelocity().setY(0.2));
        boat.setGravity(true);
        lastBoatVelocity.put(boat.getUniqueId(), boat.getVelocity().clone());
    }

    private void startVelocityMonitor() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!(player.getVehicle() instanceof Boat boat) || !isFloatBoat(boat)) continue;

                    UUID boatId = boat.getUniqueId();
                    Vector currentVelocity = boat.getVelocity();
                    Vector lastVelocity = lastBoatVelocity.getOrDefault(boatId, new Vector(0, 0, 0));

                    if (currentVelocity.distanceSquared(lastVelocity) > GRAVITY_THRESHOLD) {
                        if (!boat.hasGravity()) {
                            boat.setGravity(true);
                        }
                    } else if (boat.hasGravity()) {
                        boat.setGravity(false);
                    }

                    lastBoatVelocity.put(boatId, currentVelocity.clone());
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void applyNoGravity(Boat boat) {
        boat.setGravity(false);
        boat.getPersistentDataContainer().set(keys.Float_Boat, PersistentDataType.BYTE, (byte) 1);
    }

    private boolean isFloatBoat(Boat boat) {
        return boat.getPersistentDataContainer().has(keys.Float_Boat, PersistentDataType.BYTE);
    }
}
