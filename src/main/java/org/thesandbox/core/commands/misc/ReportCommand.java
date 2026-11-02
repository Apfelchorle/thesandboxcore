package org.thesandbox.core.commands.misc;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.commands.CommandMessages;
import org.thesandbox.core.services.discord.DiscordBridge;
import org.thesandbox.core.util.colors.Colors;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReportCommand extends BaseCommand {
    private static final long COOLDOWN_MS = 5 * 60 * 1000L; // 5 minutes
    private final Map<String, Long> lastReport = new ConcurrentHashMap<>(); // reporterUUID:targetUUID -> time

    private final TheSandboxCore core;

    public ReportCommand(TheSandboxCore core) {
        this.core = core;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args)
    {
        if (!(sender instanceof Player reporter))
        {
            sendError(sender, Colors.RED + "Only players can use /" + label + ".");
            return true;
        }

        if (args.length < 2)
        {
            sendUsage(reporter, Colors.RED + "Usage: /" + label + " <user> <reason>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null)
        {
            sendError(reporter, Colors.RED + "That player is not online.");
            return true;
        }

        // No self-reports
        if (reporter.getUniqueId().equals(target.getUniqueId()))
        {
            sendError(reporter, "You cannot report yourself.");
            return true;
        }

        // Disallow reporting staff members
        if (target.hasPermission("sandbox.staff"))
        {
            reporter.sendMessage(org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.ERROR, "You cannot report a staff member. If you need to report a staff member, " +
                    "open a \"Staff Report\" ticket in our Discord server. https://discord.the-sandbox.org"));
            return true;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
        if (reason.isEmpty())
        {
            sendError(reporter, Colors.RED + "Please provide a reason.");
            return true;
        }

        // Cooldown: same reporter->target within 5 minutes
        String key = reporter.getUniqueId() + ":" + target.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastReport.get(key);
        if (last != null && (now - last) < COOLDOWN_MS) {
            sendError(reporter, "You have already submitted a report on this player.");
            return true;
        }
        lastReport.put(key, now);

        // Acknowledge to reporter
        sendCommand(reporter, "Your report has been logged. Thank you for keeping our server safe.");

        // Prepare details for in-game notice + Discord embed
        Location loc = target.getLocation();

        // Send to Discord as an embed
        DiscordBridge bridge = core.getDiscord();
        if (bridge != null && bridge.isReady())
        {
            bridge.sendReportEmbed(reporter.getName(), target.getName(), reason, loc);
        }

        // Build rich in-game staff message with clickable names that run /tpo <name>
        Component full = Component.text()
                .append(Colors.component("&c&lServer &8» &c"))
                .append(clickableTeleport(reporter.getName()))
                .append(Colors.component(" §creported "))
                .append(clickableTeleport(target.getName()))
                .append(Colors.component(" §cfor: "))
                .append(Component.text(reason, NamedTextColor.RED))
                .build();

        // Send to online staff (sandbox.staff)
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (core.getShushService() != null && core.getShushService().isEnabled(viewer.getUniqueId())) continue;
            if (viewer.hasPermission("sandbox.staff")) {
                viewer.sendMessage(full);
            }
        }

        // Log to console
        Bukkit.getConsoleSender().sendMessage(org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.SERVER, reporter.getName() + " reported " + target.getName()
                + " for: (" + reason + ")."));

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        if (args.length == 1)
        {
            String partial = args[0].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(partial))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }
        return List.of();
    }

    /* ================= helpers ================= */

    private Component clickableTeleport(String playerName)
    {
        return Component.text(playerName, NamedTextColor.RED)
                .hoverEvent(HoverEvent.showText(Component.text("Click to Teleport", NamedTextColor.YELLOW)))
                .clickEvent(ClickEvent.runCommand("/tpo " + playerName));
    }
}
