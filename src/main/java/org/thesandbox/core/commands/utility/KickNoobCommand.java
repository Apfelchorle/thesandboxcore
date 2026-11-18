package org.thesandbox.core.commands.utility;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.util.colors.Colors;

import java.util.List;

public class KickNoobCommand extends BaseCommand
{
    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args)
    {
        final String who = (sender instanceof Player) ? sender.getName() : "CONSOLE";

        // Broadcast admin action
        Bukkit.getServer().broadcast(org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.SERVER, Colors.RED + who + " - Kicking all non-staff users"));

        int kicked = 0;
        for (Player p : Bukkit.getOnlinePlayers())
        {
            // Skip staff (sandbox.staff = staff permission)
            if (p.hasPermission("sandbox.staff"))
                continue;

            try {
                p.kick(org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.SERVER, Colors.RED + "All non-staff were kicked by " + who + "."));
                kicked++;
            } catch (Throwable ignored) {}
        }

        sendCommand(sender, Colors.GRAY + "Kicked " + kicked + " non-staff player(s).");
        return true;
    }
}
