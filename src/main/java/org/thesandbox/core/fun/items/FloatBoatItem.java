package org.thesandbox.core.fun.items;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerInput;
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
import org.bukkit.util.Vector;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.fun.items.itemUTILS.Item;
import org.thesandbox.core.fun.items.itemUTILS.ItemKeys;
import org.thesandbox.core.util.PlayerDataKeys;
import org.thesandbox.core.util.PluginConfigManager;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FloatBoatItem extends PacketListenerCommon implements Item, Listener, PacketListener {

    private static final String NAME = PlayerDataKeys.FLOAT_BOAT;
    public static final Set<UUID> ALLOWED_DISMOUNTS = ConcurrentHashMap.newKeySet();

    private static final double VERTICAL_SPEED = 0.8;
    private static final double FLY_SPEED = 1.2;

    private final TheSandboxCore plugin;
    private final PluginConfigManager configManager;
    private final ItemKeys keys;

    public FloatBoatItem(TheSandboxCore plugin, PluginConfigManager configManager, ItemKeys keys) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.keys = keys;

        Bukkit.getPluginManager().registerEvents(this, plugin);
        PacketEvents.getAPI().getEventManager().registerListener(this);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_INPUT) return;

        Player player = event.getPlayer();
        if (player == null) return;

        WrapperPlayClientPlayerInput input = new WrapperPlayClientPlayerInput(event);

        final boolean jumpPressed = input.isJump();
        final boolean sneakPressed = input.isShift();

        final float forwardInput = input.isForward() ? 1f : (input.isBackward() ? -1f : 0f);
        final float sidewaysInput = input.isLeft() ? 1f : (input.isRight() ? -1f : 0f);

        Bukkit.getScheduler().runTask(plugin, () -> {
            Entity vehicle = player.getVehicle();
            if (!(vehicle instanceof Boat boat) || !isFloatBoat(boat) || !boat.isValid()) {
                return;
            }

            float yaw = player.getLocation().getYaw();
            float pitch = player.getLocation().getPitch();

            double yawRadians = Math.toRadians(yaw);
            double pitchRadians = Math.toRadians(pitch);

            double rotX = -Math.sin(yawRadians) * Math.cos(pitchRadians);
            double rotZ = Math.cos(yawRadians) * Math.cos(pitchRadians);
            double rotY = -Math.sin(pitchRadians);

            Vector direction = new Vector(rotX, rotY, rotZ);
            if (direction.lengthSquared() > 0) {
                direction.normalize();
            }

            Vector newVel = new Vector(0, 0, 0);

            if (forwardInput > 0) {
                newVel.add(direction.clone().multiply(FLY_SPEED));
            } else if (forwardInput < 0) {
                newVel.add(direction.clone().multiply(-FLY_SPEED));
            }

            if (sidewaysInput != 0) {
                Vector sideDirection = new Vector(-direction.getZ(), 0, direction.getX());
                if (sideDirection.lengthSquared() > 0) {
                    sideDirection.normalize();
                }
                if (sidewaysInput > 0) {
                    newVel.add(sideDirection.multiply(FLY_SPEED * 0.6));
                } else {
                    newVel.add(sideDirection.multiply(-FLY_SPEED * 0.6));
                }
            }

            if (jumpPressed || pitch < -25.0f) {
                newVel.setY(VERTICAL_SPEED);
            } else if (sneakPressed) {
                newVel.setY(-VERTICAL_SPEED);
            } else {
                newVel.setY(0);
            }

            boat.setVelocity(newVel);
            boat.setFallDistance(0);
        });
    }

    @Override
    public ItemStack create() {
        ItemStack item = new ItemStack(Material.OAK_BOAT);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text(NAME, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("A boat that defies gravity.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Space/Look Up: Go Up | Shift: Lower | W/A/S/D: Drive", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("/dismount to exit safely!", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
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
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getPrice() {
        return configManager.getOrCreate("items." + NAME + ".price", 30);
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
            if (ALLOWED_DISMOUNTS.remove(player.getUniqueId())) {
                return;
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        ALLOWED_DISMOUNTS.remove(event.getPlayer().getUniqueId());
    }

    public void cleanup() {
        PacketEvents.getAPI().getEventManager().unregisterListener(this);
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