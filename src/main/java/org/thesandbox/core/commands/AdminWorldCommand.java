package org.thesandbox.core.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class AdminWorldCommand implements ISubCommand
{
    private final JavaPlugin plugin;

    public AdminWorldCommand(JavaPlugin plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args)
    {
        if (!(sender instanceof Player))
        {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&c&lError &8» &cThis command can only be used in-game."));
            return true;
        }

        Player player = (Player) sender;

        String worldName = "adminworld";
        World world = Bukkit.getWorld(worldName);

        if (world == null)
        {
            // Try to load the world only if a folder exists
            File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
            if (worldFolder.exists() && worldFolder.isDirectory())
            {
                try
                {
                    world = WorldCreator.name(worldName).createWorld();
                }
                catch (Throwable t)
                {
                    world = null;
                }
            }
        }

        if (world == null)
        {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
            "&c&lError &8» &cThe adminworld is not loaded! Report this to a server administrator."));
            return true;
        }

        boolean success = player.teleport(world.getSpawnLocation());
        if (success)
        {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
            "&7&lCommand &8» &7Teleporting you to the adminworld."));
        }
        else
        {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
            "&c&lError &8» &cFailed to teleport. Report this to a server administrator."));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        return Collections.emptyList();
    }
}