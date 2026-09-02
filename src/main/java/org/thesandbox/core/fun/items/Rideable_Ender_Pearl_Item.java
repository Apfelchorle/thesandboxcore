package org.thesandbox.core.fun.items;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.fun.items.itemUTILS.Item;
import org.thesandbox.core.fun.items.itemUTILS.ItemKeys;
import org.thesandbox.core.util.PlayerDataKeys;

import java.util.List;

public class Rideable_Ender_Pearl_Item implements Item, Listener {

    private static final String NAME = PlayerDataKeys.RIDEABLE_ENDER_PEARL;

    private final TheSandboxCore plugin;
    private final ItemKeys keys;

    public Rideable_Ender_Pearl_Item(TheSandboxCore plugin, ItemKeys keys) {
        this.plugin = plugin;
        this.keys = keys;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public ItemStack create() {
        ItemStack item = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(NAME, NamedTextColor.DARK_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text(
                        "The Sandbox is not responsible for any injuries sustained while using this item.",
                        NamedTextColor.WHITE,
                        TextDecoration.ITALIC
                )
        ));
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(keys.Rideable_Ender_Pearl, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(keys.Rideable_Ender_Pearl, PersistentDataType.BYTE);
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        // Vanilla throw must proceed so the pearl can be launched and ridden.
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getPrice() {
        return plugin.getConfig().getInt("items.Rideable_Ender_Pearl_Item.price", 100);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLaunch(PlayerLaunchProjectileEvent event) {
        if (!(event.getProjectile() instanceof EnderPearl pearl)) {
            return;
        }
        if (!matches(event.getItemStack())) {
            return;
        }

        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!pearl.isValid() || pearl.isDead()) {
                return;
            }
            if (!player.isValid() || !player.isOnline()) {
                return;
            }
            pearl.addPassenger(player);
        });
    }
}
