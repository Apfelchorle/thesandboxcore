package org.thesandbox.core.commands.utility;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.thesandbox.core.commands.ISubCommand;

import java.util.List;

public class OverworldCommand implements ISubCommand {

    public OverworldCommand(JavaPlugin plugin) {
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        return WorldTeleportHelper.teleport(sender, "world", "Overworld");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return WorldTeleportHelper.noCompletions();
    }
}
