package org.thesandbox.core.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.fun.Utils;
import org.thesandbox.core.login.LoginService;
import org.thesandbox.core.login.LoginService.Rank;
import org.thesandbox.core.util.GenericDataKeys;

public class CommandCenter {

    private static final TheSandboxCore plugin = TheSandboxCore.getInstance();
    private final LoginService loginService = plugin.getLoginService();

    // constructer ;p bleh
    public CommandCenter() {
    }


    public static boolean request(Command command, CommandSender sender, LoginService.Rank requiredRank) {
        String command_name = command.getName();
        Utils.blog("-command: " + command_name, "-CommandSender: " + sender, plugin.getName());


        if (sender instanceof Player player) {
            String player_name = Utils.plainText(player.name());
            String player_rank = getRank(player);
            Utils.blog("Player: " + player_name, "Rank: " + player_rank);
            return isAllowed(player, requiredRank);
        }
        sender.sendMessage(Component.text("This command can only be run by players!", NamedTextColor.RED));
        Utils.blog("Refused Command : " + command_name, "Sender : " + sender, "RequiredRank : " + requiredRank);
        return false;
    }

    private static boolean isAllowed(Player player, LoginService.Rank rank) {
        return hasPermission(player, rank);
    }


    private static boolean hasPermission(Player player, LoginService.Rank rank) {
        if (player.hasPermission(rank.name())) {
            Utils.Feedback(player, GenericDataKeys.CMD_SUCCESS, NamedTextColor.GREEN, Sound.ENTITY_ARROW_HIT_PLAYER);
            return true;
        }
        Utils.Feedback(player, GenericDataKeys.CMD_PERM_ERROR, NamedTextColor.DARK_RED, Sound.ENTITY_VILLAGER_NO);
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
