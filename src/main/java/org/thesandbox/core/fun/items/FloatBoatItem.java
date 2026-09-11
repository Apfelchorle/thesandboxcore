package org.thesandbox.core.fun.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.fun.items.itemUTILS.Item;
import org.thesandbox.core.fun.items.itemUTILS.ItemKeys;
import org.thesandbox.core.util.PlayerDataKeys;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FloatBoatItem implements Item, Listener {

    private static final String NAME = PlayerDataKeys.FLOAT_BOAT;

    // Increased speeds and forward boost to fix sluggishness
    private static final double VERTICAL_SPEED = 0.45;
    private static final double HORIZONTAL_BOOST = 0.08;

    public static final Set<UUID> ALLOWED_DISMOUNTS = ConcurrentHashMap.newKeySet();

    private final TheSandboxCore plugin;
    private final ItemKeys keys;
    private BukkitTask monitorTask;

    public FloatBoatItem(TheSandboxCore plugin, ItemKeys keys) {
        this.plugin = plugin;
        this.keys = keys;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startPitchMonitor();
    }

    @Override
    public ItemStack create() {
        ItemStack item = new ItemStack(Material.OAK_BOAT);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text(NAME, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("A boat that defies gravity.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Look Up : Go Up | Shift: Lower | /dismount to exit", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("TheSandBox Is Not Responsible For You Going Missing in OuterSpace!", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
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
        // Handled by vanilla boat placement
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getPrice() {
        return plugin.getConfig().getInt("items." + NAME + ".price", 30);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDismounted() instanceof Boat boat)) return;

        if (isFloatBoat(boat)) {
            // Remove the single player's permission on dismount
            if (ALLOWED_DISMOUNTS.remove(player.getUniqueId())) {
                return;
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up only the quitting player's UUID
        ALLOWED_DISMOUNTS.remove(event.getPlayer().getUniqueId());
    }

    private void startPitchMonitor() {
        if (monitorTask != null && !monitorTask.isCancelled()) {
            monitorTask.cancel();
        }

        monitorTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.isOnline()) continue;

                    Entity vehicle = player.getVehicle();
                    if (!(vehicle instanceof Boat boat) || !isFloatBoat(boat) || !boat.isValid()) {
                        continue;
                    }

                    Vector currentVel = boat.getVelocity();
                    float pitch = player.getLocation().getPitch();
                    Vector direction = player.getLocation().getDirection();

                    double targetY = 0;
                    if (pitch < -15.0f) {
                        targetY = VERTICAL_SPEED;
                    } else if (player.isSneaking()) {
                        targetY = -VERTICAL_SPEED;
                    }

                    // Add slight horizontal momentum using player direction to reduce mid-air drag
                    double targetX = currentVel.getX() + (direction.getX() * HORIZONTAL_BOOST);
                    double targetZ = currentVel.getZ() + (direction.getZ() * HORIZONTAL_BOOST);

                    Vector newVel = new Vector(targetX, targetY, targetZ);

                    // Cap maximum velocity to keep control manageable
                    if (newVel.lengthSquared() > 1.2) {
                        newVel.normalize().multiply(1.1);
                    }

                    boat.setVelocity(newVel);
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    /**
     * Call this ONLY when your plugin is disabling (onDisable).
     */
    public void cleanup() {
        if (monitorTask != null) {
            monitorTask.cancel();
        }
        ALLOWED_DISMOUNTS.clear();
    }

    private void applyNoGravity(Boat boat) {
        boat.setGravity(false);
        boat.getPersistentDataContainer().set(keys.Float_Boat, PersistentDataType.BYTE, (byte) 1);
    }

    private boolean isFloatBoat(Boat boat) {
        return boat != null && boat.getPersistentDataContainer().has(keys.Float_Boat, PersistentDataType.BYTE);
    }

    public boolean isFloatBoatPublic(Boat boat) {
        return isFloatBoat(boat);
    }
}