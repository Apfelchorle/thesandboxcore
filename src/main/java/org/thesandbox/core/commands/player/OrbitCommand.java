package org.thesandbox.core.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.commands.CommandMessages;
import org.thesandbox.core.util.colors.Colors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class OrbitCommand extends BaseCommand {
    // Match TFM-style behavior: constant upward strength; no lateral motion
    private static final double STRENGTH = 10.0;
    private static final long TICK_INTERVAL = 2L;
    private final TheSandboxCore plugin;
    // Track active orbits: target UUID -> repeating task
    private final Map<UUID, BukkitTask> orbitTasks = new HashMap<>();

    public OrbitCommand(TheSandboxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!hasStaffPermission(sender)) {
            return true;
        }

        if (args.length < 1) {
            sendUsage(sender, "Usage: /" + label + " <player>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            List<Player> matches = Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.getName().toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
            if (matches.size() == 1) {
                target = matches.get(0);
            }
        }

        if (target == null) {
            sendError(sender, "Player not found.");
            return true;
        }

        UUID tid = target.getUniqueId();
        // sender.getName() can be null for console
        String actor = sender.getName() != null ? sender.getName() : "Console";

        // Toggle OFF if already orbiting
        if (orbitTasks.containsKey(tid)) {
            stopOrbit(target);
            if (!sender.equals(target)) {
                target.sendMessage(CommandMessages.command(Colors.YELLOW + "You are now descending back into the world."));
                target.setGameMode(GameMode.CREATIVE);
                target.sendMessage(CommandMessages.command(Colors.GRAY + "Your gamemode was updated to CREATIVE."));
            }
            Colors.broadcast(CommandMessages.server(Colors.AQUA + actor + " - Stopped orbiting " + target.getName()));
            return true;
        }

        // Toggle ON
        target.setGameMode(GameMode.SURVIVAL);
        target.sendMessage(CommandMessages.command(Colors.GRAY + "Your gamemode was updated to SURVIVAL."));
        startOrbit(target);

        Colors.broadcast(CommandMessages.server(Colors.AQUA + actor + " - Orbiting " + target.getName()));

        if (!sender.equals(target)) {
            target.sendMessage(CommandMessages.command(Colors.GOLD + "BLAST OFF!"));
        }
        return true;
    }

    private void stopOrbit(Player p) {
        UUID id = p.getUniqueId();
        BukkitTask t = orbitTasks.remove(id);
        if (t != null) t.cancel();
        p.setVelocity(new Vector(0, 0, 0));
        p.setFallDistance(0.0f);
    }

    private void startOrbit(Player p) {
        final UUID id = p.getUniqueId();
        p.setVelocity(new Vector(0, STRENGTH, 0));
        p.setFallDistance(0.0f);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!p.isOnline() || p.isDead()) {
                stopOrbit(p);
                return;
            }
            p.setVelocity(new Vector(0, STRENGTH, 0));
            p.setFallDistance(0.0f);
        }, TICK_INTERVAL, TICK_INTERVAL);

        orbitTasks.put(id, task);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            final String prefix = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(prefix))
                    .sorted()
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
