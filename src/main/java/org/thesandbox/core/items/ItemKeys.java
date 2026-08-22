package org.thesandbox.core.items;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class ItemKeys {
    public final NamespacedKey lightningRod;

    public ItemKeys(Plugin plugin) {
        this.lightningRod = new NamespacedKey(plugin, "lightning_rod");
    }
}
