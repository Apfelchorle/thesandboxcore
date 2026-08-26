package org.thesandbox.core.fun.items.itemUTILS;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
public class ItemListener implements Listener {

    private final List<Item> items;

    public ItemListener(List<Item> items) {
        this.items = items;
    }

    @EventHandler
    public void OnInteract(PlayerInteractEvent event) {
        ItemStack held = event.getItem();
        if (held == null) return;

        if (event.getHand() != EquipmentSlot.HAND) return;

        for (Item item : items) {
            if (item.matches(held)) {
                item.onInteract(event);
                return;
            }
        }
    }

}
