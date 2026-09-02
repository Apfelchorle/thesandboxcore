package org.thesandbox.core.fun.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.fun.items.itemUTILS.Item;
import org.thesandbox.core.fun.items.itemUTILS.ItemKeys;
import org.thesandbox.core.util.PlayerDataKeys;
import org.thesandbox.core.util.PlayerDataListener;

import java.util.Arrays;
import java.util.List;

public class GrapplingHookItem implements Item, Listener {

    private final PlayerDataListener playerDataListener;
    private final TheSandboxCore plugin;
    private final ItemKeys keys;
    private static final String NAME = PlayerDataKeys.GRAPPLING_HOOK;

    public GrapplingHookItem(PlayerDataListener playerDataListener, TheSandboxCore plugin, ItemKeys keys) {
        this.playerDataListener = playerDataListener;
        this.plugin = plugin;
        this.keys = keys;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public ItemStack create() {
        ItemStack item = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(NAME, TextColor.color(48, 123, 255))
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(List.of(
                Component.text("Grapple Like Your Favourite Fictional Character",
                        TextColor.color(46, 46, 46),
                        TextDecoration.ITALIC)
        ));

        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(keys.Grappling_Hook, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(keys.Grappling_Hook, PersistentDataType.BYTE);
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
        return plugin.getConfig().getInt("items." + PlayerDataKeys.GRAPPLING_HOOK + ".price", 100);
    }

    @EventHandler
    public void onGrapple(PlayerFishEvent event) {
        Player player = event.getPlayer();

        ItemStack rod = (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND)
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();

        if (!matches(rod)) {
            return;
        }
        PlayerFishEvent.State state = event.getState();

        List<String> validstates = Arrays.asList("REEL_IN", "CAUGHT_ENTITY", "FAILED_ATTEMPT", "IN_GROUND");

        if (validstates.contains(state.toString())) {
            Location playerLoc = player.getLocation();
            Location hookLoc = event.getHook().getLocation();
            Vector direction = hookLoc.toVector().subtract(playerLoc.toVector());
            if (direction.lengthSquared() > 0) {
                direction.normalize();
            }
            direction.multiply(1.5);
            direction.setY(direction.getY() + 0.3);
            player.setVelocity(direction);
        }
    }
}