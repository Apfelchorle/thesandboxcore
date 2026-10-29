package org.thesandbox.core.commands.Admin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.listeners.GenericListener;
import org.thesandbox.core.util.GenericDataKeys;
import org.thesandbox.core.util.PluginConfigManager;

import java.util.List;
import java.util.Locale;

/**
 * /waterflow [on|off] – toggles (or sets) whether water and lava can flow.
 * The state is persisted in config and applied live through {@link GenericListener}.
 */
public class WaterflowCommand extends BaseCommand {

    private final PluginConfigManager configManager;
    private final GenericListener listener;

    public WaterflowCommand(PluginConfigManager configManager, GenericListener listener) {
        this.configManager = configManager;
        this.listener = listener;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!hasStaffPermission(sender)) {
            return true;
        }

        boolean current = listener.isWaterFlowEnabled();
        boolean newValue = !current;

        if (args.length > 0) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "on", "true", "enable" -> newValue = true;
                case "off", "false", "disable" -> newValue = false;
                default -> {
                    sendUsage(sender, "Usage: /" + label + " [on|off]");
                    return true;
                }
            }
        }

        configManager.forceset(GenericDataKeys.WATER_FLOW, newValue);
        listener.setWaterFlowEnabled(newValue);

        broadcastServer(sender.getName() + " - Water & lava flow " + (newValue ? "enabled" : "disabled"));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("on", "off").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                .toList();
        }
        return List.of();
    }
}
