package org.thesandbox.core.commands;

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

import java.util.Locale;

public class CommandCenter {

    public static boolean request(Command command, CommandSender sender, LoginService.Rank requiredRank) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        if (hasRank(player, requiredRank)) {
            return true;
        }

        Utils.Feedback(player, GenericDataKeys.CMD_PERM_ERROR, NamedTextColor.DARK_RED, Sound.ENTITY_VILLAGER_NO);
        Utils.blog(
                "Refused Command : " + command.getName(),
                "Sender : " + sender.getName(),
                "RequiredRank : " + requiredRank
        );
        return false;
    }

    /**
     * True if the sender holds the node of the required rank OR of any higher rank,
     * so an Operator doesn't need an explicit sandbox.staff node to run staff commands.
     * Non-players (console) always pass.
     */
    public static boolean hasRank(CommandSender sender, Rank required) {
        if (!(sender instanceof Player)) {
            return true;
        }
        for (Rank r : Rank.values()) {
            if (r.ordinal() < required.ordinal()) continue;
            if (sender.hasPermission("sandbox." + r.name().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    public static String getRank(Player player) {
        TheSandboxCore core = TheSandboxCore.getInstance();
        if (core != null && core.getLoginService() != null) {
            Rank rank = core.getLoginService().getRank(player);
            if (rank != null) {
                return rank.name();
            }
        }
        return Rank.DEFAULT.name();
    }
}
