package org.thesandbox.core.util.Internals;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Comparator;

public class FunctionsHolder {
    public static Player resolveTarget(CommandSender sender, String input) {
        if (input == null || input.isBlank()) return null;
        if (input.equalsIgnoreCase("@s")) {
            return (sender instanceof Player) ? (Player) sender : null;
        }

        if (input.equalsIgnoreCase("@p")) {
            if (!(sender instanceof Player senderPlayer)) return null;
            return senderPlayer.getWorld().getPlayers().stream()
                    .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(senderPlayer.getLocation())))
                    .orElse(senderPlayer);
        }
        return Bukkit.getPlayer(input);
    }

    /**
     * @deprecated Use {@link #parseIntSafe(String)} instead, which returns null on error
     * instead of silently returning 0.
     */
    @Deprecated
    public static int parseint(String input) {
        Integer result = parseIntSafe(input);
        return result != null ? result : 0;
    }

    /**
     * Parse an integer from a string.
     *
     * @return the parsed integer, or null if invalid
     */
    public static Integer parseIntSafe(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
