package org.thesandbox.core.commands.utility;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import de.myzelyam.api.vanish.VanishAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.data.PlayerDataListener;
import org.thesandbox.core.util.colors.Colors;

import java.util.*;

/**
 * Player utilities that work for both online and offline players.
 */
public class PlayerCommand extends BaseCommand {

    private static final List<String> SUBCOMMANDS = List.of("help", "list", "tp", "uuid");

    private final PlayerDataListener dataListener;

    public PlayerCommand(PlayerDataListener dataListener) {
        this.dataListener = dataListener;
    }

    /**
     * @param sender  the player or console running the command
     * @param command the command instance
     * @param label   the label that was used
     * @param args    subcommand and optional player argument
     * @return always true, so Bukkit does not append the default usage message
     *
     * <p>This command holds many utilities for players, online and offline.</p>
     */
    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help" -> sendHelp(sender, label);
            case "list" -> listPlayers(sender);
            case "tp" -> teleport(sender, label, args);
            case "uuid" -> showUuid(sender, label, args);
            default -> sendError(sender, "Unknown subcommand '" + args[0] + "'. Use /" + label + " help.");
        }
        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        send(sender, "&7&m--------------&r &dPlayer Utilities &7&m--------------");
        send(sender, "&d/" + label + " help &8- &7Show this help");
        send(sender, "&d/" + label + " list &8- &7List online players");
        send(sender, "&d/" + label + " tp <player> &8- &7Teleport to a player or their logout spot &8(staff)");
        send(sender, "&d/" + label + " uuid [player|uuid] &8- &7Show a player's UUID");
        send(sender, "&7&m-----------------------------------------------------");
    }

    private void listPlayers(CommandSender sender) {
        boolean viewerIsStaff = sender.hasPermission("sandbox.staff");

        List<Player> visible = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isVanished(player) && !viewerIsStaff) {
                continue;
            }
            visible.add(player);
        }
        visible.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));

        send(sender, "&7&m----------&r &dOnline Players (" + visible.size() + ") &7&m----------");
        if (visible.isEmpty()) {
            send(sender, "&7No players are online.");
        } else {
            List<String> names = new ArrayList<>(visible.size());
            for (Player player : visible) {
                names.add(isVanished(player)
                        ? "&f" + player.getName() + " &e(VANISHED)"
                        : "&f" + player.getName());
            }
            send(sender, String.join("&7, ", names));
        }
        send(sender, "&7&m-----------------------------------------------------");
    }

    /**
     * Teleports the sender to an online player, or to an offline player's last
     * logout location. Existing players may not have a stored location yet, so
     * Essentials' logout location is used as a fallback.
     */
    private void teleport(CommandSender sender, String label, String[] args) {
        if (!hasStaffPermission(sender)) {
            return;
        }

        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }

        if (args.length != 2) {
            sendUsage(sender, "/" + label + " tp <player>");
            return;
        }

        OfflinePlayer target = findPlayer(sender, args[1]);
        if (target == null) {
            sendError(sender, "Player '" + args[1] + "' was not found. They must have joined before.");
            return;
        }

        String targetName = target.getName() != null ? target.getName() : args[1];
        boolean online = false;
        Location destination = null;

        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null && onlineTarget.isOnline()) {
            destination = onlineTarget.getLocation();
            online = true;
        } else {
            destination = dataListener.getLastLocation(target.getUniqueId());
            if (destination == null) {
                destination = essentialsLogoutLocation(target);
            }
        }

        if (destination == null || destination.getWorld() == null) {
            sendError(sender, "No saved location for " + targetName + " (their world may not be loaded).");
            return;
        }

        if (!player.teleport(destination)) {
            sendError(sender, "Could not teleport to " + targetName + ".");
            return;
        }

        sendCommand(player, "&aTeleported to " + targetName
                + (online ? "&a." : "&a's last logout location."));
    }

    private void showUuid(CommandSender sender, String label, String[] args) {
        if (args.length > 2) {
            sendUsage(sender, "/" + label + " uuid [player|uuid]");
            return;
        }

        OfflinePlayer target;
        if (args.length == 2) {
            target = findPlayer(sender, args[1]);
            if (target == null) {
                sendError(sender, "Player '" + args[1] + "' was not found. They must have joined before.");
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sendUsage(sender, "/" + label + " uuid <player|uuid>");
            return;
        }

        String name = target.getName() != null ? target.getName() : "(unknown)";
        String status = target.isOnline() ? "&aonline" : "&7offline";
        send(sender, "&d" + name + " &8» &f" + target.getUniqueId() + " &8(" + status + "&8)");
    }

    private static void send(CommandSender sender, String message) {
        sender.sendMessage(Colors.color(message));
    }

    private boolean isVanished(Player player) {
        try {
            return VanishAPI.isInvisible(player);
        } catch (Throwable ignored) {
            // PremiumVanish is optional; treat unknown as visible.
            return false;
        }
    }

    /**
     * Resolves an online player, a cached offline player, a selector, or a raw UUID.
     */
    private OfflinePlayer findPlayer(CommandSender sender, String input) {
        OfflinePlayer resolved = resolveOfflineTarget(sender, input);
        if (resolved != null) {
            return resolved;
        }

        try {
            return Bukkit.getOfflinePlayer(UUID.fromString(input));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * Essentials stores its own logout location, which covers players who have
     * not quit since the last-data feature was added.
     */
    private Location essentialsLogoutLocation(OfflinePlayer target) {
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("Essentials");
            if (!(plugin instanceof Essentials essentials) || !plugin.isEnabled()) {
                return null;
            }
            User user = essentials.getUser(target.getUniqueId());
            return user != null ? user.getLogoutLocation() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) {
            return new ArrayList<>(SUBCOMMANDS);
        }

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return SUBCOMMANDS.stream()
                    .filter(sub -> sub.startsWith(prefix))
                    .toList();
        }

        if (args.length == 2
                && (args[0].equalsIgnoreCase("uuid") || args[0].equalsIgnoreCase("tp"))) {
            return offlinePlayerSelectorCompletions(args[1]);
        }

        return List.of();
    }
}
