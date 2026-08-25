package org.thesandbox.core.fun.items;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class ItemKeys {
    public final NamespacedKey lightningRod;
    public final NamespacedKey loginMessages;

    public ItemKeys(Plugin plugin) {
        this.lightningRod = new NamespacedKey(plugin, "lightning_rod");
        this.loginMessages = new NamespacedKey(plugin, "login_messages");
    }
}