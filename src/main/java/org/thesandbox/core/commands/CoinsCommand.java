package org.thesandbox.core.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.util.DataManager;
import org.thesandbox.core.util.PlayerDataListener;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CoinsCommand implements ISubCommand {

    private final PlayerDataListener playerDataListener;

    public CoinsCommand(PlayerDataListener playerDataListener) {
        this.playerDataListener = playerDataListener;
    }


    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can execute this command!", NamedTextColor.RED));
            return true;
        }

        int coins = playerDataListener.getCoins(((Player) sender).getUniqueId());

        if (args.length < 2) {
            // Shows Current Coins
            sender.sendMessage(Component.text("You Currently Have: " + coins + " Coins!", NamedTextColor.YELLOW));
            return true;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            String target = args[1];
            Player targetPlayer = Bukkit.getPlayer(target);
            int givenCoins;

            try {
                givenCoins = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("[Error]: " + args[2] + " is not a number.", NamedTextColor.RED));
                return true;
            }

            if (targetPlayer == null) {
                sender.sendMessage(Component.text(target + " is offline!", NamedTextColor.DARK_GRAY));
                return true;
            }

            // data
            playerDataListener.removeCoins(((Player) sender).getUniqueId(), givenCoins);
            playerDataListener.addCoins(targetPlayer.getUniqueId(), givenCoins);


            // notifs
            Bukkit.broadcast(Component.text(sender.getName() + " has given " + target + " " + givenCoins + " coins!", NamedTextColor.GOLD));
            sender.sendMessage(Component.text(target + " has " + playerDataListener.getCoins(targetPlayer.getUniqueId()) + " Coins!", NamedTextColor.GREEN));
            sender.sendMessage(Component.text("Current Balance: " + playerDataListener.getCoins(((Player) sender).getUniqueId()) + " Coins!", NamedTextColor.YELLOW));
            targetPlayer.sendMessage(Component.text("You've Recieved " + givenCoins + " Coins From " + sender.getName(), NamedTextColor.GREEN));

            return true;
        }



        if (!(sender.hasPermission("sandbox.staff"))) {
            sender.sendMessage(Component.text("You do not have permission to use this command!", NamedTextColor.RED));
            return true;
        }

        // beanzz hehe was hehe
        // -usfl

        // 0 = give
        // 1 = player target
        // 2 = given coins


        // TODO: Implement /coins set
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            sender.sendMessage(Component.text("Not Implemented! :)", NamedTextColor.RED));
            return true;
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> list = new ArrayList<>();
            list.add("set");
            list.add("give");
            list.removeIf(s -> !s.toLowerCase().startsWith(prefix));
            return list;
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("give")) || (args[0].equalsIgnoreCase("set"))) {
            String prefix = args[1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}