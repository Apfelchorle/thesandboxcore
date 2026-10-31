package org.thesandbox.core.commands.player;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.thesandbox.core.commands.CommandMessages;
import org.thesandbox.core.commands.ISubCommand;
import org.thesandbox.core.util.colors.Colors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class NickCleanCommand implements ISubCommand {
    // Blocked chat codes: &0 (black), &k (obfuscated), &m (strikethrough), &n (underline)
    private static final Pattern BLOCKED = Pattern.compile("(?i)[§&][0kmn]");
    private final JavaPlugin plugin;
    private final Essentials essentials;

    public NickCleanCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        Essentials ess = null;
        try {
            ess = (Essentials) plugin.getServer().getPluginManager().getPlugin("Essentials");
        } catch (Throwable ignored) {
        }
        this.essentials = ess;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (essentials == null || !essentials.isEnabled()) {
            sender.sendMessage(CommandMessages.error(Colors.RED + "Essentials is not available; cannot clean nicknames."));
            return true;
        }

        final String who = (sender instanceof Player) ? sender.getName() : "CONSOLE";

        if (args.length >= 1) {
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(CommandMessages.error(Colors.RED + "Player not found: " + args[0]));
                return true;
            }

            Colors.broadcast(CommandMessages.server(Colors.RED + who + " - Cleaning " + target.getName() + "'s nickname"));
            cleanOne(sender, target);
            return true;
        }

        // No args: clean everyone
        Colors.broadcast(CommandMessages.server(Colors.RED + who + " - Cleaning all nicknames"));
        int changed = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (cleanOne(sender, p)) changed++;
        }
        sender.sendMessage(CommandMessages.command(Colors.GRAY + "Finished cleaning nicknames. " + changed + " change" + (changed == 1 ? "" : "s") + " applied."));
        return true;
    }

    private boolean cleanOne(CommandSender sender, Player player) {
        User user = essentials.getUser(player);
        if (user == null) return false;

        String orig = user.getNickname(); // may be null
        if (orig == null || orig.isEmpty() || orig.equalsIgnoreCase(player.getName())) {
            return false; // nothing to clean
        }

        String cleaned = BLOCKED.matcher(orig).replaceAll("");

        if (!cleaned.equals(orig)) {
            if (cleaned.trim().isEmpty()) {
                user.setNickname(null);
            } else {
                user.setNickname(cleaned);
            }

            sender.sendMessage(CommandMessages.command(Colors.RESET + player.getName() + ": \"" + orig + Colors.RESET
                    + "\" -> \"" + cleaned + Colors.RESET + "\"."));
            return true;
        }
        return false;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> out = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(prefix)) {
                    out.add(p.getName());
                }
            }
            Collections.sort(out);
            return out;
        }
        return Collections.emptyList();
    }
}
