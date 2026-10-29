package org.thesandbox.core.fun.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.fun.items.itemUTILS.Item;
import org.thesandbox.core.fun.items.itemUTILS.ItemKeys;
import org.thesandbox.core.util.PlayerDataKeys;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WindRodItem implements Item {

    private final TheSandboxCore plugin;
    private final ItemKeys keys;
    private final long cooldownMs;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final String NAME = PlayerDataKeys.WIND_ROD;

    public WindRodItem(TheSandboxCore plugin, ItemKeys keys) {
        this.plugin = plugin;
        this.keys = keys;
        this.cooldownMs = Math.max(0L, Math.round(plugin.getConfig().getDouble("items." + NAME + ".cooldown", 0.5) * 1000.0));
    }

    @Override
    public ItemStack create() {
        ItemStack item = new ItemStack(Material.BREEZE_ROD);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(NAME, NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Fire a burst of three wind charges.", NamedTextColor.GRAY)
        ));
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(keys.Wind_Rod, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(keys.Wind_Rod, PersistentDataType.BYTE);
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(player.getUniqueId());
        if (last != null && now - last < cooldownMs) return;
        cooldowns.put(player.getUniqueId(), now);

        Location eye = player.getEyeLocation();
        Vector forward = eye.getDirection();
        if (forward.lengthSquared() == 0) {
            forward = new Vector(0, 0, 1);
        } else {
            forward.normalize();
        }

        Vector up = new Vector(0, 1, 0);
        Vector right = forward.clone().crossProduct(up);
        if (right.lengthSquared() < 1.0E-6) {
            right = forward.clone().crossProduct(new Vector(1, 0, 0));
        }
        if (right.lengthSquared() > 0) {
            right.normalize();
        }

        double[] spreads = { -0.12, 0.0, 0.12 };
        Location spawnAt = eye.clone().add(forward.clone().multiply(0.4));
        for (double spread : spreads) {
            Vector velocity = forward.clone().add(right.clone().multiply(spread));
            if (velocity.lengthSquared() == 0) {
                velocity = forward.clone();
            } else {
                velocity.normalize();
            }
            player.launchProjectile(WindCharge.class, velocity.multiply(1.6), charge -> charge.teleport(spawnAt));
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getPrice() {
        return plugin.getConfig().getInt("items." + NAME + ".price", 225);
    }
}
