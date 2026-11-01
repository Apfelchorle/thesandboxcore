package org.thesandbox.core.commands.misc;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.util.colors.Colors;

import java.util.ArrayList;
import java.util.List;

public class ExpelCommand extends BaseCommand {

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        Player playerSender = asPlayer(sender);
        if (playerSender == null) {
            return true;
        }

        // Defaults
        double radius = 20.0;
        double strength = 5.0;

        // Parse radius [1.0 .. 100.0]
        if (args.length >= 1) {
            try {
                radius = Math.max(1.0, Math.min(100.0, Double.parseDouble(args[0])));
            } catch (NumberFormatException ignored) {
            }
        }

        // Parse strength [0.0 .. 50.0]
        if (args.length >= 2) {
            try {
                strength = Math.max(0.0, Math.min(50.0, Double.parseDouble(args[1])));
            } catch (NumberFormatException ignored) {
            }
        }

        List<String> pushedPlayers = new ArrayList<>();
        final Vector senderPos = playerSender.getLocation().toVector();
        final World world = playerSender.getWorld();

        for (Player target : world.getPlayers()) {
            if (target.equals(playerSender)) {
                continue;
            }

            final Location targetLoc = target.getLocation();
            final Vector targetVec = targetLoc.toVector();

            boolean inRange = false;
            try {
                inRange = targetVec.distanceSquared(senderPos) <= (radius * radius);
            } catch (IllegalArgumentException ignored) {
            }

            if (!inRange) {
                continue;
            }

            // Harmless "pop" explosion (no fire, no block damage)
            try {
                world.createExplosion(targetLoc, 0.0f, false, false);
            } catch (Throwable ignored) {
                try {
                    world.createExplosion(targetLoc, 0.0f, false);
                } catch (Throwable ignored2) {
                    try {
                        world.createExplosion(targetLoc, 0.0f);
                    } catch (Throwable ignored3) {
                    }
                }
            }

            // Ensure not flying
            target.setFlying(false);
            target.setAllowFlight(false);

            // Compute push vector away from sender
            Vector delta = targetVec.clone().subtract(senderPos);
            if (delta.lengthSquared() < 1.0e-6) {
                delta = new Vector(0, 0.1, 0);
            }
            Vector velocity = delta.normalize().multiply(strength);
            target.setVelocity(velocity);

            pushedPlayers.add(target.getName());
        }

        if (pushedPlayers.isEmpty()) {
            sendError(playerSender, "No players pushed.");
        } else {
            playerSender.sendMessage(Colors.component(Colors.GRAY + "Successfully pushed " + pushedPlayers.size()
                    + " players: " + String.join(", ", pushedPlayers)));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
