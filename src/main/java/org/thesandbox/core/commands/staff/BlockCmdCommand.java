package org.thesandbox.core.commands.staff;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.commands.ISubCommand;
import org.thesandbox.core.services.moderation.CommandBlockManager;
import org.thesandbox.core.util.colors.Colors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class BlockCmdCommand implements ISubCommand {

    // 5 minutes in millis
    private static final long BLOCK_DURATION_MS = 5L * 60L * 1000L;
    private final TheSandboxCore plugin; // need core helpers
    private CommandBlockManager commandBlockManager;

    public BlockCmdCommand(JavaPlugin plugin) {
        this.plugin = (TheSandboxCore) plugin;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sandbox.staff")) {
            sender.sendMessage(Colors.color("&c&lError &8» &cNo permission."));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Colors.color("&c&lUsage &8» &c/" + label + " <player|all|purge>"));
            return true;
        }

        final String actor = (sender instanceof Player p) ? p.getName() : "CONSOLE";
        final String sub = args[0].toLowerCase(Locale.ENGLISH);

        // /blockcmd all  -> block everyone except staff
        if (sub.equals("all")) {
            if (plugin.getCommandBlockManager().isCmdBlockAll()) {
                sender.sendMessage(Colors.color("&c&lError &8» &cCommands are currently blocked for all players."));
                return true;
            }
            plugin.getCommandBlockManager().enableCmdBlockAll();
            Colors.broadcast(Colors.color("&c&lServer &8» &c" + sender.getName() + " has blocked all online players' commands."));
            return true;
        }

        // /blockcmd purge -> remove everyone’s block and disable global
        if (sub.equals("purge")) {
            plugin.getCommandBlockManager().purgeAllCmdBlocks();
            Colors.broadcast(Colors.color("&c&lServer &8» &c" + sender.getName() + " has unblocked all online players' commands."));
            return true;
        }

        // /blockcmd <player>  -> toggle that player's block (add for 5 min or remove)
        Player target = Bukkit.getPlayerExact(args[0]);
        final String targetName = (target != null) ? target.getName() : args[0];

        // Cannot block staff players
        if (target != null && target.hasPermission("sandbox.staff")) {
            sender.sendMessage(Colors.color("&c&lError &8» &cYou cannot block the commands of other staff members."));
            return true;
        }

        // If offline, allow block by name; the on-preprocess will resolve at runtime when they’re online.
        boolean wasBlocked = plugin.getCommandBlockManager().isPlayerCmdBlockedByName(targetName);

        if (wasBlocked) {
            plugin.getCommandBlockManager().unblockPlayerCommandsByName(targetName);
            Colors.broadcast(Colors.color("&c&lServer &8» &c" + sender.getName() + " has unblocked " + targetName + "'s commands."));
        } else {
            // do not allow blocking staff even if they come online later and have perms
            if (target == null) {
                // If they are offline, we can't check perms; we’ll re-check upon first command (core handler also prevents blocking mod+).
                plugin.getCommandBlockManager().blockPlayerCommandsByName(targetName, System.currentTimeMillis() + BLOCK_DURATION_MS);
            } else {
                plugin.getCommandBlockManager().blockPlayerCommands(target.getUniqueId(), System.currentTimeMillis() + BLOCK_DURATION_MS);
            }
            Colors.broadcast(Colors.color("&c&lServer &8» &c" + sender.getName() + " has blocked " + targetName + "'s commands."));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission("sandbox.staff")) return out;

        if (args.length == 1) {
            out.add("all");
            out.add("purge");
            out.addAll(
                    Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(n -> n.toLowerCase(Locale.ENGLISH).startsWith(args[0].toLowerCase(Locale.ENGLISH)))
                            .sorted(String.CASE_INSENSITIVE_ORDER)
                            .collect(Collectors.toList())
            );
        }
        return out;
    }
}
