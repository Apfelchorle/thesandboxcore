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
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
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

import java.util.List;

public class FloatBoatItem implements Item, Listener {

    private static final String NAME = PlayerDataKeys.FLOAT_BOAT;
    private static final double VERTICAL_SPEED = 0.25;

    private final TheSandboxCore plugin;
    private final ItemKeys keys;

    public FloatBoatItem(TheSandboxCore plugin, ItemKeys keys) {
        this.plugin = plugin;
        this.keys = keys;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startInputMonitor();
    }

    @Override
    public ItemStack create() {
        ItemStack item = new ItemStack(Material.OAK_BOAT);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text(NAME, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("A boat that defies gravity.", NamedTextColor.DARK_GRAY),
                    Component.text("Shift: Lower | /dismount to exit", NamedTextColor.GRAY)
            ));
            meta.setEnchantmentGlintOverride(true);
            meta.getPersistentDataContainer().set(keys.Float_Boat, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(keys.Float_Boat, PersistentDataType.BYTE);
    }

    @Override
    public void onInteract(PlayerInteractEvent e) {
        // Handled by vanilla placement
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
        if (!(event.getEntity() instanceof Boat boat)) return;
        Player player = event.getPlayer();
        if (player == null) return;

        ItemStack used = player.getInventory().getItem(event.getHand());
        if (!matches(used)) return;

        applyNoGravity(boat);
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof Boat boat && isFloatBoat(boat)) {
                boat.setGravity(false);
            }
        }
    }

    @EventHandler
    public void onJump(PlayerJumpEvent event) {
        Player player = event.getPlayer();
        if (!(player.getVehicle() instanceof Boat boat) || !isFloatBoat(boat)) return;

        Vector currentVel = boat.getVelocity();
        boat.setVelocity(new Vector(currentVel.getX(), VERTICAL_SPEED, currentVel.getZ()));
    }

    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getDismounted() instanceof Boat boat)) return;

        if (isFloatBoat(boat)) {
            event.setCancelled(true);
        }
    }

    private void startInputMonitor() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!(player.getVehicle() instanceof Boat boat) || !isFloatBoat(boat)) continue;

                    if (player.isSneaking()) {
                        Vector currentVel = boat.getVelocity();
                        boat.setVelocity(new Vector(currentVel.getX(), -VERTICAL_SPEED, currentVel.getZ()));
                    }
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