package org.thesandbox.core.commands.Admin;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.login.LoginService;

import java.util.Arrays;
import java.util.List;

/**
 * /gtfo <player> [reason...] – temporarily bans a player (config: gtfo.duration, default 24h).
 *
 * Uses /tempban (EssentialsX is a hard dependency, LiteBans overrides it when present). The previous
 * "ban <name> 24h" form only works with LiteBans; with vanilla /ban it created a PERMANENT ban
 * whose reason started with "24h".
 */
public class GtfoCommand extends BaseCommand {

    private final TheSandboxCore plugin;

    public GtfoCommand(TheSandboxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sendUsage(sender, "Usage: /" + label + " <player> [reason]");
            return true;
        }

        String permission = command.getPermission();
        if (permission != null && !hasPermission(sender, permission)) {
            return true;
        }

        Player online = Bukkit.getPlayerExact(args[0]);
        OfflinePlayer target = online != null ? online : Bukkit.getOfflinePlayerIfCached(args[0]);
        if (target == null || target.getName() == null) {
            sendError(sender, args[0] + " has never played on this server.");
            return true;
        }

        if (sender instanceof Player senderPlayer) {
            if (senderPlayer.getUniqueId().equals(target.getUniqueId())) {
                sendError(sender, "You can't ban yourself.");
                return true;
            }
            if (online != null) {
                LoginService ls = plugin.getLoginService();
                if (ls != null && ls.getRank(online).ordinal() >= ls.getRank(senderPlayer).ordinal()
                        && ls.getRank(online).ordinal() >= LoginService.Rank.STAFF.ordinal()) {
                    sendError(sender, "You can't ban a player of equal or higher rank.");
                    return true;
                }
            }
        }

        String duration = plugin.getConfig().getString("gtfo.duration", "24h");
        String reason = args.length > 1
                ? String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                : "Banned via TheSandboxCore";

        sendCommand(sender, "Banning " + target.getName() + " for " + duration + ".");
        Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "tempban " + target.getName() + " " + duration + " " + reason
        );
        // The ban kicks the player, so there is nothing to message them afterwards.
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return playerCompletions(args[0]);
        }
        return List.of();
    }
}
