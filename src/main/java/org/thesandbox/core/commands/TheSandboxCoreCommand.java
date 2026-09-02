package org.thesandbox.core.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.util.PlayerDataListener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TheSandboxCoreCommand implements ISubCommand {

    private final TheSandboxCore plugin;
    private final PlayerDataListener playerDataListener;

    public TheSandboxCoreCommand(TheSandboxCore plugin, PlayerDataListener playerDataListener) {
        this.plugin = plugin;
        this.playerDataListener = playerDataListener;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        // /thesandboxcore -> info
        if (args.length == 0) {
            sender.sendMessage(CommandMessages.command(color("&6&lTheSandboxCore &7v" + plugin.getDescription().getVersion())));
            sender.sendMessage(CommandMessages.command(color("&7Author: &f" + String.join(", ", plugin.getDescription().getAuthors()))));
            boolean discord = plugin.getConfig().getBoolean("discord.enabled", false);
            sender.sendMessage(CommandMessages.command(color("&7Discord Bridge: &f" + (discord ? "&aEnabled" : "&cDisabled"))));
            return true;
        }

        // /thesandboxcore config get [key] [player]
        if (args.length == 4 && args[0].equalsIgnoreCase("config")) {
            if (!sender.hasPermission("sandbox.admin")) {
                sender.sendMessage(CommandMessages.error(ChatColor.RED + "You don’t have permission."));
                return true;
            }

            String subcommand = args[1];
            if (subcommand.equalsIgnoreCase("get")) {
                String dataKey = args[2];
                String playerName = args[3];

                // Performance fix: Run database/offline lookup asynchronously
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    @SuppressWarnings("deprecation")
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
                    UUID uuid = offlinePlayer.getUniqueId();

                    Object dataResult = playerDataListener.get(uuid, dataKey, "not found");
                    String response = dataResult != null ? dataResult.toString() : "null";

                    // Sync back to sender safely
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(ChatColor.GOLD + playerName + " (" + dataKey + "): " + ChatColor.WHITE + response));
                });
                return true;
            }
        }

        // /thesandboxcore reload
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
        sender.sendMessage(CommandMessages.usage(ChatColor.RED + "Usage: /" + label + " [reload | config get <key> <player>]"));
        return true;
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("sandbox.admin")) {
            return List.of();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            if ("reload".startsWith(input)) completions.add("reload");
            if ("config".startsWith(input)) completions.add("config");
            return completions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("config")) {
            if ("get".startsWith(args[1].toLowerCase())) completions.add("get");
            return completions;
        }

        // args.length == 3 is the data key.

        if (args.length == 4 && args[0].equalsIgnoreCase("config") && args[1].equalsIgnoreCase("get")) {
            String input = args[3].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(OfflinePlayer::getName)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
