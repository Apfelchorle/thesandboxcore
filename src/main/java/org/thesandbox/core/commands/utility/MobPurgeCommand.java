package org.thesandbox.core.commands.utility;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.commands.CommandMessages;
import org.thesandbox.core.util.colors.Colors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MobPurgeCommand extends BaseCommand
{
    private final JavaPlugin plugin;

    public MobPurgeCommand(JavaPlugin plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args)
    {
        // Optional: args[0] may be a specific mob type (e.g., ZOMBIE)
        EntityType targetType = null;
        String mobName = null;

        if (args.length > 0)
        {
            try
            {
                targetType = EntityType.valueOf(args[0].toUpperCase());
            }
            catch (IllegalArgumentException e)
            {
                sendError(sender, Colors.RED + args[0] + " is not a valid mob type.");
                return true;
            }

            // Validate that it's actually a mob (living & spawnable) and not players
            if (!isMobType(targetType))
            {
                sendError(sender, Colors.RED + formatName(targetType.name()) + " is an entity, but not a mob. /entitywipe is for entities.");
                return true;
            }

            mobName = formatName(targetType.name());
        }

        // Broadcast admin action in red
        String who = (sender instanceof Player) ? sender.getName() : "CONSOLE";
        Bukkit.getServer().broadcast(org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.SERVER, Colors.RED + who + " - Purging all "
                + (targetType != null ? mobName + "s" : "mobs")));

        // Do the purge
        int removed = 0;
        for (World world : Bukkit.getWorlds())
        {
            for (Entity e : world.getEntities())
            {
                if (!(e instanceof LivingEntity)) continue;         // only living things
                if (e instanceof Player) continue;                  // never touch players

                if (targetType == null)
                {
                    // remove all mobs
                    e.remove();
                    removed++;
                }
                else
                {
                    if (e.getType() == targetType)
                    {
                        e.remove();
                        removed++;
                    }
                }
            }
        }

        // Feedback to sender
        if (targetType != null)
        {
            sender.sendMessage(org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.COMMAND, Colors.GRAY + removed + " " + mobName
                    + (removed == 1 ? "" : "s") + " removed."));
        }
        else
        {
            sendCommand(sender, Colors.GRAY + removed + " mob" + (removed == 1 ? "" : "s") + " removed.");
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
            for (EntityType t : EntityType.values())
            {
                if (isMobType(t))
                {
                    String name = t.name();
                    if (name.toLowerCase().startsWith(prefix))
                    {
                        out.add(name);
                    }
                }
            }
            Collections.sort(out);
            return out;
        }
        return Collections.emptyList();
    }

    /* ---------------- helpers ---------------- */

    private boolean isMobType(EntityType type)
    {
        // Treat "mobs" as spawnable, living, non-player entities
        return type != null && type.isAlive() && type.isSpawnable() && type != EntityType.PLAYER;
    }

    private String formatName(String enumName)
    {
        String s = enumName.replace('_', ' ').toLowerCase();
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
