package org.thesandbox.core.commands.Admin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.commands.CommandMessages;
import org.thesandbox.core.commands.ISubCommand;

import java.util.*;

/**
 * /wildcard <command...>  – runs a command once for every online player.
 * Use '?' as the player-name placeholder; if absent the name is appended.
 *
 * Note: the blocklist (wildcard.blocked-commands) only inspects the first token of the command.
 * Wrapper commands such as "execute ... run <blocked>" are not detected.
 */
public class WildcardCommand implements ISubCommand
{
    private final TheSandboxCore core;

    public WildcardCommand(TheSandboxCore core)
    {
        this.core = core;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args)
    {
        if (!(sender instanceof Player))
        {
            sender.sendMessage(CommandMessages.error("This command cannot be used from console."));
            return true;
        }

        if (args.length == 0)
        {
            sender.sendMessage(CommandMessages.usage("Usage: /" + label + " <command...> [use '?' as the player placeholder]"));
            return true;
        }

        // Base command name: strip leading slash AND any "namespace:" prefix so
        // "minecraft:op" can't be used to dodge the blocklist.
        String baseName = args[0];
        if (baseName.startsWith("/")) baseName = baseName.substring(1);
        int colon = baseName.indexOf(':');
        if (colon >= 0) baseName = baseName.substring(colon + 1);
        if (baseName.isEmpty())
        {
            sender.sendMessage(CommandMessages.error("Please provide a command to run."));
            return true;
        }

        Set<String> names = resolveNames(baseName);

        // Never allow wildcard to run itself (would multiply by player count per level).
        if (names.contains("wildcard") || names.contains(command.getName().toLowerCase(Locale.ENGLISH))
                || names.contains(label.toLowerCase(Locale.ENGLISH)))
        {
            sender.sendMessage(CommandMessages.error("Refusing to run /" + label + " from itself."));
            return true;
        }

        // Blocked commands unless sandbox.superuser
        if (!sender.hasPermission("sandbox.superuser"))
        {
            for (String b : core.getConfig().getStringList("wildcard.blocked-commands"))
            {
                if (b == null || b.isEmpty()) continue;
                String bb = b.replaceFirst("^/", "").toLowerCase(Locale.ENGLISH);
                int bc = bb.indexOf(':');
                if (bc >= 0) bb = bb.substring(bc + 1);
                if (names.contains(bb))
                {
                    sender.sendMessage(CommandMessages.error("I applaud you for trying."));
                    return true;
                }
            }
        }

        String template = String.join(" ", args);
        if (template.startsWith("/")) template = template.substring(1);
        boolean hasPlaceholder = template.indexOf('?') >= 0;

        // Snapshot: commands like /kick modify the live online-player collection.
        List<Player> targets = new ArrayList<>(core.getServer().getOnlinePlayers());
        int ran = 0;
        for (Player target : targets)
        {
            if (!target.isOnline()) continue;
            String run = hasPlaceholder
                    ? template.replace("?", target.getName())
                    : template + " " + target.getName();

            sender.sendMessage(CommandMessages.command("Running: /" + run));
            Bukkit.dispatchCommand(sender, run);
            ran++;
        }

        sender.sendMessage(CommandMessages.command("Ran the command for " + ran + " player(s)."));
        return true;
    }

    /** The typed name plus the real command name and all aliases (from the server command map). */
    private static Set<String> resolveNames(String baseName)
    {
        Set<String> names = new HashSet<>();
        names.add(baseName.toLowerCase(Locale.ENGLISH));

        Command resolved = Bukkit.getCommandMap().getCommand(baseName);
        if (resolved != null)
        {
            names.add(resolved.getName().toLowerCase(Locale.ENGLISH));
            for (String a : resolved.getAliases())
            {
                if (a != null && !a.isEmpty()) names.add(a.toLowerCase(Locale.ENGLISH));
            }
        }
        return names;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        if (args.length == 1)
        {
            return Collections.singletonList("<command>");
        }
        return Collections.emptyList();
    }
}
