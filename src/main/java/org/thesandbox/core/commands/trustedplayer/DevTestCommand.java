package org.thesandbox.core.commands.trustedplayer;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.commands.CommandCenter;
import org.thesandbox.core.fun.Utils;
import org.thesandbox.core.login.LoginService;

public class DevTestCommand extends BaseCommand {

    @Override
    public boolean execute(
        CommandSender sender,
        Command command,
        String label,
        String[] args
    ) {
        if (
                (CommandCenter.request(command, sender, LoginService.Rank.OPERATOR))
        ) {
            Utils.mlog("Bleep Bloop Command Worked!");
            passedTest(sender);
        } else {
            sendError(sender, "You do not have permission to use this command.");
        }
        return true;
    }

    private static void passedTest(CommandSender sender) {
        if (sender instanceof Player plr) {
            String rank = TheSandboxCore.getInstance()
                    .getLoginService()
                    .getRank(plr)
                    .name();
            Utils.SendMessage(plr, "You are Epical! (" + rank + ")", NamedTextColor.DARK_PURPLE);
        } else {
            sender.sendMessage("You are Epical! (CONSOLE)");
        }
        Utils.sendASCII("Wee woo wee woo weee woo");
    }
}
