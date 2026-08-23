package org.thesandbox.core.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.util.PlayerDataListener;

import java.util.List;
import java.util.stream.Collectors;

public class CoinsGiveCommand implements ISubCommand {

    private final PlayerDataListener playerDataListener;

    public CoinsGiveCommand(PlayerDataListener playerDataListener) {
        this.playerDataListener = playerDataListener;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("thesandbox.staff")) {
            sender.sendMessage(Component.text("You do not have permission to give coins.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /coins give <player> <amount>", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found or offline.", NamedTextColor.RED));
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
            if (amount <= 0) {
                sender.sendMessage(Component.text("Amount must be a positive number.", NamedTextColor.RED));
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid amount. Please type a valid number.", NamedTextColor.RED));
            return true;
        }

        playerDataListener.addCoins(target.getUniqueId(), amount);

        sender.sendMessage(Component.text("Gave ")
                .append(Component.text(amount + " coins ", NamedTextColor.GOLD))
                .append(Component.text("to "))
                .append(Component.text(target.getName(), NamedTextColor.YELLOW))
                .color(NamedTextColor.GREEN));

        target.sendMessage(Component.text("You received ")
                .append(Component.text(amount + " coins!", NamedTextColor.GOLD))
                .color(NamedTextColor.GREEN));

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
