package org.thesandbox.core.commands.Admin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.thesandbox.core.commands.BaseCommand;

/**
 * Removes all dropped gold ingot items from all loaded worlds.
 */
public class GoldWipeCommand extends BaseCommand {

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!hasStaffPermission(sender)) {
            return true;
        }

        int totalRemoved = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Item droppedItem) {
                    ItemStack stack = droppedItem.getItemStack();
                    if (stack.getType() == org.bukkit.Material.GOLD_INGOT) {
                        entity.remove();
                        totalRemoved += stack.getAmount();
                    }
                }
            }
        }

        Component msg = Component.text("Removed " + totalRemoved + " dropped gold ingot(s).", NamedTextColor.GOLD);
        Bukkit.broadcast(msg);
        return true;
    }
}
