package org.thesandbox.core.commands.utility;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.commands.CommandMessages;
import org.thesandbox.core.commands.ISubCommand;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * /loop <amount> <command...>
 * The amount is capped (config: loop.max-iterations, default 1000) because the loop runs
 * synchronously on the main thread – an unbounded value would trip the server watchdog.
 */
public class LoopCommand implements ISubCommand
{
    private static final int DEFAULT_MAX = 1000;

    private final TheSandboxCore plugin;

    public LoopCommand(TheSandboxCore plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args)
    {
        if (!sender.hasPermission("sandbox.superuser"))
        {
            sender.sendMessage(CommandMessages.error("Only superusers can use /" + label + "."));
            return true;
        }

        if (args.length < 2)
        {
            sender.sendMessage(CommandMessages.usage("Usage: /" + label + " <amount> <command>"));
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
            sender.sendMessage(CommandMessages.error("Invalid amount: " + args[0]));
            return true;
        }
        if (amount < 1)
        {
            sender.sendMessage(CommandMessages.error("Amount must be a positive integer."));
            return true;
        }
        if (amount > max)
        {
            sender.sendMessage(CommandMessages.error("Amount is too high (max " + max + ")."));
            return true;
        }

        String toRun = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
        while (toRun.startsWith("/"))
        {
            toRun = toRun.substring(1);
        }
        if (toRun.isEmpty())
        {
            sender.sendMessage(CommandMessages.error("Please provide a command to run."));
            return true;
        }

        // Recursion guard (also covers namespaced forms like "thesandboxcore:loop").
        String base = toRun.split("\\s+", 2)[0].toLowerCase(Locale.ENGLISH);
        int colon = base.indexOf(':');
        if (colon >= 0) base = base.substring(colon + 1);
        if (base.equals(label.toLowerCase(Locale.ENGLISH)) || base.equals(command.getName().toLowerCase(Locale.ENGLISH)) || base.equals("loop"))
        {
            sender.sendMessage(CommandMessages.error("Refusing to loop the /loop command."));
            return true;
        }

        sender.sendMessage(CommandMessages.command("Looping " + amount + "x: /" + toRun));

        int failed = 0;
        for (int i = 0; i < amount; i++)
        {
            // Keep firing even if one run fails; just count it.
            if (!Bukkit.dispatchCommand(sender, toRun)) failed++;
        }

        sender.sendMessage(CommandMessages.command("Finished looping" + (failed > 0 ? " (" + failed + " run(s) reported failure)." : ".")));
        return true;
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
