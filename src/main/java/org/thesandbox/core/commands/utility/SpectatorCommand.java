package org.thesandbox.core.commands.utility;

import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.thesandbox.core.commands.GameModeChangeHelper;
import org.thesandbox.core.commands.ISubCommand;

import java.util.List;

public class SpectatorCommand implements ISubCommand {

    public SpectatorCommand(JavaPlugin plugin) {
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        return GameModeChangeHelper.execute(sender, args, GameMode.SPECTATOR);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return GameModeChangeHelper.tabComplete(sender, args);
    }
}
