package org.thesandbox.core.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.util.GenericDataKeys;

import java.util.*;

/**
 * Base class for commands that provides shared utilities:
 * - Centralized permission checking
 * - Player resolution with @s and @p support
 * - Consistent messaging via CommandMessages
 * - Tab completion helpers
 */
public abstract class BaseCommand implements ISubCommand {

    protected boolean hasStaffPermission(CommandSender sender) {
        return hasPermission(sender, "sandbox.staff");
    }

    /**
     * Check if the sender has the required permission.
     * Sends an error message via CommandMessages if permission is denied.
     */
    protected boolean hasPermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        sendError(sender, GenericDataKeys.CMD_PERM_ERROR);
        return false;
    }

    protected void sendError(CommandSender sender, String message) {
        sender.sendMessage(CommandMessages.error(message));
    }

    protected boolean hasAdminPermission(CommandSender sender) {
        return hasPermission(sender, "sandbox.admin");
    }

    protected boolean hasSuperuserPermission(CommandSender sender) {
        return hasPermission(sender, "sandbox.superuser");
    }

    protected void sendCommand(CommandSender sender, String message) {
        sender.sendMessage(CommandMessages.command(message));
    }

    protected void sendUsage(CommandSender sender, String usage) {
        sender.sendMessage(CommandMessages.usage(usage));
    }

    protected void broadcastServer(String message) {
        Bukkit.broadcastMessage(CommandMessages.server(message));
    }

    /**
     * Check if the sender is a Player. Sends an error if not.
     */
    protected boolean requirePlayer(CommandSender sender) {
        if (sender instanceof Player) {
            return true;
        }
        sendError(sender, "This command can only be used by players.");
        return false;
    }

    /**
     * Cast sender to Player, or send error and return null.
     */
    protected Player asPlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        sendError(sender, "This command can only be used by players.");
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
            sender.sendMessage(CommandMessages.error(input + " is not a valid number."));
            return null;
        }
    }

    /**
     * Tab complete with online player names.
     */
    protected List<String> playerCompletions(String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> names = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase(Locale.ROOT).startsWith(lower)) {
                names.add(p.getName());
            }
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    /**
     * Tab complete with online player names and @s/@p selectors.
     */
    protected List<String> playerSelectorCompletions(String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> names = new ArrayList<>();
        names.add("@s");
        names.add("@p");
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase(Locale.ROOT).startsWith(lower)) {
                names.add(p.getName());
            }
        }
        names.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(lower));
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
