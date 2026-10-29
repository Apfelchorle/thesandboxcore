package org.thesandbox.core.commands.Admin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.thesandbox.core.commands.BaseCommand;

/**
 * Deprecated command that redirects to /updatelocal and /updateallproxies.
 */
public class UpdateCommand extends BaseCommand {

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        sendUsage(sender, "This command has been deprecated. Use /updatelocal or /updateallproxies instead.");
        Bukkit.dispatchCommand(sender, "updatelocal");
        return true;
    }
}