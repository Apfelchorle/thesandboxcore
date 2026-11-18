package org.thesandbox.core.util.permissions;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

/**
 * Single source of truth for staff tiers.
 *
 * <p>{@code sandbox.mod < sandbox.staff < sandbox.admin < sandbox.operator <
 * sandbox.superuser}. Console is treated as {@link #SUPERUSER}. Player ranks
 * below moderator (VIP, Master Builder) intentionally map to {@link #DEFAULT}
 * for staff-visibility filtering.</p>
 *
 * <p>Use {@link #of(Player)} for players and {@link #of(CommandSender)} when a
 * console sender is possible.</p>
 */
public enum PermissionTier {
    DEFAULT(0),
    MOD(1),
    STAFF(2),
    ADMIN(3),
    OPERATOR(4),
    SUPERUSER(5);

    private final int weight;

    PermissionTier(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }

    /** True when this tier is the same as or above {@code other}. */
    public boolean atLeast(PermissionTier other) {
        return other != null && weight >= other.weight;
    }

    public static PermissionTier of(Player player) {
        if (player == null) return DEFAULT;
        if (player.hasPermission("sandbox.superuser")) return SUPERUSER;
        if (player.hasPermission("sandbox.operator")) return OPERATOR;
        if (player.hasPermission("sandbox.admin")) return ADMIN;
        if (player.hasPermission("sandbox.staff")) return STAFF;
        if (player.hasPermission("sandbox.mod")) return MOD;
        return DEFAULT;
    }

    /**
     * Console is above every player tier; a non-player sender that is not the
     * console (e.g. a command block) is treated as {@link #DEFAULT}.
     */
    public static PermissionTier of(CommandSender sender) {
        if (sender instanceof Player player) return of(player);
        if (sender instanceof ConsoleCommandSender) return SUPERUSER;
        return DEFAULT;
    }

    public static int levelOf(CommandSender sender) {
        return of(sender).weight;
    }
}
