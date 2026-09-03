package org.thesandbox.core.fun.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.fun.items.itemUTILS.Item;
import org.thesandbox.core.fun.items.itemUTILS.ItemKeys;
import org.thesandbox.core.util.PlayerDataKeys;
import org.thesandbox.core.util.PluginConfigManager;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StackingPotatoItem implements Item, Listener {

    private final String NAME = PlayerDataKeys.Stacking_Potato;
    private final PluginConfigManager pluginConfigManager;
    private final TheSandboxCore plugin;
    private final ItemKeys itemKeys;

    // Tracks each player's current stack, bottom to top
    private final Map<UUID, Deque<LivingEntity>> stacks = new HashMap<>();

    public StackingPotatoItem(PluginConfigManager pluginConfigManager, TheSandboxCore plugin, ItemKeys itemKeys) {
        this.pluginConfigManager = pluginConfigManager;
        this.plugin = plugin;
        this.itemKeys = itemKeys;
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
                Component.text("Left-click to drop the stack.", NamedTextColor.GRAY)
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
        // Left-click = drop the stack
        if (e.getAction() != Action.LEFT_CLICK_AIR && e.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        if (!matches(e.getItem())) {
            return;
        }

        dropStack(e.getPlayer());
    }

    @EventHandler
    public void onRightClickEntity(PlayerInteractEntityEvent e) {
        Player player = e.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();

        if (!matches(held)) {
            return;
        }

        if (!(e.getRightClicked() instanceof LivingEntity target)) {
            return;
        }

        e.setCancelled(true);

        Deque<LivingEntity> stack = stacks.computeIfAbsent(player.getUniqueId(), k -> new ArrayDeque<>());

        Entity mountOnto = stack.isEmpty() ? player : stack.peekLast();
        mountOnto.addPassenger(target);
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
        return pluginConfigManager.getOrCreate("items." + PlayerDataKeys.Stacking_Potato + ".price", 0);
    }
}