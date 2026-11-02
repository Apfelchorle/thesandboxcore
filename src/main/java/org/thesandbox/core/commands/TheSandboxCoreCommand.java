package org.thesandbox.core.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.commands.trusted.ConfigCommand;
import org.thesandbox.core.data.PlayerDataListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Plugin info, reload, and the safe typed {@code /config} bridge.
 */
public class TheSandboxCoreCommand extends BaseCommand {

    private final TheSandboxCore plugin;
    private final ConfigCommand configCommand;

    public TheSandboxCoreCommand(TheSandboxCore plugin, PlayerDataListener playerDataListener) {
        this.plugin = plugin;
        this.configCommand = new ConfigCommand(plugin, playerDataListener);
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            send(sender, "&6&lTheSandboxCore &7v" + plugin.getPluginMeta().getVersion());
            send(sender, "&6&lCurrent Version: &7"
                    + plugin.getConfig().getString("update.tag-state.core", "Unknown"));
            send(sender, "&7Author: &f" + String.join(", ", plugin.getPluginMeta().getAuthors()));
            boolean discord = plugin.getConfig().getBoolean("discord.enabled", false);
            send(sender, "&7Discord Bridge: " + (discord ? "&aEnabled" : "&cDisabled"));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("config")) {
            if (!hasStaffPermission(sender)) {
                return true;
            }
            return configCommand.execute(
                    sender,
                    command,
                    label + " config",
                    Arrays.copyOfRange(args, 1, args.length)
            );
        }

        if (sub.equals("reload") && args.length == 1) {
            if (!hasAdminPermission(sender)) {
                return true;
            }
            long start = System.currentTimeMillis();
            plugin.reloadAndReconnect();
            MessageService.reload();
            long elapsed = System.currentTimeMillis() - start;

            sendSuccess(sender, MessageService.formatCommon(
                    "reloaded",
                    "TheSandboxCore reloaded in %s ms.",
                    elapsed
            ));
            plugin.getLogger().info("[TheSandboxCore] Reload triggered by "
                    + sender.getName() + " (" + elapsed + " ms)");
            return true;
        }

        sendUsage(sender, "/" + label + " [reload | config <get|set|list|keys> ...]");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("sandbox.staff")) {
            return List.of();
        }

        if (args.length == 1) {
            String input = args[0].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            if (sender.hasPermission("sandbox.admin") && "reload".startsWith(input)) {
                out.add("reload");
            }
            if ("config".startsWith(input)) {
                out.add("config");
            }
            return out;
        }

        if (args[0].equalsIgnoreCase("config")) {
            return configCommand.tabComplete(
                    sender,
                    command,
                    alias,
                    Arrays.copyOfRange(args, 1, args.length)
            );
        }

        return List.of();
    }
}
