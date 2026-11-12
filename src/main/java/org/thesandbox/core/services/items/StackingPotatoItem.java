package org.thesandbox.core.services.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.data.PlayerDataListener;
import org.thesandbox.core.services.entities.RideableDragonEntity;
import org.thesandbox.core.util.Utils;
import org.thesandbox.core.services.items.util.Item;
import org.thesandbox.core.services.items.util.ItemKeys;
import org.thesandbox.core.data.PlayerDataKeys;
import org.thesandbox.core.util.config.PluginConfigManager;

import java.util.*;

public class StackingPotatoItem implements Item, Listener {

    private static final int MAX_STACK_SIZE = 10;
    private final String NAME = PlayerDataKeys.Stacking_Potato;
    private final PluginConfigManager pluginConfigManager;
    private final TheSandboxCore plugin;
    private final PlayerDataListener playerDataListener;
    private final ItemKeys itemKeys;
    private final RideableDragonEntity rideableDragonEntity;
    private final Map<UUID, Deque<LivingEntity>> stacks = new HashMap<>();
    private final Map<UUID, Long> lastsnek = new HashMap<>();

    public StackingPotatoItem(PluginConfigManager pluginConfigManager, TheSandboxCore plugin, ItemKeys itemKeys, RideableDragonEntity rideableDragonEntity) {
        this.pluginConfigManager = pluginConfigManager;
        this.plugin = plugin;
        this.itemKeys = itemKeys;
        this.rideableDragonEntity = rideableDragonEntity;
        this.playerDataListener = plugin.getDataListener();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public ItemStack create() {
        ItemStack item = new ItemStack(Material.POTATO);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Stacking Potato", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(java.util.List.of(
                Component.text("Right-click an entity to stack it!", NamedTextColor.GRAY),
                Component.text("Left-click to drop the stack.", NamedTextColor.GRAY),
                Component.text("Double Shift To Change Modes.", NamedTextColor.GRAY)
        ));

        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(itemKeys.Stacking_Potato, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(itemKeys.Stacking_Potato, PersistentDataType.BYTE);
    }

    @Override
    public void onInteract(PlayerInteractEvent e) {
        // Left-click = drop the stack or dismount dragon
        if (e.getAction() != Action.LEFT_CLICK_AIR && e.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        if (!matches(e.getItem())) {
            return;
        }

        Player player = e.getPlayer();
        if (player.getVehicle() instanceof EnderDragon dragon) {
            RideableDragonEntity.ALLOWED_DISMOUNTS.add(player.getUniqueId());
            dragon.removePassenger(player);
            Utils.Feedback(player, "Dismounted dragon.", NamedTextColor.GREEN, Sound.ENTITY_ITEM_PICKUP);
            return;
        }

        dropStack(player);
    }

    private void modebasedoutcome(Player player, LivingEntity target) {
        // Ender Dragons can only be ridden, not stacked
        if (target instanceof EnderDragon dragon) {
            potato_ride(player, dragon);
            return;
        }

        // current modes are : stack | ride --> Requested By Tizz Tizzius The Ninty First
        UUID uuid = player.getUniqueId();
        switch(getmode(uuid)) {
            case "stack" -> potato_stack(player, target);
            case "drop" -> dropStack(player);
            case "ride" -> potato_ride(player, target);
            default -> Utils.Feedback(player,"Sorry That's Not Possible!",NamedTextColor.RED, Sound.ENTITY_VILLAGER_DEATH);
        }
    }

    private void potato_ride(Player player, LivingEntity target) {
        if (player.getVehicle() != null) {
            Utils.Feedback(player, "You are already riding an entity!", NamedTextColor.RED, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        if (target instanceof EnderDragon dragon) {
            RideableDragonEntity controller = rideableDragonEntity != null
                    ? rideableDragonEntity
                    : plugin.getRideableDragonEntity();
            if (controller != null) {
                controller.track(dragon);
            }
            dragon.setPhase(EnderDragon.Phase.HOVER);
            boolean success = dragon.addPassenger(player);
            if (success) {
                Utils.Feedback(player, "Mounted Dragon! Use WASD to fly, Jump to shoot fireballs.", NamedTextColor.LIGHT_PURPLE, Sound.ENTITY_ENDER_DRAGON_GROWL);
            } else {
                Utils.Feedback(player, "Could not mount dragon!", NamedTextColor.RED, Sound.ENTITY_VILLAGER_NO);
            }
            return;
        }

        // Normal (non-dragon) ride logic
        Entity current = target;
        while (current.getVehicle() != null) {
            current = current.getVehicle();
        }
        while (!current.getPassengers().isEmpty()) {
            current = current.getPassengers().getFirst();
        }
        boolean success = current.addPassenger(player);
        if (success) {
            Utils.Feedback(player, "Mounted " + target.getName() + "!", NamedTextColor.GREEN, Sound.ENTITY_ITEM_PICKUP);
        }
    }

    private void potato_stack(Player player, LivingEntity target) {

        if (!player.hasPermission("sandbox.staff") && target instanceof Player) {
            Utils.playSound(player, player.getLocation(), Sound.ENTITY_VILLAGER_NO);
            Utils.SendMessage(player, "Only Staff Can Stack Players!", TextColor.color(255, 3, 0));
            return;
        }

        if (target.equals(player)) {
            return;
        }

        if (target.getVehicle() != null) {
            return;
        }

        Deque<LivingEntity> stack = stacks.get(player.getUniqueId());

        if (stack != null && stack.contains(target)) {
            return;
        }

        if (stack != null && stack.size() >= MAX_STACK_SIZE && !player.hasPermission("sandbox.staff")) {
            Utils.playSound(player, player.getLocation(), Sound.ENTITY_VILLAGER_NO);
            Utils.SendMessage(player, "Maximum Stack Size Reached: " + MAX_STACK_SIZE, TextColor.color(255, 3, 0));
            return;
        }
        stack = stacks.computeIfAbsent(player.getUniqueId(), k -> new ArrayDeque<>());
        Entity mountOnto = stack.isEmpty() ? player : stack.peekLast();
        if (!mountOnto.isValid()) {
            stack.clear();
            return;
        }
        if (!mountOnto.addPassenger(target)) {
            return;
        }
        stack.addLast(target);
    }

    private void dropStack(Player player) {
        Deque<LivingEntity> stack = stacks.remove(player.getUniqueId());
        if (stack == null || stack.isEmpty()) {
            return;
        }
        while (!stack.isEmpty()) {
            LivingEntity entity = stack.pollLast();
            entity.leaveVehicle();
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getPrice() {
        return pluginConfigManager.getOrCreate("items." + PlayerDataKeys.Stacking_Potato + ".price", 50);
    }
    private String getmode(UUID uuid) {
        return playerDataListener.get(uuid, PlayerDataKeys.Stacking_Potato_Mode, "stack");
    }
    private void setmode(String mode, UUID uuid) {
        playerDataListener.set(uuid, PlayerDataKeys.Stacking_Potato_Mode, mode);
    }
    private void toggle_mode(UUID uuid) {
        String mode = getmode(uuid);
        String newMode = "stack".equalsIgnoreCase(mode) ? "ride" : "stack";
        setmode(newMode, uuid);
    }

    @EventHandler
    public void onDoubleShift(PlayerToggleSneakEvent e) {
        Player player = e.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!matches(held)) {
            return;
        }
        if (!e.isSneaking()) return;
        UUID uuid = player.getUniqueId();
        long now =  System.currentTimeMillis();
        if (lastsnek.containsKey(uuid)) {
            long diff = now - lastsnek.get(uuid);
            if (diff < pluginConfigManager.getOrCreate("items." + NAME + ".ms_detection_rate", 200)) {
                lastsnek.remove(uuid);
                toggle_mode(player.getUniqueId());
                Utils.Feedback(player,"StackingPotato Mode Changed To: " + getmode(uuid), NamedTextColor.DARK_GREEN, Sound.ENTITY_ITEM_PICKUP);
                return;
            }
        }
        lastsnek.put(uuid, now);
    }

    @EventHandler
    public void onRightClickEntity(PlayerInteractEntityEvent e) {
        if (e.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
            return;
        }

        Player player = e.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();

        if (!matches(held)) {
            return;
        }

        Entity clicked = e.getRightClicked();
        LivingEntity target;
        if (clicked instanceof LivingEntity living) {
            target = living;
        } else if (clicked instanceof ComplexEntityPart part && part.getParent() != null) {
            target = part.getParent();
        } else {
            return;
        }

        e.setCancelled(true);
        modebasedoutcome(player, target);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Deque<LivingEntity> stack = stacks.remove(e.getPlayer().getUniqueId());
        if (stack == null) {
            return;
        }
        while (!stack.isEmpty()) {
            LivingEntity entity = stack.pollLast();
            if (entity.isValid()) {
                entity.leaveVehicle();
            }
        }
        e.getPlayer().leaveVehicle();
        lastsnek.remove(e.getPlayer().getUniqueId());
    }


}
