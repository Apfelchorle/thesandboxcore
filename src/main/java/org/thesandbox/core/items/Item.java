package org.thesandbox.core.items;

import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public interface Item {
    ItemStack create();
    boolean matches(ItemStack item);
    void onInteract(PlayerInteractEvent e);
    String getName();
}
