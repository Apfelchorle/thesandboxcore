package org.thesandbox.core.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.util.PlayerDataListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class CoinsCommand implements ISubCommand {

    private final PlayerDataListener playerDataListener;

    public CoinsCommand(PlayerDataListener playerDataListener) {
        this.playerDataListener = playerDataListener;
    }

    private Player resolveTarget(CommandSender sender, String input) {
        if (input.equalsIgnoreCase("@s")) {
            return (sender instanceof Player) ? (Player) sender : null;
        }

        if (input.equalsIgnoreCase("@p")) {
            if (!(sender instanceof Player)) return null; // console has no location to measure "nearest" from
            Player senderPlayer = (Player) sender;
            return senderPlayer.getWorld().getPlayers().stream()
                    .filter(p -> !p.equals(senderPlayer))
                    .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(senderPlayer.getLocation())))
                    .orElse(null);
        }
        return Bukkit.getPlayer(input);
    }


    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {


        // CONSOLE CAN RUN THESE

        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            if (!(sender.hasPermission("sandbox.staff"))) {
                sender.sendMessage(Component.text("You do not have permission to use this command!", NamedTextColor.RED));
                return true;
            }

            String target = args[1];
            Player targetPlayer = resolveTarget(sender, target);
            int amount;

            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("[Error]: " + args[2] + " is not a number.", NamedTextColor.RED));
                return true;
            }

            if (targetPlayer == null) {
                sender.sendMessage(Component.text(target + " is offline!", NamedTextColor.DARK_GRAY));
                return true;
            }

            playerDataListener.setCoins(targetPlayer.getUniqueId(), amount);

            sender.sendMessage(Component.text( sender.getName() + " Set Coins To " + amount + " For " + targetPlayer.getName(), NamedTextColor.RED));
            return true;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
            if (!(sender.hasPermission("sandbox.staff"))) {
                sender.sendMessage(Component.text("You do not have permission to use this command!", NamedTextColor.RED));
                return true;
            }
            String target = args[1];
            Player targetPlayer = resolveTarget(sender, target);
            int amount;
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("[Error]: " + args[2] + " is not a number.", NamedTextColor.RED));
                return true;
            }

            if (targetPlayer == null) {
                sender.sendMessage(Component.text(target + " is offline!", NamedTextColor.DARK_GRAY));
                return true;
            }

            playerDataListener.addCoins(targetPlayer.getUniqueId(), amount);

            Bukkit.broadcast(Component.text(sender.getName() + " has given " + targetPlayer.getName() + " " + amount + " coins!", NamedTextColor.GOLD));
            sender.sendMessage(Component.text(targetPlayer.getName() + " has " + playerDataListener.getCoins(targetPlayer.getUniqueId()) + " Coins!", NamedTextColor.GREEN));
            targetPlayer.sendMessage(Component.text("You've Recieved " + amount + " Coins From " + sender.getName(), NamedTextColor.GREEN));

            return true;
        }


        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can execute this command!", NamedTextColor.RED));
            return true;
        }

        // CONSOLE CANT RUN THESE

        int coins = playerDataListener.getCoins(((Player) sender).getUniqueId());

        if (args.length < 2) {
            // Shows Current Coins
            sender.sendMessage(Component.text("You Currently Have: " + coins + " Coins!", NamedTextColor.YELLOW));
            return true;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            String target = args[1];
            Player targetPlayer = resolveTarget(sender, target);
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

            int senderBalance = playerDataListener.getCoins(((Player) sender).getUniqueId());
            if (senderBalance < givenCoins) {
                sender.sendMessage(Component.text("You don't have enough coins!", NamedTextColor.RED));
                return true;
            }

            // data
            playerDataListener.removeCoins(((Player) sender).getUniqueId(), givenCoins);
            playerDataListener.addCoins(targetPlayer.getUniqueId(), givenCoins);


            // notifs
            Bukkit.broadcast(Component.text(sender.getName() + " has given " + targetPlayer.getName() + " " + givenCoins + " coins!", NamedTextColor.GOLD));
            sender.sendMessage(Component.text(targetPlayer.getName() + " has " + playerDataListener.getCoins(targetPlayer.getUniqueId()) + " Coins!", NamedTextColor.GREEN));
            sender.sendMessage(Component.text("Current Balance: " + playerDataListener.getCoins(((Player) sender).getUniqueId()) + " Coins!", NamedTextColor.YELLOW));
            targetPlayer.sendMessage(Component.text("You've Recieved " + givenCoins + " Coins From " + sender.getName(), NamedTextColor.GREEN));

            return true;
        }
        // beanzz hehe was hehe
        // -usfl

        // 0 = give
        // 1 = player target
        // 2 = given coins
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> list = new ArrayList<>();
            list.add("set");
            list.add("give");
            list.add("add");
            list.removeIf(s -> !s.toLowerCase().startsWith(prefix));
            return list;
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("add"))) {
            String prefix = args[1].toLowerCase();
            List<String> suggestions = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
            suggestions.add("@p");
            suggestions.add("@s");
            suggestions.removeIf(name -> !name.toLowerCase().startsWith(prefix));
            return suggestions;
        }

        return List.of();
    }
}