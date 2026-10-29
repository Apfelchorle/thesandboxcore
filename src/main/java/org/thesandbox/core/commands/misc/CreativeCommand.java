package org.thesandbox.core.commands.misc;

import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.thesandbox.core.commands.GameModeChangeHelper;
import org.thesandbox.core.commands.ISubCommand;

import java.util.List;

public class CreativeCommand implements ISubCommand {

    public CreativeCommand(JavaPlugin plugin) {
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        return GameModeChangeHelper.execute(sender, args, GameMode.CREATIVE);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return GameModeChangeHelper.tabComplete(sender, args);
    }
}
