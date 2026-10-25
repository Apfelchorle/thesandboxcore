package org.thesandbox.core.commands.trustedplayer;

import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.commands.CommandCenter;
import org.thesandbox.core.commands.ISubCommand;
import org.thesandbox.core.fun.Utils;
import org.thesandbox.core.login.LoginService;
import org.thesandbox.core.login.LoginService.Rank;

public class DevTestCommand implements ISubCommand {

    @Override
    public boolean execute(
        CommandSender sender,
        Command command,
        String label,
        String[] args
    ) {
        if (
            CommandCenter.request(
                command.getName(),
                sender,
                LoginService.Rank.OPERATOR
            )
        ) {
            Utils.mlog("Bleep Bloop Command Worked!");
            passedTest(sender);
        }
        return false;
    }

    @Override
    public List<String> tabComplete(
        CommandSender sender,
        Command command,
        String alias,
        String[] args
    ) {
        return null;
    }

    private static void passedTest(CommandSender sender) {
        String name = sender.getName();
        Player plr = Bukkit.getPlayer(name);
        String rank = TheSandboxCore.getInstance()
            .getLoginService()
            .getRank(plr)
            .name();
        Utils.SendMessage(plr, "You are Epical!", NamedTextColor.DARK_PURPLE);
        Utils.sendASCII("Wee woo wee woo weee woo");
    }
}
