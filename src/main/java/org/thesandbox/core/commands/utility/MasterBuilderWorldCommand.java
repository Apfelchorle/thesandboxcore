package org.thesandbox.core.commands.utility;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.thesandbox.core.commands.ISubCommand;

import java.util.List;

public class MasterBuilderWorldCommand implements ISubCommand {

    public MasterBuilderWorldCommand(JavaPlugin plugin) {
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        return WorldTeleportHelper.teleport(sender, "masterbuilderworld", "Masterbuilderworld");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return WorldTeleportHelper.noCompletions();
    }
}
