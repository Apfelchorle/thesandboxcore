package org.thesandbox.core.commands.Admin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.commands.ISubCommand;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class gtfoCommand implements ISubCommand {
    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {


        if  (args.length == 1) {
            String targetname = args[0];
            OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetname);

            String permission = command.getPermission();
            if (permission != null && !sender.hasPermission(permission)) {
                sender.sendMessage(Component.text("You do not have permission to use this command!", NamedTextColor.RED));
                return true;
            }

            if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
                sender.sendMessage(Component.text(targetname + " has never played on this server.", NamedTextColor.RED));
                return true;
            }

            // all this does is /ban "player" for 24hrs

            sender.sendMessage(Component.text("Banning " + targetname + " For 24h" , NamedTextColor.DARK_RED));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ban " + targetname + " 24h Banned via TheSandboxCore");

            if (targetPlayer.isOnline()) {
                Objects.requireNonNull(targetPlayer.getPlayer()).sendMessage(Component.text("Fuck off gng", NamedTextColor.DARK_RED, TextDecoration.BOLD));
            }
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
