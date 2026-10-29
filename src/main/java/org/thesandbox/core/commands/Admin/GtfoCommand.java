package org.thesandbox.core.commands.Admin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.commands.BaseCommand;

import java.util.List;

/**
 * Temporarily bans a player for 24 hours.
 */
public class GtfoCommand extends BaseCommand {
    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sendUsage(sender, "Usage: /" + label + " <player>");
            return true;
        }

        String permission = command.getPermission();
        if (permission != null && !hasPermission(sender, permission)) {
            return true;
        }

        String targetName = args[0];
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);

        if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
            sendError(sender, targetName + " has never played on this server.");
            return true;
        }

        sendCommand(sender, "Banning " + targetName + " for 24h.");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ban " + targetName + " 24h Banned via TheSandboxCore");

        if (targetPlayer.isOnline()) {
            Player onlinePlayer = targetPlayer.getPlayer();
            if (onlinePlayer != null) {
                onlinePlayer.sendMessage(Component.text(
                        "You have been temporarily banned for 24 hours.",
                        NamedTextColor.DARK_RED,
                        TextDecoration.BOLD
                ));
            }
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return playerCompletions(args[0]);
        }
        return List.of();
    }
}
