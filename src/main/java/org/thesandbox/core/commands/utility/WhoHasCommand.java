package org.thesandbox.core.commands.utility;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.commands.CommandMessages;
import org.thesandbox.core.util.colors.Colors;

import java.util.*;
import java.util.stream.Collectors;

public class WhoHasCommand extends BaseCommand
{
    private final JavaPlugin plugin;

    public WhoHasCommand(JavaPlugin plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args)
    {
        if (args.length < 1)
        {
            sendUsage(sender, Colors.RED + "Usage: /" + label + " <item> [clear]");
            return true;
        }

        final boolean doClear = args.length >= 2 && "clear".equalsIgnoreCase(args[1]);
        final String materialName = args[0];
        final Material material = Material.matchMaterial(materialName);

        if (material == null)
        {
            sendError(sender, Colors.RED + "Invalid item: " + materialName);
            return true;
        }

        // CLEAR mode: broadcast &b and do not list users
        if (doClear)
        {
            if (!sender.hasPermission("sandbox.staff"))
            {
                sendError(sender, Colors.RED + "You do not have permission to clear inventories.");
                return true;
            }

            final String actor = (sender instanceof Player p) ? p.getName() : "CONSOLE";
            Bukkit.getServer().broadcast(org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.SERVER, Colors.AQUA + actor + " - Clearing all inventories of " + material.name()));

            for (final Player target : Bukkit.getOnlinePlayers())
            {
                // Do not clear from staff
                if (target.hasPermission("sandbox.staff"))
                    continue;

                if (target.getInventory().contains(material))
                {
                    target.getInventory().remove(material);
                }
            }
            return true; // do not list players when clearing
        }

        // LIST mode: show who has the item
        final List<String> players = new ArrayList<>();
        for (final Player player : Bukkit.getOnlinePlayers())
        {
            if (player.getInventory().contains(material))
            {
                players.add(player.getName());
            }
        }

        if (players.isEmpty())
        {
            sendCommand(sender, Colors.GRAY + "There are no players with " + material.name());
        }
        else
        {
            sender.sendMessage(org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.COMMAND, Colors.GRAY + "Players with " + material.name() + ": "
                    + Colors.RED + String.join(", ", players)));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        if (args.length == 1)
        {
            final String partial = args[0].toUpperCase(Locale.ENGLISH);
            return Arrays.stream(Material.values())
                    .map(Enum::name)
                    .filter(n -> n.startsWith(partial))
                    .sorted()
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && sender.hasPermission("sandbox.staff"))
        {
            return Collections.singletonList("clear");
        }

        return Collections.emptyList();
    }
}
