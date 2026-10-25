package org.thesandbox.core.commands;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.fun.Utils;
import org.thesandbox.core.login.LoginService;
import org.thesandbox.core.login.LoginService.Rank;
import org.thesandbox.core.util.GenericDataKeys;

public class CommandCenter {

    private static TheSandboxCore plugin = TheSandboxCore.getInstance();
    private LoginService loginService = plugin.getLoginService();

    // constructer ;p bleh
    public CommandCenter() {}

    public static boolean request(
        String command,
        CommandSender sender,
        LoginService.Rank requiredRank
    ) {
        Utils.mlog(
            "-command: " + command,
            "-CommandSender: " + sender,
            plugin.getName()
        );

        if (sender instanceof Player player) {
            Utils.mlog(
                "Player: " + Utils.plainText(player.name()),
                "Rank: " + getRank(player)
            );
            return isAllowed(player, requiredRank);
        }
        return false;
    }

    // input the rank you WANT not the Rank The Player has
    private static boolean isAllowed(Player player, LoginService.Rank rank) {
        if (player.hasPermission(rank.name())) {
            Utils.SendMessage(
                player,
                GenericDataKeys.CMD_SUCCESS,
                NamedTextColor.GREEN
            );
            return true;
        }
        Utils.SendMessage(
            player,
            GenericDataKeys.CMD_PERM_ERROR,
            NamedTextColor.DARK_RED
        );
        return false;
    }

    private static String getRank(Player player) {
        TheSandboxCore core = TheSandboxCore.getInstance();
        if (core != null && core.getLoginService() != null) {
            LoginService ls = plugin.getLoginService();
            LoginService.Rank rank = ls.getRank(player.getUniqueId());
            if (rank != null) return rank.name();
        }
        return Rank.DEFAULT.name();
    }
}
