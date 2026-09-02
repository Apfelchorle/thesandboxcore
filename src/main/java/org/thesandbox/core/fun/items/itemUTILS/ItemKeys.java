package org.thesandbox.core.fun.items.itemUTILS;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class ItemKeys {
    public final NamespacedKey lightningRod;
    public final NamespacedKey loginMessages;

    public final NamespacedKey clownFish;

    public final NamespacedKey Rideable_Ender_Pearl;
    public final NamespacedKey Grappling_Hook;

    public ItemKeys(Plugin plugin) {
        this.lightningRod = new NamespacedKey(plugin, "lightning_rod");
        this.loginMessages = new NamespacedKey(plugin, "login_messages");
        this.clownFish = new NamespacedKey(plugin, "clown_fish");
        this.Rideable_Ender_Pearl = new NamespacedKey(plugin, "Rideable_Ender_Pearl");
        this.Grappling_Hook = new NamespacedKey(plugin, "Grappling_Hook");
    }
}