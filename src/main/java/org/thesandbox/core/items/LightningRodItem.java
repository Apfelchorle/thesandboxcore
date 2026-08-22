package org.thesandbox.core.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LightningRodItem implements Item {

    private static final long COOLDOWN_MS = 3000; // 3 seconds
    private final ItemKeys keys;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public LightningRodItem(ItemKeys keys) {
        this.keys = keys;
    }

    @Override
    public ItemStack create() {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Lightning Rod", NamedTextColor.YELLOW));
        meta.lore(java.util.List.of(
                Component.text("Strike Bad Actors Down!", NamedTextColor.DARK_GRAY, TextDecoration.BOLD)
        ));
        meta.getPersistentDataContainer().set(keys.lightningRod, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(keys.lightningRod, PersistentDataType.BYTE);
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(player.getUniqueId());
        if (last != null && now - last < COOLDOWN_MS) return;
        cooldowns.put(player.getUniqueId(), now);

        Block target = player.getTargetBlockExact(50);
        Location strikeLoc = (target != null) ? target.getLocation().add(0.5, 1, 0.5) : player.getLocation();
        player.getWorld().strikeLightning(strikeLoc);
    }
}
