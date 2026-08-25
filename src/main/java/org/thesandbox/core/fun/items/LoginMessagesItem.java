package org.thesandbox.core.fun.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.util.PlayerDataKeys;
import org.thesandbox.core.util.PlayerDataListener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LoginMessagesItem implements Item {


    private final TheSandboxCore plugin;

    private final ItemKeys keys;

    private final long cooldownMs;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final String NAME = "LoginMessages";
    private final PlayerDataListener playerDataListener;

    public LoginMessagesItem(TheSandboxCore plugin, ItemKeys keys, PlayerDataListener playerDataListener) {
        this.plugin = plugin;
        this.keys = keys;
        this.cooldownMs = plugin.getConfig().getLong("items.loginMessages.cooldown", 1500);
        this.playerDataListener = playerDataListener;
    }


    @Override
    public ItemStack create() {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(NAME, NamedTextColor.WHITE));
        meta.lore(java.util.List.of(Component.text("Have Your Very Own Unique Nicknames!", NamedTextColor.DARK_GRAY, TextDecoration.BOLD)));
        meta.getPersistentDataContainer().set(keys.loginMessages, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(keys.loginMessages, PersistentDataType.BYTE);
    }

    @Override
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        e.setCancelled(true);
        Player p = e.getPlayer();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(p.getUniqueId());
        if (last != null && now - last < cooldownMs) return;
        cooldowns.put(p.getUniqueId(), now);

        String current = playerDataListener.get(p.getUniqueId(), PlayerDataKeys.LOGIN_MESSAGES_STATE, "not_owned");

        String next;
        if (current.equals("enabled")) {
            next = "disabled";
        } else {
            next = "enabled"; // covers "disabled" -> "enabled" and "not_owned" -> "enabled" (owning the item implies ownership)
        }

        playerDataListener.set(p.getUniqueId(), PlayerDataKeys.LOGIN_MESSAGES_STATE, next);

        if (next.equals("enabled")) {
            p.sendMessage(Component.text("Login message enabled!", NamedTextColor.GREEN));
        } else {
            p.sendMessage(Component.text("Login message disabled.", NamedTextColor.RED));
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getPrice() {
        return plugin.getConfig().getInt("items.loginMessages.price", 250);
    }
}
