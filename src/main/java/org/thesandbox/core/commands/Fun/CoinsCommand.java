package org.thesandbox.core.commands.Fun;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.commands.ISubCommand;
import org.thesandbox.core.managers.BalanceManager;
import org.thesandbox.core.util.Internals.FunctionsHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CoinsCommand implements ISubCommand {

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            if (!requireStaff(sender)) {
                return true;
            }
            Player targetPlayer = resolveTarget(sender, args[1]);
            Integer amount = parseAmount(sender, args[2]);
            if (amount == null) {
                return true;
            }
            if (targetPlayer == null) {
                sender.sendMessage(Component.text(args[1] + " is offline!", NamedTextColor.DARK_GRAY));
                return true;
            }
            if (amount < 0) {
                sender.sendMessage(Component.text("[Error]: Amount cannot be negative.", NamedTextColor.RED));
                return true;
            }
            balance().setCoins(targetPlayer.getUniqueId(), amount);
            sender.sendMessage(Component.text(
                    sender.getName() + " Set Coins To " + amount + " For " + targetPlayer.getName(),
                    NamedTextColor.DARK_GREEN
            ));
            return true;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("take")) {
            if (!requireStaff(sender)) {
                return true;
            }
            Player targetPlayer = resolveTarget(sender, args[1]);
            Integer amount = parseAmount(sender, args[2]);
            if (amount == null) {
                return true;
            }
            balance().TakeCoins(sender, targetPlayer, amount, args);
            return true;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
            if (!requireStaff(sender)) {
                return true;
            }
            Integer amount = parseAmount(sender, args[2]);
            if (amount == null) {
                return true;
            }
            balance().AddCoins(sender, args[1], amount, args);
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("get")) {
            Player targetPlayer = resolveTarget(sender, args[1]);
            if (targetPlayer == null) {
                sender.sendMessage(Component.text(args[1] + " is offline!", NamedTextColor.DARK_GRAY));
                return true;
            }
            sender.sendMessage(Component.text(
                    targetPlayer.getName() + " has " + balance().getCoins(targetPlayer.getUniqueId()) + " Coins!",
                    NamedTextColor.YELLOW
            ));
            return true;
        }

        if (!(sender instanceof Player senderPlayer)) {
            sender.sendMessage(Component.text("Usage: /" + label + " <get|set|add|take> <player> [amount]", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text(
                    "You Currently Have: " + balance().getCoins(senderPlayer.getUniqueId()) + " Coins!",
                    NamedTextColor.YELLOW
            ));
            return true;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            Player targetPlayer = resolveTarget(sender, args[1]);
            Integer givenCoins = parseAmount(sender, args[2]);
            if (givenCoins == null) {
                return true;
            }
            balance().TransferCoins(senderPlayer, targetPlayer, givenCoins, args);
            return true;
        }

        sender.sendMessage(Component.text("Usage: /" + label + " [give <player> <amount>]", NamedTextColor.RED));
        return true;
    }

    private boolean requireStaff(CommandSender sender) {
        if (sender.hasPermission("sandbox.staff")) {
            return true;
        }
        sender.sendMessage(Component.text("You do not have permission to use this command!", NamedTextColor.RED));
        return false;
    }

    private Player resolveTarget(CommandSender sender, String input) {
        return FunctionsHolder.resolveTarget(sender, input);
    }

    private Integer parseAmount(CommandSender sender, String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("[Error]: " + input + " is not a number.", NamedTextColor.RED));
            return null;
        }
    }

    private BalanceManager balance() {
        return TheSandboxCore.getInstance().getBalanceManager();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> list = new ArrayList<>();
            list.add("get");
            list.add("give");
            if (sender.hasPermission("sandbox.staff")) {
                list.add("set");
                list.add("add");
                list.add("take");
            }
            list.removeIf(s -> !s.toLowerCase().startsWith(prefix));
            return list;
        }

        if (args.length == 2 && List.of("give", "set", "add", "get", "take").contains(args[0].toLowerCase())) {
            String prefix = args[1].toLowerCase();
            List<String> suggestions = Bukkit.getOnlinePlayers()
                    .stream()
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
