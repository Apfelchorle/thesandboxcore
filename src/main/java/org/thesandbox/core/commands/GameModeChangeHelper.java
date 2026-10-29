package org.thesandbox.core.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class GameModeChangeHelper {

    private GameModeChangeHelper() {
    }

    public static boolean execute(CommandSender sender, String[] args, GameMode mode) {
        String modeName = mode.name();

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(CommandMessages.error(ChatColor.RED + "Only players can change their own gamemode."));
                return true;
            }
            player.setGameMode(mode);
            player.sendMessage(CommandMessages.command(ChatColor.GRAY + "Your gamemode was set to " + modeName + "."));
            return true;
        }

        if (!sender.hasPermission("sandbox.staff")) {
            sender.sendMessage(CommandMessages.error(ChatColor.RED + "No permission."));
            return true;
        }

        if (args[0].equalsIgnoreCase("-a")) {
            for (Player target : Bukkit.getOnlinePlayers()) {
                target.setGameMode(mode);
            }
            Bukkit.broadcastMessage(CommandMessages.server(ChatColor.RED + sender.getName()
                    + " changed everyone's gamemode to " + modeName + "."));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            target = Bukkit.getPlayer(args[0]);
        }
        if (target == null) {
            sender.sendMessage(CommandMessages.error(ChatColor.RED + "Player not found."));
            return true;
        }

        target.setGameMode(mode);
        Bukkit.broadcastMessage(CommandMessages.server(ChatColor.RED + sender.getName()
                + " changed " + target.getName() + "'s gamemode to " + modeName + "."));
        sender.sendMessage(CommandMessages.command(ChatColor.GRAY + "Set " + target.getName() + "'s gamemode to " + modeName + "."));
        return true;
    }

    public static List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length != 1 || !sender.hasPermission("sandbox.staff")) {
            return Collections.emptyList();
        }

        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> names = new ArrayList<>();
        if ("-a".startsWith(prefix)) {
            names.add("-a");
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                names.add(player.getName());
            }
        }
        return names;
    }
}
