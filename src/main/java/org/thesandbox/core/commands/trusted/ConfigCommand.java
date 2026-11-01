package org.thesandbox.core.commands.trusted;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.data.PlayerDataKeys;
import org.thesandbox.core.data.PlayerDataListener;

import java.util.*;

public class ConfigCommand extends BaseCommand {

    private final TheSandboxCore plugin;
    private final PlayerDataListener playerDataListener;

    public ConfigCommand(TheSandboxCore plugin, PlayerDataListener playerDataListener) {
        this.plugin = plugin;
        this.playerDataListener = playerDataListener;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!hasStaffPermission(sender)) {
            return true;
        }

        // No arguments — show usage
        if (args.length == 0) {
            sendUsage(sender, "/" + label + " <get|set|list> [key] [value] [player]");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "get" -> handleGet(sender, label, args);
            case "set" -> handleSet(sender, label, args);
            case "list" -> handleList(sender, label, args);
            default -> sendUsage(sender, "/" + label + " <get|set|list> [key] [value] [player]");
        }

        return true;
    }

    private void handleGet(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sendUsage(sender, "/" + label + " get <key> [player]");
            return;
        }

        // Determine if targeting a player or global config
        boolean hasPlayer = args.length >= 3;
        String key = String.join(" ", Arrays.copyOfRange(args, 1, args.length - (hasPlayer ? 1 : 0)));
        String target = hasPlayer ? args[args.length - 1] : null;

        if (target != null) {
            // Player data get
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                OfflinePlayer offline = resolveOfflineTarget(sender, target);
                if (offline == null || offline.getUniqueId() == null) {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            sendError(sender, "Player not found: " + target));
                    return;
                }

                UUID uuid = offline.getUniqueId();
                Object value = playerDataListener.get(uuid, key, null);
                String result = value != null ? value.toString() : "(not set)";

                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(Component.text()
                                .append(Component.text("[Player] ", NamedTextColor.DARK_GREEN))
                                .append(Component.text(target, NamedTextColor.GOLD))
                                .append(Component.text(" ", NamedTextColor.DARK_GREEN))
                                .append(Component.text(key, NamedTextColor.AQUA))
                                .append(Component.text(" = ", NamedTextColor.DARK_GREEN))
                                .append(Component.text(result, NamedTextColor.WHITE))
                                .build()));
            });
        } else {
            // Global config get
            Object value = plugin.getConfig().get(key);
            String result = value != null ? value.toString() : "(not set)";
            sender.sendMessage(Component.text()
                    .append(Component.text("[Global] ", NamedTextColor.DARK_PURPLE))
                    .append(Component.text(key, NamedTextColor.AQUA))
                    .append(Component.text(" = ", NamedTextColor.DARK_PURPLE))
                    .append(Component.text(result, NamedTextColor.WHITE))
                    .build());
        }
    }

    private void handleSet(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            sendUsage(sender, "/" + label + " set <key> <value> [player]");
            return;
        }

        // Determine if targeting a player or global config
        boolean hasPlayer = args.length >= 4;
        String key = String.join(" ", Arrays.copyOfRange(args, 1, args.length - (hasPlayer ? 2 : 1)));
        String value = args[args.length - (hasPlayer ? 2 : 1)];
        String target = hasPlayer ? args[args.length - 1] : null;

        if (target != null) {
            // Player data set
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                OfflinePlayer offline = resolveOfflineTarget(sender, target);
                if (offline == null || offline.getUniqueId() == null) {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            sendError(sender, "Player not found: " + target));
                    return;
                }

                UUID uuid = offline.getUniqueId();
                Object typedValue = parseValue(value);
                playerDataListener.set(uuid, key, typedValue);

                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(Component.text()
                                .append(Component.text("Set ", NamedTextColor.GREEN))
                                .append(Component.text(key, NamedTextColor.AQUA))
                                .append(Component.text(" = ", NamedTextColor.GREEN))
                                .append(Component.text(value, NamedTextColor.WHITE))
                                .append(Component.text(" for ", NamedTextColor.GREEN))
                                .append(Component.text(target, NamedTextColor.GOLD))
                                .build()));
            });
        } else {
            // Global config set
            Object typedValue = parseValue(value);
            plugin.getConfig().set(key, typedValue);
            plugin.saveConfig();
            sender.sendMessage(Component.text()
                    .append(Component.text("[Global] Set ", NamedTextColor.GREEN))
                    .append(Component.text(key, NamedTextColor.AQUA))
                    .append(Component.text(" = ", NamedTextColor.GREEN))
                    .append(Component.text(value, NamedTextColor.WHITE))
                    .build());
        }
    }

    private void handleList(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sendUsage(sender, "/" + label + " list [player|global]");
            return;
        }

        String target = args[1].toLowerCase(Locale.ROOT);

        if (target.equals("global")) {
            // List all global config keys
            ConfigurationSection section = plugin.getConfig().getConfigurationSection("");
            if (section == null) {
                sendError(sender, "No global config found.");
                return;
            }

            sender.sendMessage(Component.text("[Global Config Keys]:", NamedTextColor.DARK_PURPLE));
            listConfigKeys(sender, section, "");
        } else {
            // List player data
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                OfflinePlayer offline = resolveOfflineTarget(sender, target);
                if (offline == null || offline.getUniqueId() == null) {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            sendError(sender, "Player not found: " + target));
                    return;
                }

                UUID uuid = offline.getUniqueId();
                // Load all data for the player
                for (String key : PlayerDataKeys.getAllKeys()) {
                    Object value = playerDataListener.get(uuid, key, null);
                    String result = value != null ? value.toString() : "(not set)";

                    Bukkit.getScheduler().runTask(plugin, () ->
                            sender.sendMessage(Component.text()
                                    .append(Component.text(key, NamedTextColor.AQUA))
                                    .append(Component.text(" = ", NamedTextColor.GRAY))
                                    .append(Component.text(result, NamedTextColor.WHITE))
                                    .build()));
                }
            });
        }
    }

    private void listConfigKeys(CommandSender sender, ConfigurationSection section, String path) {
        for (String key : section.getKeys(false)) {
            String fullPath = path.isEmpty() ? key : path + "." + key;
            Object value = section.get(key);
            if (value instanceof ConfigurationSection) {
                listConfigKeys(sender, (ConfigurationSection) value, fullPath);
            } else {
                String result = value != null ? value.toString() : "(null)";
                sender.sendMessage(Component.text()
                        .append(Component.text(fullPath, NamedTextColor.AQUA))
                        .append(Component.text(" = ", NamedTextColor.GRAY))
                        .append(Component.text(result, NamedTextColor.WHITE))
                        .build());
            }
        }
    }

    /**
     * Attempt to parse a string value into its appropriate type.
     */
    private Object parseValue(String value) {
        // Try boolean
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;

        // Try integer
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
        }

        // Try double
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
        }

        // Return as string
        return value;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("sandbox.staff")) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();

        // First argument: subcommands
        if (args.length == 1) {
            String input = args[0].toLowerCase(Locale.ROOT);
            if ("get".startsWith(input)) completions.add("get");
            if ("set".startsWith(input)) completions.add("set");
            if ("list".startsWith(input)) completions.add("list");
            return completions;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        // Second argument: keys or list targets
        if (args.length == 2) {
            if (sub.equals("list")) {
                String input = args[1].toLowerCase(Locale.ROOT);
                if ("global".startsWith(input)) completions.add("global");
                // Add offline player names
                completions.addAll(offlinePlayerCompletions(args[1]));
                return completions;
            }

            // For get/set: suggest PlayerDataKeys
            String input = args[1].toLowerCase(Locale.ROOT);
            for (String key : PlayerDataKeys.getAllKeys()) {
                if (key.toLowerCase(Locale.ROOT).startsWith(input)) {
                    completions.add(key);
                }
            }
            // Also suggest global config top-level keys
            ConfigurationSection section = plugin.getConfig().getConfigurationSection("");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    if (key.toLowerCase(Locale.ROOT).startsWith(input) && !completions.contains(key)) {
                        completions.add(key);
                    }
                }
            }
            return completions;
        }

        // Third argument for set: value (no completion) or player for get
        if (args.length == 3) {
            if (sub.equals("get")) {
                // Player target
                return offlinePlayerCompletions(args[2]);
            }
            // For set, this is the value — no completion
            return Collections.emptyList();
        }

        // Fourth argument for set: player target
        if (args.length == 4 && sub.equals("set")) {
            return offlinePlayerCompletions(args[3]);
        }

        return Collections.emptyList();
    }
}
