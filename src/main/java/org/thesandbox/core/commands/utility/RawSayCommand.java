package org.thesandbox.core.commands.utility;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.commands.CommandMessages;
import org.thesandbox.core.util.colors.Colors;

import java.util.Collections;
import java.util.List;

public class RawSayCommand extends BaseCommand
{
    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args)
    {
        if (args.length == 0)
        {
            sendUsage(sender, Colors.RED + "Usage: /" + label + " <message>");
            return true;
        }

        // Support & color codes
        String message = Colors.color(String.join(" ", args));

        // Broadcast to all players (console will also see it in server log)
        Bukkit.getServer().broadcast(org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.SERVER, message));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        // No special completions for a free-form message
        return Collections.emptyList();
    }
}
