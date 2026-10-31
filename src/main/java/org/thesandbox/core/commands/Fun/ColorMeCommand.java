package org.thesandbox.core.commands.Fun;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.thesandbox.core.commands.ISubCommand;
import org.thesandbox.core.util.Colors;

import java.util.*;

public class ColorMeCommand implements ISubCommand, TabCompleter {

    private final JavaPlugin plugin;
    private final Map<String, String> colors = new LinkedHashMap<>();

    public ColorMeCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        // Plain colours only (no magic/underline/italic/strike/reset)
        colors.put("black", Colors.BLACK);
        colors.put("dark_blue", Colors.DARK_BLUE);
        colors.put("dark_green", Colors.DARK_GREEN);
        colors.put("dark_aqua", Colors.DARK_AQUA);
        colors.put("dark_red", Colors.DARK_RED);
        colors.put("dark_purple", Colors.DARK_PURPLE);
        colors.put("gold", Colors.GOLD);
        colors.put("gray", Colors.GRAY);
        colors.put("dark_gray", Colors.DARK_GRAY);
        colors.put("blue", Colors.BLUE);
        colors.put("green", Colors.GREEN);
        colors.put("aqua", Colors.AQUA);
        colors.put("red", Colors.RED);
        colors.put("light_purple", Colors.LIGHT_PURPLE);
        colors.put("yellow", Colors.YELLOW);
        colors.put("white", Colors.WHITE);
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Colors.color("&c&lError &8» " + "Only players can use this command."));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(Colors.color("&c&lUsage &8» " + "/" + label + " <color|list>"));
            return true;
        }

        final Plugin p = Bukkit.getPluginManager().getPlugin("Essentials");
        if (!(p instanceof Essentials ess)) {
            sender.sendMessage(Colors.color("&c&lError &8» " + "Essentials is required. Contact a server administrator."));
            return true;
        }

        String arg = args[0].toLowerCase(Locale.ROOT);
        if (arg.equals("list")) {
            sender.sendMessage(Colors.color("&7&lCommand &8» " + "&7Available colors: &e" + String.join(", ", colors.keySet())));
            return true;
        }

        String chosen = resolveColor(arg);
        if (chosen == null) {
            sender.sendMessage(Colors.color("&c&lError &8» " + "&cInvalid color: " + arg + ". Use /" + label + " list"));
            return true;
        }

        // Work with Essentials nickname ONLY (no rank/prefix from chat format)
        User user = ess.getUser(player);
        if (user == null) {
            sender.sendMessage(Colors.color("&c&lError &8» " + "Could not access Essentials playerdata. Contact a server administrator."));
            return true;
        }

        String baseNick = user.getNickname();              // Essentials nick (may be null)
        if (baseNick == null || baseNick.isBlank()) {
            baseNick = player.getName();                   // fallback to username
        }

        // Strip any colors from the nick itself, don’t touch rank/prefix (since we’re not using displayName)
        baseNick = Colors.strip(baseNick).trim();
        if (baseNick.isBlank()) baseNick = player.getName();

        String newNick = chosen + baseNick + Colors.WHITE; // apply color only

        user.setNickname(newNick); // Essentials API
        sender.sendMessage(Colors.color("&7&lCommand &8» &7Your nickname is now: " + newNick));
        return true;
    }

    private String resolveColor(String input) {
        for (String k : colors.keySet()) if (k.startsWith(input)) return colors.get(k);
        for (String k : colors.keySet()) if (k.contains(input))  return colors.get(k);
        return null;
    }

    // ISubCommand tab completion
    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String pref = args[0].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            if ("list".startsWith(pref)) out.add("list");
            for (String key : colors.keySet()) if (key.startsWith(pref)) out.add(key);
            return out;
        }
        return Collections.emptyList();
    }

    // Bukkit TabCompleter bridge
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> r = tabComplete(sender, command, alias, args);
        return (r != null) ? r : Collections.emptyList();
    }
}
