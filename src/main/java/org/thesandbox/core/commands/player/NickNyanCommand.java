package org.thesandbox.core.commands.player;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.commands.CommandMessages;
import org.thesandbox.core.util.colors.Colors;

import java.util.List;
import java.util.Random;

public class NickNyanCommand extends BaseCommand {
    // Only actual color codes (no formatting like BOLD/UNDERLINE/MAGIC/RESET)
    private static final String[] COLOR_POOL = new String[]{
            Colors.BLACK, Colors.DARK_BLUE, Colors.DARK_GREEN, Colors.DARK_AQUA,
            Colors.DARK_RED, Colors.DARK_PURPLE, Colors.GOLD, Colors.GRAY,
            Colors.DARK_GRAY, Colors.BLUE, Colors.GREEN, Colors.AQUA,
            Colors.RED, Colors.LIGHT_PURPLE, Colors.YELLOW, Colors.WHITE
    };
    private static final int MIN_VISIBLE = 3;
    private static final int MAX_VISIBLE = 30;
    private final Essentials essentials;
    private final Random rng = new Random();

    public NickNyanCommand(JavaPlugin plugin) {
        Essentials ess = null;
        try {
            ess = (Essentials) plugin.getServer().getPluginManager().getPlugin("Essentials");
        } catch (Throwable ignored) {
        }
        this.essentials = ess;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            return true;
        }
        if (essentials == null || !essentials.isEnabled()) {
            sender.sendMessage(CommandMessages.error(Colors.RED + "Essentials is not enabled on this server."));
            return true;
        }

        User user = essentials.getUser(player);
        if (user == null) {
            sender.sendMessage(CommandMessages.error(Colors.RED + "Could not access your Essentials data."));
            return true;
        }

        // /nicknyan off -> clear nickname
        if (args.length >= 1 && args[0].equalsIgnoreCase("off")) {
            user.setNickname(null);
            sender.sendMessage(CommandMessages.command(Colors.YELLOW + "Nickname cleared."));
            return true;
        }

        // Determine base text:
        // - If arg provided, use it.
        // - Else if player has a nickname, nyanify that nickname's VISIBLE Colors.
        // - Else use the player's username (and set one).
        final String baseInput;
        if (args.length >= 1) {
            baseInput = args[0].trim();
        } else {
            String currentNick = user.getNickname();
            if (currentNick != null && !Colors.strip(currentNick).isEmpty()) {
                baseInput = Colors.strip(currentNick).trim();
            } else {
                baseInput = player.getName(); // no nick -> use username
            }
        }

        // Allow &-codes in input (only matters when user supplied an arg).
        String coloredAttempt = Colors.color(baseInput);
        String visible = Colors.strip(coloredAttempt);
        if (visible == null) visible = "";
        visible = visible.trim();

        // Validate allowed characters and visible length
        if (!visible.matches("^[a-zA-Z0-9_]+$")) {
            sender.sendMessage(CommandMessages.error(Colors.RED + "That nickname contains invalid characters. Allowed: A-Z, a-z, 0-9, _"));
            return true;
        }
        int vlen = visible.length();
        if (vlen < MIN_VISIBLE || vlen > MAX_VISIBLE) {
            sender.sendMessage(CommandMessages.error(Colors.RED + "Your nickname must be between " + MIN_VISIBLE + " and " + MAX_VISIBLE + " visible characters."));
            return true;
        }

        // Uniqueness check against usernames, display names (stripped), and Essentials nicks
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p == player) continue;
            String otherDisplay = Colors.strip(Colors.plain(p.displayName()));
            String otherNick = null;
            try {
                User otherUser = essentials.getUser(p);
                if (otherUser != null) otherNick = otherUser.getNickname();
            } catch (Throwable ignored) {
            }
            if (p.getName().equalsIgnoreCase(visible)
                    || (otherDisplay != null && otherDisplay.trim().equalsIgnoreCase(visible))
                    || (otherNick != null && Colors.strip(otherNick).trim().equalsIgnoreCase(visible))) {
                sender.sendMessage(CommandMessages.error(Colors.RED + "That nickname is already in use."));
                return true;
            }
        }

        // Build randomized-color nickname per codepoint
        String nyanNick = nyanifyVisible(visible);
        String finalNick = nyanNick + Colors.WHITE; // reset to avoid bleed

        user.setNickname(finalNick);
        sender.sendMessage(CommandMessages.command(Colors.YELLOW + "Your nickname is now: " + finalNick));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("off");
        }
        return List.of();
    }

    // ===== Helpers =====

    /**
     * Random color per codepoint of the provided VISIBLE (no colors) Colors.
     */
    private String nyanifyVisible(String text) {
        StringBuilder out = new StringBuilder();
        int[] cps = text.codePoints().toArray();
        for (int cp : cps) {
            String cc = COLOR_POOL[rng.nextInt(COLOR_POOL.length)];
            out.append(cc).append(new String(Character.toChars(cp)));
        }
        return out.toString();
    }
}
