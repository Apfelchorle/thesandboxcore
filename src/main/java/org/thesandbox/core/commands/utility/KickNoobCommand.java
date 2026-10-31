package org.thesandbox.core.commands.utility;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.thesandbox.core.commands.CommandMessages;
import org.thesandbox.core.commands.ISubCommand;
import org.thesandbox.core.util.Colors;

import java.util.Collections;
import java.util.List;

public class KickNoobCommand implements ISubCommand
{
    private final JavaPlugin plugin;

    public KickNoobCommand(JavaPlugin plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args)
    {
        final String who = (sender instanceof Player) ? sender.getName() : "CONSOLE";

        // Broadcast admin action
        Colors.broadcast(CommandMessages.server(Colors.RED + who + " - Kicking all non-staff users"));

        int kicked = 0;
        for (Player p : Bukkit.getOnlinePlayers())
        {
            // Skip staff (sandbox.staff = staff permission)
            if (p.hasPermission("sandbox.staff"))
                continue;

            try {
                p.kick(Colors.component(CommandMessages.server(Colors.RED + "All non-staff were kicked by " + who + ".")));
                kicked++;
            } catch (Throwable ignored) {}
        }

        sender.sendMessage(CommandMessages.command(Colors.GRAY + "Kicked " + kicked + " non-staff player(s)."));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        return Collections.emptyList();
    }
}
