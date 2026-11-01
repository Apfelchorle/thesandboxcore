package org.thesandbox.core.commands.utility;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.commands.CommandMessages;
import org.thesandbox.core.util.colors.Colors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WorldCommand extends BaseCommand
{
    private static final List<String> ALLOWED_WORLDS = List.of(
            "world",
            "world_nether",
            "world_the_end",
            "masterbuilderworld",
            "flatlands"
    );

    private final JavaPlugin plugin;

    public WorldCommand(JavaPlugin plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args)
    {
        if (!(sender instanceof Player player))
        {
            sender.sendMessage(CommandMessages.error(Colors.RED + "This command can only be used in-game."));
            return true;
        }

        if (args.length < 1)
        {
            player.sendMessage(CommandMessages.usage(Colors.RED + "Usage: /" + label + " <" + String.join("|", ALLOWED_WORLDS) + ">"));
            return true;
        }

        String requested = args[0].toLowerCase();
        if (!ALLOWED_WORLDS.contains(requested))
        {
            player.sendMessage(CommandMessages.error(Colors.RED + "Unknown world: " + requested));
            return true;
        }

        World world = WorldTeleportHelper.resolveWorld(requested);

        if (world == null)
        {
            player.sendMessage(CommandMessages.error(Colors.RED + "That world does not exist."));
            return true;
        }

        // Teleport to world spawn
        boolean success = player.teleport(world.getSpawnLocation());
        if (success)
        {
            player.sendMessage(CommandMessages.command(Colors.GRAY + "Teleporting you to " + requested + "."));
        }
        else
        {
            player.sendMessage(CommandMessages.error(Colors.RED + "Failed to teleport."));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        if (args.length == 1)
        {
            String prefix = args[0].toLowerCase();
            List<String> out = new ArrayList<>();
            for (String w : ALLOWED_WORLDS)
            {
                if (w.startsWith(prefix))
                {
                    out.add(w);
                }
            }
            return out;
        }
        return Collections.emptyList();
    }
}
