package org.thesandbox.core.services.login;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.thesandbox.core.util.colors.Colors;
import org.thesandbox.core.util.permissions.PermissionTier;

import java.util.Locale;
import java.util.UUID;

public class LoginService {

    private final JavaPlugin plugin;

    public LoginService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Already stored pretty-case; return as-is for display.
     */
    public static String displayRankTitleCase(Rank rank) {
        return (rank == null ? Rank.DEFAULT : rank).displayName;
    }

    /**
     * Very simple heuristic for chat usage.
     */
    public static String articleFor(String nextWord) {
        if (nextWord == null || nextWord.isEmpty()) return "a";
        char c = Character.toLowerCase(nextWord.trim().charAt(0));
        return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u'
                ? "an"
                : "a";
    }

    /**
     * Title-case + underscores→spaces; kept for legacy callers.
     */
    public static String toTitleCaseWords(String s) {
        if (s == null || s.isEmpty()) return s;
        String[] parts = s
                .replace('_', ' ')
                .toLowerCase(Locale.ENGLISH)
                .split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) sb.append(part.substring(1));
            sb.append(' ');
        }
        return sb.toString().trim();
    }

    // ===== Rank resolution & convenience =====

    /**
     * Colorize legacy '&' codes to section sign.
     */
    public static String colorize(String s) {
        return s == null
                ? null
                : Colors.color(s);
    }

    /**
     * Kept for the existing plugin lifecycle. No database setup is needed anymore.
     */
    public void init() {
        // Custom login messages were removed. Rank lookup is permission-based only.
    }

    /**
     * Kept for the existing plugin lifecycle.
     */
    public void shutdown() {
        // No resources to close.
    }

    public Rank getRank(UUID uuid) {
        if (uuid == null) return Rank.DEFAULT;
        OfflinePlayer op = plugin.getServer().getOfflinePlayer(uuid);
        return getRank(op);
    }

    public Rank getRank(OfflinePlayer p) {
        if (p instanceof Player) return getRank((Player) p);
        return Rank.DEFAULT;
    }

    /**
     * Highest → lowest permissions check.
     */
    public Rank getRank(Player p) {
        return Rank.fromPermissions(p);
    }

    public String getDisplayNameFor(Player p) {
        return getRank(p).displayName;
    }

    public String getColorFor(Player p) {
        return getRank(p).color;
    }

    /**
     * Restored helpers used elsewhere in your codebase.
     */
    public boolean isDefault(Player p) {
        return getRank(p) == Rank.DEFAULT;
    }

    public boolean isDefault(OfflinePlayer p) {
        return getRank(p) == Rank.DEFAULT;
    }

    // ===== Ranks =====
    public enum Rank {
        DEFAULT("Player", "&7"),
        VIP("VIP", "&5"),
        MB("Master Builder", "&3"),
        STAFF("Staff", "&6"),
        ADMIN("Admin", "&c"),
        OPERATOR("Operator", "&4");

        /**
         * Pretty display name (e.g., "Master Builder").
         */
        public final String display; // kept for backward compat
        public final String displayName; // primary field used elsewhere
        /**
         * Legacy color using '&' codes (e.g., "&c").
         */
        public final String color;

        Rank(String display, String color) {
            this.display = display;
            this.displayName = display;
            this.color = color;
        }

        /**
         * Single permission-to-rank mapping used everywhere (login messages,
         * Discord prefixes, player data). Highest rank wins.
         */
        public static Rank fromPermissions(Player player) {
            if (player == null) return DEFAULT;

            PermissionTier tier = PermissionTier.of(player);
            if (tier.atLeast(PermissionTier.OPERATOR)) return OPERATOR;
            if (tier == PermissionTier.ADMIN) return ADMIN;
            if (tier == PermissionTier.STAFF) return STAFF;
            if (player.hasPermission("sandbox.mb")) return MB;
            if (player.hasPermission("sandbox.vip")) return VIP;
            return DEFAULT;
        }
    }
}
