package org.thesandbox.core.managers;

import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
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

    // isallowed?
    private boolean isAllowed(
        Player sender,
        Player reciever,
        int amount,
        String[] args
    ) {
        int sendercoins = getCoins(sender.getUniqueId());
        boolean checks =
            sendercoins > amount &&
            reciever.isOnline() &&
            sender.isOnline() &&
            !sender.name().equals(reciever.name()) &&
            amount > 0 &&
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

    // Feedback
    private void TransactionMessage(
        Player player,
        String message,
        NamedTextColor color
    ) {
        Utils.Feedback(
            player,
            message,
            color,
            Sound.BLOCK_AMETHYST_BLOCK_CHIME
        );
    }

    //

    // sounds cool but is just the old checks ported from coinsCommand.java
    private boolean Malformation_Check(
        Player sender,
        Player targetPlayer,
        int amount,
        String[] args
    ) {
        String target = args[1];
        int givenCoins;
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

        if (targetPlayer == null) {
            sender.sendMessage(
                Component.text(
                    target + " is offline!",
                    NamedTextColor.DARK_GRAY
                )
            );
            return true;
        }

        int senderBalance = pdl.getCoins(((Player) sender).getUniqueId());
        if (senderBalance < givenCoins) {
            sender.sendMessage(
                Component.text(
                    "You don't have enough coins!",
                    NamedTextColor.RED
                )
            );
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
            return true;
        }
        return false;
    }

    // old helper methods
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

    public void transferCoins(Player source, Player recieve, int amount) {
        setCoins(source.getUniqueId(), getCoins(source.getUniqueId()) - amount);
        setCoins(
            recieve.getUniqueId(),
            getCoins(recieve.getUniqueId()) + amount
        );
    }
}
