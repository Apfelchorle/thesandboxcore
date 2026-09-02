package org.thesandbox.core.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.util.PlayerDataListener;

import java.util.ArrayList;
import java.util.Arrays;
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
            sender.sendMessage(color("&6&lTheSandboxCore &7v" + plugin.getPluginMeta().getVersion()));
            sender.sendMessage(color("&7Author: &f" + String.join(", ", plugin.getPluginMeta().getAuthors())));
            boolean discord = plugin.getConfig().getBoolean("discord.enabled", false);
            sender.sendMessage(color("&7Discord Bridge: &f" + (discord ? "&aEnabled" : "&cDisabled")));
            return true;
        }

        // /thesandboxcore config get [key with spaces...] [player]
        if (args.length >= 4 && args[0].equalsIgnoreCase("config")) {
            if (!sender.hasPermission("sandbox.admin")) {
                sender.sendMessage(Component.text("You don’t have permission.", NamedTextColor.RED));
                return true;
            }

            String subcommand = args[1];
            if (subcommand.equalsIgnoreCase("get")) {
                String playerName = args[args.length - 1];
                String dataKey = String.join(" ", Arrays.copyOfRange(args, 2, args.length - 1));

                // Performance fix: Run database/offline lookup asynchronously
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
                    UUID uuid = offlinePlayer.getUniqueId();

                    Object dataResult = playerDataListener.get(uuid, dataKey, "not found");
                    String response = dataResult.toString();

                    // Construct message using non-deprecated Components API
                    Component message = Component.text()
                            .append(Component.text(playerName, NamedTextColor.GOLD))
                            .append(Component.text(" (" + dataKey + "): ", NamedTextColor.GOLD))
                            .append(Component.text(response, NamedTextColor.WHITE))
                            .build();

                    // Sync back to sender safely
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(message));
                });
                return true;
            }
        }

        // /thesandboxcore reload
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("sandbox.admin")) {
                sender.sendMessage(Component.text("You don’t have permission.", NamedTextColor.RED));
                return true;
            }
            long t0 = System.currentTimeMillis();
            plugin.reloadAndReconnect();
            long dt = System.currentTimeMillis() - t0;

            sender.sendMessage(Component.text("TheSandboxCore reloaded in " + dt + " ms.", NamedTextColor.GREEN));
            plugin.getLogger().info("[TheSandboxCore] Reload triggered by " + sender.getName() + " (" + dt + " ms)");
            return true;
        }

        sender.sendMessage(Component.text("Usage: /" + label + " [reload | config get <key> <player>]", NamedTextColor.RED));
        return true;
    }

    // Modern color conversion using Adventure legacy serializer
    private Component color(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
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

        if (args.length >= 4 && args[0].equalsIgnoreCase("config") && args[1].equalsIgnoreCase("get")) {
            String input = args[args.length - 1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(OfflinePlayer::getName)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
