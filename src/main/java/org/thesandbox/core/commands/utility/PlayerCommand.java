package org.thesandbox.core.commands.utility;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import de.myzelyam.api.vanish.VanishAPI;
import litebans.api.Database;
import litebans.api.Entry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.data.PlayerDataListener;
import org.thesandbox.core.services.login.LoginService;
import org.thesandbox.core.util.colors.ComponentBuilder;

import java.util.*;

/**
 * Player utilities that work for both online and offline players.
 */
public class PlayerCommand extends BaseCommand {

    private static final List<String> SUBCOMMANDS = List.of("help", "info", "list", "tp", "uuid");

    private final PlayerDataListener dataListener;
    private final LoginService loginService;

    public PlayerCommand(PlayerDataListener dataListener, LoginService loginService) {
        this.dataListener = dataListener;
        this.loginService = loginService;
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
            case "info" -> info(sender, label, args);
            case "list" -> listPlayers(sender);
            case "tp" -> teleport(sender, label, args);
            case "uuid" -> showUuid(sender, label, args);
            default -> sendError(sender, "Unknown subcommand '" + args[0] + "'. Use /" + label + " help.");
        }
        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        send(sender, "&8&m--------------&r &6Player Utilities &8&m--------------");
        send(sender, "&e/" + label + " help &8- &7Show this help");
        send(sender, "&e/" + label + " info <player> &8- &7Show detailed player info &8(staff)");
        send(sender, "&e/" + label + " list &8- &7List online players with details");
        send(sender, "&e/" + label + " tp <player> &8- &7Teleport to a player or their logout spot &8(staff)");
        send(sender, "&e/" + label + " uuid [player|uuid] &8- &7Show a player's UUID (click to copy)");
        send(sender, "&8&m-----------------------------------------------------");
    }

    /**
     * Shows detailed player info with hover-to-reveal for sensitive data.
     * IP is censored in the visible text but revealed on hover.
     */
    private void info(CommandSender sender, String label, String[] args) {
        if (!hasStaffPermission(sender)) {
            return;
        }

        if (args.length != 2) {
            sendUsage(sender, "/" + label + " info <player>");
            return;
        }

        OfflinePlayer target = findPlayer(sender, args[1]);
        if (target == null) {
            sendError(sender, "Player '" + args[1] + "' was not found. They must have joined before.");
            return;
        }

        String targetName = target.getName() != null ? target.getName() : "(unknown)";
        UUID uuid = target.getUniqueId();
        boolean isOnline = target.isOnline();
        Player onlinePlayer = isOnline ? target.getPlayer() : null;

        // Build the info component
        ComponentBuilder builder = ComponentBuilder.empty()
                .append(ComponentBuilder.legacy("&8&m--------------&r &6" + targetName + " &8&m--------------")).newline();

        // Name & status
        String statusText = isOnline ? "&aOnline" : "&cOffline";
        builder.append(ComponentBuilder.legacy("&7Name: ")).append(ComponentBuilder.legacy(targetName)).newline();
        builder.append(ComponentBuilder.legacy("&7Status: ")).append(ComponentBuilder.legacy(statusText)).newline();

        // UUID (click to copy)
        Component uuidComp = ComponentBuilder.text(uuid.toString())
                .gray()
                .copyToClipboard(uuid.toString())
                .hoverText("Click to copy UUID")
                .build();
        builder.append(ComponentBuilder.legacy("&7UUID: ")).append(uuidComp).newline();

        // Rank - use saved rank for offline players
        String savedRank = dataListener.getLastRank(uuid);
        String rankDisplay;
        String rankColor;
        if (isOnline && onlinePlayer != null) {
            LoginService.Rank rank = loginService.getRank(onlinePlayer);
            rankDisplay = rank != null ? rank.displayName : "Player";
            rankColor = rank != null ? rank.color : "&7";
        } else if (savedRank != null && !savedRank.isEmpty()) {
            LoginService.Rank rank = LoginService.Rank.valueOf(savedRank);
            rankDisplay = rank.displayName;
            rankColor = rank.color;
        } else {
            rankDisplay = "Player";
            rankColor = "&7";
        }
        builder.append(ComponentBuilder.legacy("&7Rank: ")).append(ComponentBuilder.legacy(rankColor + rankDisplay)).newline();

        // Ban/Mute status via LiteBans
        appendLiteBansStatus(builder, uuid);

        // IP address (censored, shown on hover for staff only)
        String ip = dataListener.getLastIp(uuid);
        if (ip != null && !ip.isEmpty()) {
            String censoredIp = censorIp(ip);
            Component ipComp = ComponentBuilder.text(censoredIp)
                    .gray()
                    .hoverText("&7IP: " + ip)
                    .build();
            builder.append(ComponentBuilder.legacy("&7IP: ")).append(ipComp).newline();
        } else {
            builder.append(ComponentBuilder.legacy("&7IP: &7No data")).newline();
        }

        // Last logout location
        Location lastLoc = dataListener.getLastLocation(uuid);
        if (lastLoc != null) {
            String locStr = String.format("&7%s (%.1f, %.1f, %.1f)",
                    lastLoc.getWorld().getName(), lastLoc.getX(), lastLoc.getY(), lastLoc.getZ());
            builder.append(ComponentBuilder.legacy("&7Last Logout: ")).append(ComponentBuilder.legacy(locStr)).newline();
        } else {
            builder.append(ComponentBuilder.legacy("&7Last Logout: &7No data")).newline();
        }

        // Online-specific info
        if (isOnline && onlinePlayer != null) {
            builder.append(ComponentBuilder.legacy("&7World: ")).append(ComponentBuilder.legacy(onlinePlayer.getWorld().getName())).newline();
            builder.append(ComponentBuilder.legacy("&7Health: "))
                    .append(ComponentBuilder.legacy(String.format("%.1f/%.1f", onlinePlayer.getHealth(), getMaxHealth(onlinePlayer)))).newline();
            builder.append(ComponentBuilder.legacy("&7Game Mode: ")).append(ComponentBuilder.legacy(onlinePlayer.getGameMode().name())).newline();
            builder.append(ComponentBuilder.legacy("&7Ping: ")).append(ComponentBuilder.legacy(onlinePlayer.getPing() + "ms")).newline();
        }

        builder.append(ComponentBuilder.legacy("&8&m-----------------------------------------------------"));
        builder.sendTo(sender);
    }

    /**
     * Appends LiteBans ban/mute status to the player info component.
     * Shows active punishments with reason and duration revealed on hover.
     */
    private void appendLiteBansStatus(ComponentBuilder builder, UUID uuid) {
        if (!Bukkit.getPluginManager().isPluginEnabled("LiteBans")) {
            builder.append(ComponentBuilder.legacy("&7Status: &7LiteBans not found")).newline();
            return;
        }

        try {
            Database db = Database.get();
            if (db == null) {
                builder.append(ComponentBuilder.legacy("&7Status: &7LiteBans API unavailable")).newline();
                return;
            }

            String scope = Database.ANY_SERVER_SCOPE;
            boolean isBanned = db.isPlayerBanned(uuid, scope);
            boolean isMuted = db.isPlayerMuted(uuid, scope);

            if (!isBanned && !isMuted) {
                builder.append(ComponentBuilder.legacy("&7Status: &aNo active punishments")).newline();
                return;
            }

            // Build hover text with punishment details
            ComponentBuilder hover = ComponentBuilder.empty();
            boolean first = true;

            if (isBanned) {
                Entry ban = db.getBan(uuid, null, scope);
                if (ban != null) {
                    hover.append(ComponentBuilder.legacy("&cBANNED")).newline();
                    hover.append(ComponentBuilder.legacy("&7Reason: ")).append(ComponentBuilder.legacy(ban.getReason())).newline();
                    hover.append(ComponentBuilder.legacy("&7By: ")).append(ComponentBuilder.legacy(ban.getExecutorName())).newline();
                    if (ban.isPermanent()) {
                        hover.append(ComponentBuilder.legacy("&7Duration: &cPermanent"));
                    } else {
                        hover.append(ComponentBuilder.legacy("&7Duration: ")).append(ComponentBuilder.legacy(ban.getDurationString()));
                        hover.newline().append(ComponentBuilder.legacy("&7Remaining: ")).append(ComponentBuilder.legacy(ban.getRemainingDurationString(System.currentTimeMillis() / 1000)));
                    }
                    first = false;
                }
            }

            if (isMuted) {
                if (!first) hover.newline().newline();
                Entry mute = db.getMute(uuid, null, scope);
                if (mute != null) {
                    hover.append(ComponentBuilder.legacy("&9MUTED")).newline();
                    hover.append(ComponentBuilder.legacy("&7Reason: ")).append(ComponentBuilder.legacy(mute.getReason())).newline();
                    hover.append(ComponentBuilder.legacy("&7By: ")).append(ComponentBuilder.legacy(mute.getExecutorName()));
                    if (mute.isPermanent()) {
                        hover.append(ComponentBuilder.legacy("&7Duration: &9Permanent"));
                    } else {
                        hover.append(ComponentBuilder.legacy("&7Duration: ")).append(ComponentBuilder.legacy(mute.getDurationString()));
                        hover.newline().append(ComponentBuilder.legacy("&7Remaining: ")).append(ComponentBuilder.legacy(mute.getRemainingDurationString(System.currentTimeMillis() / 1000)));
                    }
                }
            }

            // Build visible status text
            String visibleStatus;
            if (isBanned && isMuted) {
                visibleStatus = "&cBANNED &9MUTED";
            } else if (isBanned) {
                visibleStatus = "&cBANNED";
            } else {
                visibleStatus = "&9MUTED";
            }

            Component statusComp = ComponentBuilder.text(visibleStatus)
                    .hoverComponent(hover.build())
                    .build();
            builder.append(ComponentBuilder.legacy("&7Status: ")).append(statusComp).newline();

        } catch (Exception e) {
            Bukkit.getLogger().warning("[TheSandboxCore] Failed to query LiteBans for " + uuid + ": " + e.getMessage());
            builder.append(ComponentBuilder.legacy("&7Status: &7LiteBans error")).newline();
        }
    }

    /**
     * Gets the max health of a player using the non-deprecated Attribute API.
     */
    private static double getMaxHealth(Player player) {
        return player.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
    }

    /**
     * Censors an IP address, showing only the first and last octet.
     * e.g. "192.168.1.100" -> "192.*.*.100"
     */
    private static String censorIp(String ip) {
        if (ip == null || ip.isEmpty()) return "*.*.*.*";
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return "*.*.*.*";
        return parts[0] + ".*.*." + parts[3];
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

        send(sender, "&8&m----------&r &6Online Players (" + visible.size() + ") &8&m----------");
        if (visible.isEmpty()) {
            send(sender, "&7No players are online.");
        } else {
            for (Player player : visible) {
                ComponentBuilder builder = ComponentBuilder.empty();

                // Build hover content with detailed info
                ComponentBuilder hover = ComponentBuilder.empty()
                        .append(ComponentBuilder.legacy("&e" + player.getName())).newline()
                        .append(ComponentBuilder.legacy("&7UUID: ")).append(ComponentBuilder.legacy(player.getUniqueId().toString())).newline()
                        .append(ComponentBuilder.legacy("&7Rank: ")).append(ComponentBuilder.legacy(getRankDisplay(player))).newline()
                        .append(ComponentBuilder.legacy("&7World: ")).append(ComponentBuilder.legacy(player.getWorld().getName())).newline()
                        .append(ComponentBuilder.legacy("&7Health: ")).append(ComponentBuilder.legacy(String.format("%.1f/%.1f", player.getHealth(), getMaxHealth(player)))).newline()
                        .append(ComponentBuilder.legacy("&7Game Mode: ")).append(ComponentBuilder.legacy(player.getGameMode().name()));

                if (viewerIsStaff) {
                    String ip = dataListener.getLastIp(player.getUniqueId());
                    if (ip != null && !ip.isEmpty()) {
                        hover.newline().append(ComponentBuilder.legacy("&7IP: ")).append(ComponentBuilder.legacy(ip));
                    }
                }

                // Build visible component
                String vanishedTag = isVanished(player) ? " &e(VANISHED)" : "";
                LoginService.Rank rank = loginService.getRank(player);
                String rankColor = rank != null ? rank.color : "&7";

                builder.append(ComponentBuilder.legacy(rankColor + player.getName() + vanishedTag))
                        .hoverComponent(hover.build());

                // Click to copy UUID
                builder.copyToClipboard(player.getUniqueId().toString());

                builder.newline().sendTo(sender);
            }
        }
        send(sender, "&7&m-----------------------------------------------------");
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
        UUID uuid = target.getUniqueId();

        // Build a clickable component: shows name » UUID, click UUID to copy
        ComponentBuilder builder = ComponentBuilder.empty()
                .append(ComponentBuilder.legacy("&6" + name + " &8» "))
                .append(ComponentBuilder.text(uuid.toString())
                        .color(NamedTextColor.WHITE)
                        .copyToClipboard(uuid.toString())
                        .hoverText("Click to copy UUID")
                        .build())
                .append(ComponentBuilder.legacy(" &8(" + status + "&8)"));

        builder.sendTo(sender);
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

    /**
     * Gets the rank display string for a player.
     */
    private String getRankDisplay(Player player) {
        LoginService.Rank rank = loginService.getRank(player);
        return rank != null ? rank.color + rank.displayName : "&7Player";
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
                && (args[0].equalsIgnoreCase("uuid") || args[0].equalsIgnoreCase("tp") || args[0].equalsIgnoreCase("info"))) {
            return offlinePlayerSelectorCompletions(args[1]);
        }

        return List.of();
    }
}
