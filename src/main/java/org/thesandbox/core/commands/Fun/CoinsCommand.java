package org.thesandbox.core.commands.Fun;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.managers.BalanceManager;

import java.util.ArrayList;
import java.util.List;

public class CoinsCommand extends BaseCommand {

    private final BalanceManager balanceManager;

    public CoinsCommand() {
        this(TheSandboxCore.getInstance().getBalanceManager());
    }

    public CoinsCommand(BalanceManager balanceManager) {
        this.balanceManager = balanceManager;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                sendCommand(sender, "You currently have: " + balance().getCoins(player.getUniqueId()) + " Coins!");
            } else {
                sendUsage(sender, "Usage: /" + label + " <get|give|set|add|take> [args]");
            }
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "get" -> {
                if (args.length < 2) {
                    sendUsage(sender, "Usage: /" + label + " get <player>");
                    return true;
                }
                Player target = resolveTarget(sender, args[1]);
                if (target == null) {
                    sendError(sender, args[1] + " is offline!");
                    return true;
                }
                sendCommand(sender, target.getName() + " has " + balance().getCoins(target.getUniqueId()) + " Coins!");
                return true;
            }
            case "give" -> {
                Player player = asPlayer(sender);
                if (player == null) {
                    return true;
                }
                if (args.length < 3) {
                    sendUsage(sender, "Usage: /" + label + " give <player> <amount>");
                    return true;
                }
                Player target = resolveTarget(sender, args[1]);
                Integer amount = parseInt(sender, args[2]);
                if (amount == null) {
                    return true;
                }
                balance().TransferCoins(player, target, amount, args);
                return true;
            }
            case "set" -> {
                if (!hasStaffPermission(sender)) {
                    return true;
                }
                if (args.length < 3) {
                    sendUsage(sender, "Usage: /" + label + " set <player> <amount>");
                    return true;
                }
                Player target = resolveTarget(sender, args[1]);
                Integer amount = parseInt(sender, args[2]);
                if (amount == null) {
                    return true;
                }
                if (target == null) {
                    sendError(sender, args[1] + " is offline!");
                    return true;
                }
                if (amount < 0) {
                    sendError(sender, "Amount cannot be negative.");
                    return true;
                }
                balance().setCoins(target.getUniqueId(), amount);
                sendCommand(sender, sender.getName() + " set coins to " + amount + " for " + target.getName());
                return true;
            }
            case "take" -> {
                if (!hasStaffPermission(sender)) {
                    return true;
                }
                if (args.length < 3) {
                    sendUsage(sender, "Usage: /" + label + " take <player> <amount>");
                    return true;
                }
                Player target = resolveTarget(sender, args[1]);
                Integer amount = parseInt(sender, args[2]);
                if (amount == null) {
                    return true;
                }
                balance().TakeCoins(sender, target, amount, args);
                return true;
            }
            case "add" -> {
                if (!hasStaffPermission(sender)) {
                    return true;
                }
                if (args.length < 3) {
                    sendUsage(sender, "Usage: /" + label + " add <player> <amount>");
                    return true;
                }
                Integer amount = parseInt(sender, args[2]);
                if (amount == null) {
                    return true;
                }
                balance().AddCoins(sender, args[1], amount, args);
                return true;
            }
            default -> {
                sendUsage(sender, "Usage: /" + label + " <get|give" + (sender.hasPermission("sandbox.staff") ? "|set|add|take" : "") + "> [args]");
                return true;
            }
        }
    }

    private BalanceManager balance() {
        return balanceManager != null ? balanceManager : TheSandboxCore.getInstance().getBalanceManager();
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
            return playerSelectorCompletions(args[1]);
        }

        return List.of();
    }
}
