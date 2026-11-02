package org.thesandbox.core.commands.utility;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.commands.CommandMessages;
import org.thesandbox.core.util.colors.Colors;

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
                sender.sendMessage(org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.ERROR, Colors.RED + "Only players can change their own gamemode."));
                return true;
            }
            player.setGameMode(mode);
            player.sendMessage(org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.COMMAND, Colors.GRAY + "Your gamemode was set to " + modeName + "."));
            return true;
        }

        if (!sender.hasPermission("sandbox.staff")) {
            sender.sendMessage(org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.ERROR, Colors.RED + "No permission."));
            return true;
        }

        if (args[0].equalsIgnoreCase("-a")) {
            for (Player target : Bukkit.getOnlinePlayers()) {
                target.setGameMode(mode);
            }
            Bukkit.getServer().broadcast(org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.SERVER, Colors.RED + sender.getName()
                    + " changed everyone's gamemode to " + modeName + "."));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            target = Bukkit.getPlayer(args[0]);
        }
        if (target == null) {
            sender.sendMessage(org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.ERROR, Colors.RED + "Player not found."));
            return true;
        }

        target.setGameMode(mode);
        Bukkit.getServer().broadcast(org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.SERVER, Colors.RED + sender.getName()
                + " changed " + target.getName() + "'s gamemode to " + modeName + "."));
        sender.sendMessage(org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.COMMAND, Colors.GRAY + "Set " + target.getName() + "'s gamemode to " + modeName + "."));
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
