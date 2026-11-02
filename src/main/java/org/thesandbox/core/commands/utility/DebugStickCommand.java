package org.thesandbox.core.commands.utility;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.commands.CommandMessages;
import org.thesandbox.core.util.colors.Colors;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class DebugStickCommand extends BaseCommand
{
    private final JavaPlugin plugin;

    public DebugStickCommand(JavaPlugin plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args)
    {
        if (!(sender instanceof Player player))
        {
            sendError(sender, Colors.RED + "This command can only be used in-game.");
            return true;
        }

        // Build the item (DEBUG_STICK if present, else STICK)
        Material mat = getDebugStickMaterial();
        ItemStack item = new ItemStack(mat, 1);

        // API 1.21 always exposes Adventure item meta methods.
        ItemMeta meta = item.getItemMeta();
        if (meta != null)
        {
            meta.displayName(Component.text("Debug Stick", NamedTextColor.LIGHT_PURPLE));
            meta.lore(Arrays.asList(
                    Component.text("Behold: The DEBUG STICK!", NamedTextColor.GOLD),
                    Component.text("Left click to select the block to change.", NamedTextColor.YELLOW),
                    Component.text("Right click to change its properties!", NamedTextColor.RED)
            ));
            item.setItemMeta(meta);
        }

        // Give to player (drop if full)
        HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        if (!leftovers.isEmpty())
        {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
            sendCommand(player, Colors.RED + "Your inventory is full; dropped the item at your feet.");
        }
        else
        {
            sendCommand(player, Colors.GREEN + "You received a Debug Stick.");
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        return Collections.emptyList();
    }

    private Material getDebugStickMaterial()
    {
        try {
            return Material.valueOf("DEBUG_STICK");
        } catch (IllegalArgumentException ex) {
            return Material.STICK; // fallback for older versions
        }
    }
}
