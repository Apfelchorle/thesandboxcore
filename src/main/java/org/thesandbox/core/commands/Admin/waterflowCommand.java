package org.thesandbox.core.commands.Admin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.thesandbox.core.commands.ISubCommand;

import java.util.List;

public class waterflowCommand implements ISubCommand {
    /// @param sender  player who sent command
    /// @param command command
    /// @param label   command label
    /// @param args    command arguments
    /// @return Boolean
    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage("not implemented ;)");
        return false;
    }

    /// @param sender  player who sent command
    /// @param command command
    /// @param alias   command aliases
    /// @param args    arguments
    /// @return List for autocomplete
    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
