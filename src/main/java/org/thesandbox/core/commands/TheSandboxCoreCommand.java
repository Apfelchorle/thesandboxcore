package org.thesandbox.core.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.thesandbox.core.TheSandboxCore;

import java.util.List;

public class TheSandboxCoreCommand implements ISubCommand {

    private final TheSandboxCore plugin;

    public TheSandboxCoreCommand(TheSandboxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        // /thesandboxcore → info
        // /thesandboxcore reload → reload (sandbox.admin)
        if (args.length == 0) {
            sender.sendMessage(CommandMessages.command(color("&6&lTheSandboxCore &7v" + plugin.getDescription().getVersion())));
            sender.sendMessage(CommandMessages.command(color("&7Author: &f" + String.join(", ", plugin.getDescription().getAuthors()))));
            boolean discord = plugin.getConfig().getBoolean("discord.enabled", false);
            sender.sendMessage(CommandMessages.command(color("&7Discord Bridge: &f" + (discord ? "&aEnabled" : "&cDisabled"))));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("sandbox.admin")) {
                sender.sendMessage(CommandMessages.error(ChatColor.RED + "You don’t have permission."));
                return true;
            }
            long t0 = System.currentTimeMillis();
            plugin.reloadAndReconnect();
            long dt = System.currentTimeMillis() - t0;
            sender.sendMessage(CommandMessages.command(ChatColor.GREEN + "TheSandboxCore reloaded in " + dt + " ms."));
            Bukkit.getLogger().info("[TheSandboxCore] Reload triggered by " + sender.getName() + " (" + dt + " ms)");
            return true;
        }

        sender.sendMessage(CommandMessages.usage(ChatColor.RED + "Usage: /" + label + " [reload]"));
        return true;
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && "reload".startsWith(args[0].toLowerCase()) && sender.hasPermission("sandbox.admin")) {
            return List.of("reload");
        }
        return List.of();
    }
}
