package org.thesandbox.core.commands.staff;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.util.colors.Colors;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class CommandSpyCommand extends BaseCommand {
    private final TheSandboxCore plugin;

    public CommandSpyCommand(TheSandboxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Colors.color("&c&lError &8» &cOnly players can use this command."));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("toggle")) {
            boolean currentlyEnabled = !plugin.getCommandSpyManager().isCmdSpyDisabled(player.getUniqueId());
            boolean newState = !currentlyEnabled;

            plugin.getCommandSpyManager().setCmdSpy(player, newState);

            player.sendMessage(Colors.color("&7&lCommand &8» &7CommandSpy has been "
                    + (newState ? "enabled" : "disabled")
                    + "&7."));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "on": {
                plugin.getCommandSpyManager().setCmdSpy(player, false);

                player.sendMessage(Colors.color("&7&lCommand &8» &7CommandSpy has been enabled."));
                return true;
            }

            case "off": {
                plugin.getCommandSpyManager().setCmdSpy(player, false);

                player.sendMessage(Colors.color("&7&lCommand &8» &7CommandSpy has been &cdisabled."));
                return true;
            }

            case "status": {
                boolean enabled = !plugin.getCommandSpyManager().isCmdSpyDisabled(player.getUniqueId());

                player.sendMessage(Colors.color("&7&lCommand &8» &7CommandSpy is currently "
                        + (enabled ? "enabled" : "disabled")
                        + "&7."));
                return true;
            }

            default: {
                player.sendMessage(Colors.color("&c&lUsage &8» &c/" + label + " [on|off|toggle|status]"));
                return true;
            }
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("on", "off", "toggle", "status");
        }

        return Collections.emptyList();
    }
}
