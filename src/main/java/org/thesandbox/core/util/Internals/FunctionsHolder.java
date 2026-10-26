package org.thesandbox.core.util.Internals;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Comparator;

public class FunctionsHolder {
    public Player resolveTarget(CommandSender sender, String input) {
        if (input.equalsIgnoreCase("@s")) {
            return (sender instanceof Player) ? (Player) sender : null;
        }

        if (input.equalsIgnoreCase("@p")) {
            if (!(sender instanceof Player senderPlayer)) return null; // console has no location to measure "nearest" from
            return senderPlayer.getWorld().getPlayers().stream()
                    .filter(p -> !p.equals(senderPlayer))
                    .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(senderPlayer.getLocation())))
                    .orElse(null);
        }
        return Bukkit.getPlayer(input);
    }
}
