package org.thesandbox.core.commands.utility;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.util.commands.CommandGuard;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * /loop <amount> <command...>
 * The amount is capped (config: loop.max-iterations, default 1000) because the loop runs
 * synchronously on the main thread – an unbounded value would trip the server watchdog.
 */
public class LoopCommand extends BaseCommand
{
    private static final int DEFAULT_MAX = 1000;
    private static final long COOLDOWN_MS = 2000L;

    private final TheSandboxCore plugin;
    private final Map<UUID, Long> lastUse = new HashMap<>();

    public LoopCommand(TheSandboxCore plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args)
    {
        if (!sender.hasPermission("sandbox.superuser"))
        {
            sendError(sender, "Only superusers can use /" + label + ".");
            return true;
        }

        if (args.length < 2)
        {
            sendUsage(sender, "Usage: /" + label + " <amount> <command>");
            return true;
        }

        final int max = Math.max(1, plugin.getConfig().getInt("loop.max-iterations", DEFAULT_MAX));
        final int amount;
        try
        {
            amount = Integer.parseInt(args[0]);
        }
        catch (NumberFormatException ex)
        {
            sendError(sender, "Invalid amount: " + args[0]);
            return true;
        }
        if (amount < 1)
        {
            sendError(sender, "Amount must be a positive integer.");
            return true;
        }
        if (amount > max)
        {
            sendError(sender, "Amount is too high (max " + max + ").");
            return true;
        }

        String toRun = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
        while (toRun.startsWith("/"))
        {
            toRun = toRun.substring(1);
        }
        if (toRun.isEmpty())
        {
            sendError(sender, "Please provide a command to run.");
            return true;
        }

        // Recursion and wrapper bypasses are rejected by the shared guard.
        String blocked = CommandGuard.blockedReason(toRun, List.of(), false);
        if (blocked != null)
        {
            sendError(sender, "Refusing to loop a protected command (/" + blocked + ").");
            return true;
        }

        if (sender instanceof Player player && isOnCooldown(player))
        {
            sendError(sender, "Please wait a moment before using /" + label + " again.");
            return true;
        }

        sendCommand(sender, "Looping " + amount + "x: /" + toRun);

        int failed = 0;
        for (int i = 0; i < amount; i++)
        {
            // Keep firing even if one run fails; just count it.
            if (!Bukkit.dispatchCommand(sender, toRun)) failed++;
        }

        sendCommand(sender, "Finished looping" + (failed > 0 ? " (" + failed + " run(s) reported failure)." : "."));
        return true;
    }

    private boolean isOnCooldown(Player player)
    {
        long now = System.currentTimeMillis();
        Long last = lastUse.get(player.getUniqueId());
        if (last != null && now - last < COOLDOWN_MS)
        {
            return true;
        }
        lastUse.put(player.getUniqueId(), now);
        return false;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        if (args.length == 1)
        {
            return Arrays.asList("1", "5", "10", "100");
        }
        return Collections.emptyList();
    }
}
