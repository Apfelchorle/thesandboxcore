package org.thesandbox.core.commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;

public class LocalSpawnCommand implements ISubCommand
{
    private final JavaPlugin plugin;

    public LocalSpawnCommand(JavaPlugin plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args)
    {
        if (!(sender instanceof Player)) {
            sender.sendMessage(CommandMessages.error(ChatColor.RED + "This command can only be used in-game."));
            return true;
        }

        Player player = (Player) sender;
        Location spawn = player.getWorld().getSpawnLocation();

        // Teleport synchronously (safe on main thread)
        player.teleport(spawn);

        player.sendMessage(CommandMessages.command(ChatColor.YELLOW + "Teleported to spawnpoint for world \"" +
                player.getWorld().getName() + "\"."));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        return Collections.emptyList();
    }
}