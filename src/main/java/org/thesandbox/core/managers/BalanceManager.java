package org.thesandbox.core.managers;

import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.fun.Utils;
import org.thesandbox.core.util.Internals.FunctionsHolder;
import org.thesandbox.core.util.PlayerDataKeys;
import org.thesandbox.core.util.PlayerDataListener;

public class BalanceManager {

    private final TheSandboxCore core = TheSandboxCore.getInstance();
    private final PlayerDataListener pdl = core.getDataListener();
    private final FunctionsHolder functionsHolder = core.getFunctionsHolder();

    public void init() {
        Utils.log("Initialized BalanceManager");
    }

    /// @param sender sender here is the sender
    /// @param target recieve here is the target
    /// @param amount is the amount of coins
    /// @param Args   arguments
    public void TakeCoins(
        CommandSender sender,
        Player target,
        int amount,
        String[] Args
    ) {
        if (isAllowed(sender, target, amount, Args)) {
            Bukkit.broadcast(
                Component.text(
                    sender.getName() +
                        " Has Taken " +
                        amount +
                        " Coins From " +
                        target.getName(),
                    NamedTextColor.GOLD
                )
            );
            takecoins(sender, target, amount);
        }
    }

    private boolean isAllowed(
        CommandSender sender,
        Player recieve,
        int amount,
        String[] args
    ) {
        return !Malformation_Check(sender, recieve, amount, args);
    }

    public void TransferCoins(
        Player source,
        Player recieve,
        int amount,
        String[] args
    ) {
        // check

        if (!isAllowed(source, recieve, amount, args)) return;

        // Passed
        transferCoins(source, recieve, amount);

        // transaction messages

        // BROADCAST
        Bukkit.broadcast(
            Component.text(
                source.getName() +
                    " has given " +
                    recieve.getName() +
                    " " +
                    amount +
                    " coins!",
                NamedTextColor.GOLD
            )
        );
        //

        TransactionMessage(
            recieve,
            "You've Recieved " +
                amount +
                " Coins From " +
                source.getName() +
                "!",
            NamedTextColor.DARK_GREEN
        );
        TransactionMessage(
            source,
            "You've Sent " + amount + " Coins To " + recieve.getName(),
            NamedTextColor.GOLD
        );
        TransactionMessage(
            source,
            recieve.getName() +
                " has " +
                getCoins(recieve.getUniqueId()) +
                " Coins!",
            NamedTextColor.GREEN
        );
        TransactionMessage(
            source,
            "Current Balance: " + getCoins(source.getUniqueId()) + " Coins!",
            NamedTextColor.YELLOW
        );
    }

    // Feedback
    private void TransactionMessage(
        Player player,
        String message,
        NamedTextColor color
    ) {
        Utils.Feedback(player, message, color, Sound.ENTITY_ARROW_HIT_PLAYER);
    }

    public void transferCoins(Player source, Player recieve, int amount) {
        setCoins(source.getUniqueId(), getCoins(source.getUniqueId()) - amount);
        setCoins(
            recieve.getUniqueId(),
            getCoins(recieve.getUniqueId()) + amount
        );
    }

    public void takecoins(CommandSender sender, Player player, int amount) {
        setCoins(player.getUniqueId(), getCoins(player.getUniqueId()) - amount);
        TransactionMessage(
            player,
            "You've Been Beanzz'd by hehe",
            NamedTextColor.DARK_PURPLE
        );
        if (sender instanceof Player player2) {
            setCoins(
                player2.getUniqueId(),
                getCoins(player2.getUniqueId()) + amount
            );
            TransactionMessage(
                player2,
                "You've Taken " + amount + " Coins From " + player.getName(),
                NamedTextColor.DARK_GREEN
            );
        }
    }

    // sounds cool but is just the old checks ported from coinsCommand.java
    private boolean Malformation_Check(
        CommandSender sender,
        Player targetPlayer,
        int amount,
        String[] args
    ) {
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            sender.sendMessage(
                Component.text("Player is offline!", NamedTextColor.DARK_GRAY)
            );
            return true;
        }
        if (args == null || args.length < 3) {
            sender.sendMessage(
                Component.text(
                    "[Error]: Missing command arguments.",
                    NamedTextColor.RED
                )
            );
            return true;
        }
        String target = args[1];
        int givenCoins;

        // coins is an int
        try {
            givenCoins = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(
                Component.text(
                    "[Error]: " + args[2] + " is not a number.",
                    NamedTextColor.RED
                )
            );
            return true;
        }
        // source is the recieve?
        if (sender.getName().equals(targetPlayer.getName())) {
            sender.sendMessage(
                "[Error]: " + target + " is the same as " + sender.getName()
            );
            return true;
        }

        if (!(sender instanceof Player)) {
            return isOffline(sender, target);
        }
        boolean checks =
            isOffline(sender, target) ||
            inSufficientFunds(sender, targetPlayer, givenCoins);
        if (checks) {
            Utils.blog("Validation ended with: Error detected");
            return true;
        }
        Utils.blog("passed");
        return false;
        // true means malformed
        // false means it passed
    }

    private boolean isOffline(CommandSender sender, String target) {
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(target);
        if (
            targetPlayer == null ||
            !targetPlayer.isOnline() ||
            !targetPlayer.hasPlayedBefore()
        ) {
            sender.sendMessage(
                Component.text(
                    target + " is offline!",
                    NamedTextColor.DARK_GRAY
                )
            );
            return true;
        }
        return false;
    }

    private boolean inSufficientFunds(
        CommandSender commandSender,
        Player targetPlayer,
        int givenCoins
    ) {
        Player sender =
            commandSender instanceof Player ? (Player) commandSender : null;
        if (sender == null) {
            Utils.log("Refused");
            return true;
        }
        int senderBalance = getCoins(sender.getUniqueId());
        if (senderBalance < givenCoins) {
            sender.sendMessage(
                Component.text(
                    "You don't have enough coins!",
                    NamedTextColor.RED
                )
            );
            log(sender, "Refused", givenCoins);
            return true;
        } else if (givenCoins <= 0) {
            sender.sendMessage(
                Component.text(
                    "Dude.",
                    NamedTextColor.DARK_RED,
                    TextDecoration.ITALIC,
                    TextDecoration.BOLD
                )
            );
            log(sender, "Refused", givenCoins);
            return true;
        }
        log(sender, "Allowed", givenCoins);
        return false;
    }

    private void log(Player sender, String message, int givenCoins) {
        int senderBalance = getCoins(sender.getUniqueId());
        Utils.blog(
            "Player: " + sender + " attempted to send " + givenCoins + " coins",
            " Whilst having " + senderBalance,
            " Response: " + message
        );
    }

    // old helper methods

    public void AddCoins(
        CommandSender sender,
        String target,
        int amount,
        String[] Args
    ) {
        if (
            !Malformation_Check(
                sender,
                functionsHolder.resolveTarget(sender, target),
                amount,
                Args
            )
        ) {
            Player targetPlayer = functionsHolder.resolveTarget(sender, target);
            addCoins(targetPlayer.getUniqueId(), amount);
            Bukkit.broadcast(
                Component.text(
                    sender.getName() +
                        " has given " +
                        targetPlayer.getName() +
                        " " +
                        amount +
                        " coins!",
                    NamedTextColor.GOLD
                )
            );
            TransactionMessage(
                targetPlayer,
                "You've Recieved " + amount + " Coins From " + sender.getName(),
                NamedTextColor.GREEN
            );
            sender.sendMessage(
                Component.text(
                    targetPlayer.getName() +
                        " has " +
                        getCoins(targetPlayer.getUniqueId()) +
                        " Coins!",
                    NamedTextColor.GREEN
                )
            );
        }
    }

    public int getCoins(UUID uuid) {
        return pdl.get(uuid, PlayerDataKeys.COINS, 0);
    }

    public void setCoins(UUID uuid, int coins) {
        pdl.set(uuid, PlayerDataKeys.COINS, coins);
    }

    public void addCoins(UUID uuid, int amount) {
        pdl.set(uuid, PlayerDataKeys.COINS, getCoins(uuid) + amount);
    }

    public void removeCoins(UUID uuid, int amount) {
        pdl.set(uuid, PlayerDataKeys.COINS, getCoins(uuid) - amount);
    }

    // isallowed?
    private boolean isAllowed(
        Player sender,
        Player reciever,
        int amount,
        String[] args
    ) {
        int sendercoins = getCoins(sender.getUniqueId());
        boolean checks =
            sender.isOnline() &&
            !Malformation_Check(sender, reciever, amount, args);
        if (checks) {
            return true;
        }
        Utils.Feedback(
            sender,
            "Error: Transaction Cancelled",
            NamedTextColor.RED,
            Sound.ENTITY_VILLAGER_NO
        );
        return false;
    }
}
