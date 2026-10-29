package org.thesandbox.core.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.fun.Utils;
import org.thesandbox.core.util.PlayerDataKeys;
import org.thesandbox.core.util.PlayerDataListener;

import java.util.UUID;

public class BalanceManager {

    private PlayerDataListener playerData() {
        return TheSandboxCore.getInstance().getDataListener();
    }

    public void init() {
        Utils.log("Initialized BalanceManager");
    }

    public void TakeCoins(CommandSender sender, Player target, int amount, String[] args) {
        Integer parsed = parsePositiveAmount(sender, amount, args);
        if (parsed == null) {
            return;
        }
        if (!requireOnlineTarget(sender, target)) {
            return;
        }
        if (isSamePlayer(sender, target)) {
            sender.sendMessage(Component.text("[Error]: You cannot take coins from yourself.", NamedTextColor.RED));
            return;
        }

        int taken = Math.min(parsed, getCoins(target.getUniqueId()));
        if (taken <= 0) {
            sender.sendMessage(Component.text(target.getName() + " has no coins to take.", NamedTextColor.RED));
            return;
        }

        removeCoins(target.getUniqueId(), taken);
        Bukkit.broadcast(Component.text(
                sender.getName() + " Has Taken " + taken + " Coins From " + target.getName(),
                NamedTextColor.GOLD
        ));
        TransactionMessage(target, "You've Been Beanzz'd by hehe", NamedTextColor.DARK_PURPLE);
        if (sender instanceof Player staff) {
            addCoins(staff.getUniqueId(), taken);
            TransactionMessage(staff, "You've Taken " + taken + " Coins From " + target.getName(), NamedTextColor.DARK_GREEN);
        }
    }

    public void TransferCoins(Player source, Player receive, int amount, String[] args) {
        Integer parsed = parsePositiveAmount(source, amount, args);
        if (parsed == null) {
            return;
        }
        if (!requireOnlineTarget(source, receive)) {
            return;
        }
        if (source.getUniqueId().equals(receive.getUniqueId())) {
            source.sendMessage(Component.text("[Error]: You cannot send coins to yourself.", NamedTextColor.RED));
            return;
        }
        if (getCoins(source.getUniqueId()) < parsed) {
            source.sendMessage(Component.text("You don't have enough coins!", NamedTextColor.RED));
            Utils.Feedback(source, "Error: Transaction Cancelled", NamedTextColor.RED, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        removeCoins(source.getUniqueId(), parsed);
        addCoins(receive.getUniqueId(), parsed);
        Bukkit.broadcast(Component.text(source.getName() + " has given " + receive.getName() + " " + parsed + " coins!", NamedTextColor.GOLD));
        TransactionMessage(receive, "You've Received " + parsed + " Coins From " + source.getName() + "!", NamedTextColor.DARK_GREEN);
        TransactionMessage(source, "You've Sent " + parsed + " Coins To " + receive.getName(), NamedTextColor.GOLD);
        Utils.SendMessage(source, receive.getName() + " has " + getCoins(receive.getUniqueId()) + " Coins!", NamedTextColor.GREEN);
        Utils.SendMessage(source, "Current Balance: " + getCoins(source.getUniqueId()) + " Coins!", NamedTextColor.YELLOW);
    }

    public void AddCoins(CommandSender sender, String target, int amount, String[] args) {
        Integer parsed = parsePositiveAmount(sender, amount, args);
        if (parsed == null) {
            return;
        }
        Player targetPlayer = org.thesandbox.core.util.Internals.FunctionsHolder.resolveTarget(sender, target);
        if (!requireOnlineTarget(sender, targetPlayer)) {
            return;
        }

        addCoins(targetPlayer.getUniqueId(), parsed);
        Bukkit.broadcast(Component.text(
                sender.getName() + " has given " + targetPlayer.getName() + " " + parsed + " coins!",
                NamedTextColor.GOLD
        ));
        TransactionMessage(targetPlayer, "You've Recieved " + parsed + " Coins From " + sender.getName(), NamedTextColor.GREEN);
        sender.sendMessage(Component.text(
                targetPlayer.getName() + " has " + getCoins(targetPlayer.getUniqueId()) + " Coins!",
                NamedTextColor.GREEN
        ));
    }

    private Integer parsePositiveAmount(CommandSender sender, int amount, String[] args) {
        int value = amount;
        if (args != null && args.length >= 3) {
            try {
                value = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("[Error]: " + args[2] + " is not a number.", NamedTextColor.RED));
                return null;
            }
        }
        if (value <= 0) {
            sender.sendMessage(Component.text("Dude.", NamedTextColor.DARK_RED, TextDecoration.ITALIC, TextDecoration.BOLD));
            return null;
        }
        return value;
    }

    private boolean requireOnlineTarget(CommandSender sender, Player target) {
        if (target == null || !target.isOnline()) {
            sender.sendMessage(Component.text("Player is offline!", NamedTextColor.DARK_GRAY));
            return false;
        }
        return true;
    }

    private boolean isSamePlayer(CommandSender sender, Player target) {
        return sender instanceof Player player && player.getUniqueId().equals(target.getUniqueId());
    }

    private void TransactionMessage(Player player, String message, NamedTextColor color) {
        Utils.Feedback(player, message, color, Sound.ENTITY_ARROW_HIT_PLAYER);
    }

    public int getCoins(UUID uuid) {
        return playerData().get(uuid, PlayerDataKeys.COINS, 0);
    }

    public void setCoins(UUID uuid, int coins) {
        playerData().set(uuid, PlayerDataKeys.COINS, Math.max(0, coins));
    }

    public void addCoins(UUID uuid, int amount) {
        long next = (long) getCoins(uuid) + amount;
        setCoins(uuid, (int) Math.min(Integer.MAX_VALUE, Math.max(0, next)));
    }

    public void removeCoins(UUID uuid, int amount) {
        long next = (long) getCoins(uuid) - amount;
        setCoins(uuid, (int) Math.max(0, next));
    }
}
