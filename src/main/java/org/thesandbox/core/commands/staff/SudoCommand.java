package org.thesandbox.core.commands.staff;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.util.colors.Colors;
import org.thesandbox.core.util.permissions.PermissionTier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SudoCommand extends BaseCommand {
    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sendUsage(sender, Colors.RED + "Usage: /" + label + " <player> <command>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sendError(sender, Colors.RED + "Player not found: " + args[0]);
            return true;
        }

        // Permission hierarchy enforcement
        if (!canControl(sender, target)) {
            sendError(sender, Colors.RED + "You cannot send commands as a player of equal or higher rank!");
            return true;
        }

        // Build the command string (strip any leading slash if provided)
        String cmd = joinArgs(args);
        while (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }
        if (cmd.isEmpty()) {
            sendError(sender, Colors.RED + "You must specify a command to run.");
            return true;
        }

        boolean ok = Bukkit.dispatchCommand(target, cmd);
        if (ok) {
            sendCommand(sender, Colors.GRAY + "Successfully sent the following command: \"" + cmd + "\" as " + target.getName());
        } else {
            sendError(sender, Colors.RED + "Failed to run command as " + target.getName() + ".");
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> out = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(prefix) && canControl(sender, p)) {
                    out.add(p.getName());
                }
            }
            Collections.sort(out);
            return out;
        }
        return Collections.emptyList();
    }

    /* ===================== helpers ===================== */

    /**
     * Returns true if 'actor' is allowed to sudo 'target' according to the role rules.
     * Console is treated as the highest level.
     */
    private static boolean canControl(CommandSender actor, Player target) {
        int actorLevel = roleLevel(actor);
        int targetLevel = roleLevel(target);

        // Console (or non-player senders) can control anyone
        if (!(actor instanceof Player ap)) return true;

        // You can always run commands as yourself, but never as an equal or higher level
        // (otherwise staff could impersonate each other).
        if (ap.getUniqueId().equals(target.getUniqueId())) return true;
        return actorLevel > targetLevel;
    }

    private static String joinArgs(String[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < arr.length; i++) {
            if (i > 1) sb.append(' ');
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    /**
     * Role levels come from {@link PermissionTier}. Non-player senders are
     * treated as console, above every player tier.
     */
    private static int roleLevel(CommandSender who) {
        if (!(who instanceof Player)) return PermissionTier.SUPERUSER.weight() + 1;
        return PermissionTier.levelOf(who);
    }
}
