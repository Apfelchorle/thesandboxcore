package org.thesandbox.core.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.managers.BalanceManager;

public class CustomPlayer {

    private final TheSandboxCore core = TheSandboxCore.getInstance();
    private final BalanceManager balanceManager = core.getBalanceManager();
    private final PlayerDataListener playerDataListener =
        core.getDataListener();
    private final Player player;

    public CustomPlayer(Player player) {
        this.player = player;
    }

    public Player getBukkitPlayer() {
        return player;
    }

    public void TransferCoins(
        Player sender,
        Player target,
        int givenCoins,
        String[] args
    ) {
        balanceManager.TransferCoins(sender, target, givenCoins, args);
    }
}
