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

    public static int parseint(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
