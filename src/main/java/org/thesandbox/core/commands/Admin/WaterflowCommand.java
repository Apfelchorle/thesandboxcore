package org.thesandbox.core.commands.Admin;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.util.GenericDataKeys;
import org.thesandbox.core.util.PluginConfigManager;

public class WaterflowCommand extends BaseCommand {

    private final PluginConfigManager configManager;

    public WaterflowCommand(PluginConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!hasStaffPermission(sender)) {
            return true;
        }

        boolean current = configManager.getOrCreate(GenericDataKeys.WATER_FLOW, GenericDataKeys.WATER_FLOW_DEFAULT);
        boolean newValue = !current;
        configManager.forceset(GenericDataKeys.WATER_FLOW, newValue);

        announce(sender, newValue);
        return true;
    }

    private void announce(CommandSender sender, boolean current) {
        Component message = Component.text("waterFlow is now: " + current);
        Bukkit.broadcast(message);
    }
}
