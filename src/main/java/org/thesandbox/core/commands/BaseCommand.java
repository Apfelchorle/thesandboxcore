package org.thesandbox.core.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.data.GenericDataKeys;

import java.util.*;

/**
 * Base class for commands that provides shared utilities:
 * - Centralized permission checking
 * - Player resolution with @s and @p support
 * - Consistent messaging via MessageService
 * - Tab completion helpers
 */
public abstract class BaseCommand implements ISubCommand {

    protected boolean hasStaffPermission(CommandSender sender) {
        return hasPermission(sender, "sandbox.staff");
    }

    /**
     * Check if the sender has the required permission.
     * Sends an error message via MessageService if permission is denied.
     */
    protected boolean hasPermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        sendError(sender, MessageService.common(
                "no-permission",
                GenericDataKeys.CMD_PERM_ERROR
        ));
        return false;
    }

    protected void sendError(CommandSender sender, String message) {
        sender.sendMessage(MessageService.component(MessageService.Prefix.ERROR, message));
    }

    /**
     * Sends a component with one of the themed prefixes.
     */
    protected void sendPrefixed(CommandSender sender, MessageService.Prefix prefix, String message) {
        sender.sendMessage(MessageService.component(prefix, message));
    }

    /**
     * Sends a pre-built component without adding a prefix.
     */
    protected void sendComponent(CommandSender sender, Component component) {
        sender.sendMessage(component);
    }

    protected boolean hasAdminPermission(CommandSender sender) {
        return hasPermission(sender, "sandbox.admin");
    }

    protected boolean hasSuperuserPermission(CommandSender sender) {
        return hasPermission(sender, "sandbox.superuser");
    }

    protected void sendCommand(CommandSender sender, String message) {
        sender.sendMessage(MessageService.component(MessageService.Prefix.COMMAND, message));
    }

    protected void sendSuccess(CommandSender sender, String message) {
        sender.sendMessage(MessageService.component(MessageService.Prefix.SUCCESS, message));
    }

    protected void sendInformation(CommandSender sender, String message) {
        sender.sendMessage(MessageService.component(MessageService.Prefix.INFO, message));
    }

    /**
     * Send a plain message with legacy color codes resolved.
     * Use for general-purpose output that doesn't need a prefix.
     */
    protected void send(CommandSender sender, String message) {
        sender.sendMessage(org.thesandbox.core.util.colors.Colors.component(message));
    }

    protected void sendUsage(CommandSender sender, String usage) {
        sender.sendMessage(MessageService.component(
                MessageService.Prefix.USAGE,
                CommandMessages.usageBody(usage)
        ));
    }

    protected void broadcastServer(String message) {
        Bukkit.broadcast(MessageService.component(MessageService.Prefix.SERVER, message));
    }

    /**
     * Check if the sender is a Player. Sends an error if not.
     */
    protected boolean requirePlayer(CommandSender sender) {
        if (sender instanceof Player) {
            return true;
        }
        sendError(sender, MessageService.common(
                "player-only",
                "This command can only be used by players."
        ));
        return false;
    }

    /**
     * Cast sender to Player, or send error and return null.
     */
    protected Player asPlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        sendError(sender, MessageService.common(
                "player-only",
                "This command can only be used by players."
        ));
        return null;
    }

    /**
     * Resolve a target player by name, @s (self), or @p (nearest).
     */
    protected Player resolveTarget(CommandSender sender, String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        String lower = input.toLowerCase(Locale.ROOT);
        if (lower.equals("@s")) {
            return sender instanceof Player ? (Player) sender : null;
        }

        if (lower.equals("@p")) {
            if (!(sender instanceof Player senderPlayer)) {
                return null;
            }
            return senderPlayer.getWorld().getPlayers().stream()
                    .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(senderPlayer.getLocation())))
                    .orElse(senderPlayer);
        }

        return Bukkit.getPlayer(input);
    }

    /**
     * Parse an integer from a string. Returns null and sends an error message if invalid.
     */
    protected Integer parseInt(CommandSender sender, String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageService.component(
                    MessageService.Prefix.ERROR,
                    String.format(
                            MessageService.common("invalid-number", "%s is not a valid number."),
                            input
                    )
            ));
            return null;
        }
    }

    /**
     * Resolve a target that may be offline, by name, @s (self), or @p (nearest).
     * Returns null if the name has never been seen on this server.
     */
    protected OfflinePlayer resolveOfflineTarget(CommandSender sender, String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        Player online = resolveTarget(sender, input);
        if (online != null) {
            return online;
        }

        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(input);
        return cached != null && cached.getName() != null ? cached : null;
    }

    /**
     * Tab complete with online player names.
     */
    protected List<String> playerCompletions(String prefix) {
        return playerCompletions(prefix, false, false);
    }

    /**
     * Tab complete with online and offline player names.
     */
    protected List<String> offlinePlayerCompletions(String prefix) {
        return playerCompletions(prefix, true, false);
    }

    /**
     * Tab complete with online player names and @s/@p selectors.
     */
    protected List<String> playerSelectorCompletions(String prefix) {
        return playerCompletions(prefix, false, true);
    }

    /**
     * Tab complete with online and offline player names plus @s/@p selectors.
     */
    protected List<String> offlinePlayerSelectorCompletions(String prefix) {
        return playerCompletions(prefix, true, true);
    }

    private List<String> playerCompletions(String prefix, boolean includeOffline, boolean includeSelectors) {
        String lower = prefix.toLowerCase(Locale.ROOT);

        // Case-insensitive set keeps duplicate names out while sorting as we go.
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (includeSelectors) {
            addName(names, "@s", lower);
            addName(names, "@p", lower);
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            addName(names, p.getName(), lower);
        }
        if (includeOffline) {
            for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
                addName(names, p.getName(), lower);
            }
        }
        return new ArrayList<>(names);
    }

    private void addName(Set<String> names, String name, String lowerPrefix) {
        if (name != null && name.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
            names.add(name);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
