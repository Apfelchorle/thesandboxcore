package org.thesandbox.core.commands.utility;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.commands.CommandCenter;
import org.thesandbox.core.login.LoginService;
import org.thesandbox.core.services.EntityWiper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * /entitywipe            – remove clutter (items, arrows, boats, minecarts, ...) in every world
 * /entitywipe <entity>   – remove every entity of that type
 * /entitywipe -a         – remove EVERY non-player entity (admin+), skipping vehicles that carry players
 *
 * Previously running it with no arguments wiped everything and "-a" (documented in plugin.yml) was rejected
 * as an invalid entity type.
 */
public class EntityWipeCommand extends BaseCommand
{
    private final EntityWiper wiper;

    public EntityWipeCommand(EntityWiper wiper)
    {
        this.wiper = wiper;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args)
    {
        final String who = (sender instanceof Player) ? sender.getName() : "CONSOLE";

        // /entitywipe -> clutter only
        if (args.length == 0)
        {
            broadcastServer(who + " - Wiping clutter entities");
            int removed = wiper.wipeEntities(true);
            sendCommand(sender, removed + (removed == 1 ? " entity" : " entities") + " removed.");
            return true;
        }

        // /entitywipe -a -> everything (non-player)
        if (args[0].equalsIgnoreCase("-a"))
        {
            if (!CommandCenter.request(command, sender, LoginService.Rank.ADMIN))
            {
                return true;
            }
            broadcastServer(who + " - Wiping ALL entities");
            int removed = wipeAllEntities();
            sendCommand(sender, removed + (removed == 1 ? " entity" : " entities") + " removed.");
            return true;
        }

        // /entitywipe <type>
        EntityType targetType;
        try
        {
            targetType = EntityType.valueOf(args[0].toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException ex)
        {
            sendError(sender, args[0] + " is not a valid entity type.");
            return true;
        }
        if (targetType == EntityType.PLAYER)
        {
            sendError(sender, "Player entities cannot be wiped.");
            return true;
        }
        if (targetType == EntityType.UNKNOWN)
        {
            sendError(sender, "That entity type cannot be wiped.");
            return true;
        }

        String pretty = prettify(targetType.name());
        broadcastServer(who + " - Wiping all " + pretty + "s");
        int removed = wipeEntitiesOfType(targetType);
        sendCommand(sender, removed + " " + pretty + (removed == 1 ? "" : "s") + " removed.");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        if (args.length == 1)
        {
            String prefix = args[0].toUpperCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            if ("-A".startsWith(prefix)) out.add("-a");
            for (EntityType t : EntityType.values())
            {
                if (t == EntityType.PLAYER || t == EntityType.UNKNOWN) continue;
                if (t.name().startsWith(prefix)) out.add(t.name());
            }
            Collections.sort(out);
            return out;
        }
        return Collections.emptyList();
    }

    /* ===================== helpers ===================== */

    private static boolean carriesPlayer(Entity e)
    {
        for (Entity passenger : e.getPassengers())
        {
            if (passenger instanceof Player) return true;
        }
        return false;
    }

    private int wipeAllEntities()
    {
        int removed = 0;
        for (World w : Bukkit.getWorlds())
        {
            for (Entity e : w.getEntities())
            {
                if (e instanceof Player || carriesPlayer(e)) continue;
                e.remove();
                removed++;
            }
        }
        return removed;
    }

    private int wipeEntitiesOfType(EntityType type)
    {
        int removed = 0;
        for (World w : Bukkit.getWorlds())
        {
            for (Entity e : w.getEntities())
            {
                if (e.getType() != type || e instanceof Player || carriesPlayer(e)) continue;
                e.remove();
                removed++;
            }
        }
        return removed;
    }

    private static String prettify(String enumName)
    {
        StringBuilder sb = new StringBuilder();
        for (String p : enumName.toLowerCase(Locale.ROOT).split("_"))
        {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }
}
