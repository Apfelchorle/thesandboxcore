package org.thesandbox.core.fun.items.itemUTILS;

import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public interface Item {
    ItemStack create();
    boolean matches(ItemStack item);
    void onInteract(PlayerInteractEvent e);
    String getName();

    int getPrice();
}
