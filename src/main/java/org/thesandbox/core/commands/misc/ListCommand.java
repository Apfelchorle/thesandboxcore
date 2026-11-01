package org.thesandbox.core.commands.misc;

import de.myzelyam.api.vanish.VanishAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.services.login.LoginService;
import org.thesandbox.core.util.colors.Colors;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class ListCommand extends BaseCommand {
    private final TheSandboxCore core;
    private final LoginService login;

    // Staff vs Player buckets as defined in your LoginService.Rank
    private static final EnumSet<LoginService.Rank> STAFF_RANKS = EnumSet.of(
            LoginService.Rank.OPERATOR,
            LoginService.Rank.ADMIN,
            LoginService.Rank.STAFF
    );

    public ListCommand(TheSandboxCore core) {
        this.core = core;
        this.login = core.getLoginService();
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args)
    {
        final boolean viewerIsStaff = sender.hasPermission("sandbox.staff");

        List<String> staffLines  = new ArrayList<>();
        List<String> playerLines = new ArrayList<>();

        for (Player p : Bukkit.getOnlinePlayers())
        {
            boolean vanished = isVanished(p);

            // Hide vanished players from non-staff viewers
            if (vanished && !viewerIsStaff) {
                continue;
            }

            LoginService.Rank rank = login.getRank(p);
            String entry = formatEntry(p, rank, vanished && viewerIsStaff);

            if (STAFF_RANKS.contains(rank)) {
                staffLines.add(entry);
            } else { // MB / DEFAULT (and any future non-staff rank)
                playerLines.add(entry);
            }
        }

        int totalShown = staffLines.size() + playerLines.size();

        // Header
        send(sender, "&8&m--------------------&r &6Online (" + totalShown + ") &8&m--------------------");

        // Staff section (Operator, Admin, Staff)
        send(sender, ("&eStaff &7- &e" + staffLines.size()));
        if (staffLines.isEmpty()) {
            send(sender, ("&7- &7No staff online."));
        } else {
            send(sender, ("&8- " + String.join(color("&7, "), staffLines)));
        }

        // Players section (Master Builder, VIP, Player)
        send(sender, ("&ePlayers &7- &e" + playerLines.size()));
        if (playerLines.isEmpty()) {
            send(sender, ("&7- &7No players online."));
        } else {
            send(sender, ("&7- " + String.join(color("&7, "), playerLines)));
        }

        // Footer
        send(sender, ("&7&m-----------------------------------------------------"));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        return List.of(); // /list has no args
    }

    /* ================= helpers ================= */

    private String formatEntry(Player p, LoginService.Rank rank, boolean showVanishedTag)
    {
        String vanished = showVanishedTag ? color(" &e(VANISHED)") : "";
        String name = p.getName();

        // Match the same rank prefix style used for LuckPerms/chat and scoreboard teams.
        // Operator is intentionally displayed as OP here.
        return switch (rank == null ? LoginService.Rank.DEFAULT : rank) {
            case OPERATOR -> color("&4&lOP &8• &4" + name) + Colors.RESET + vanished;
            case ADMIN -> color("&c&lADMIN &8• &c" + name) + Colors.RESET + vanished;
            case STAFF -> color("&6&lSTAFF &8• &6" + name) + Colors.RESET + vanished;
            case MB -> color("&3&lMB &8• &3" + name) + Colors.RESET + vanished;
            case VIP -> color("&5&lVIP &8• &5" + name) + Colors.RESET + vanished;
            case DEFAULT -> color("&7" + name) + Colors.RESET + vanished;
        };
    }


    private boolean isVanished(Player p)
    {
        try {
            return VanishAPI.isInvisible(p);
        } catch (Throwable t) {
            // PremiumVanish not present or API not available -> treat as not vanished
            return false;
        }
    }

    private static String color(String s) {
        return Colors.color(s);
    }
}
