package org.thesandbox.core.commands.utility;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.commands.CommandCenter;
import org.thesandbox.core.commands.CommandMessages;
import org.thesandbox.core.fun.Utils;
import org.thesandbox.core.login.LoginService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * /slowrestart [time] [reason...] | cancel | status
 *
 * Schedules a server restart with a visible countdown. Everything is configurable:
 * <pre>
 * slowrestart:
 *   default-seconds: 10   # used when no time is given
 *   max-seconds: 3600     # hard upper bound for the time argument
 * </pre>
 * Time accepts plain seconds ("90") or h/m/s units ("5m", "1h30m", "2m30s").
 */
public class SlowRestartCommand extends BaseCommand {

    private static final int FALLBACK_DEFAULT_SECONDS = 10;
    private static final int FALLBACK_MAX_SECONDS = 3600;

    /** Seconds-remaining values at which a countdown line is broadcast. */
    private static final int[] MILESTONES = {
        3600, 1800, 900, 600, 300, 120, 60, 30, 20, 10, 5, 4, 3, 2, 1,
    };

    private static final Pattern DURATION = Pattern.compile(
        "^(?:(\\d{1,5})h)?(?:(\\d{1,6})m)?(?:(\\d{1,9})s?)?$",
        Pattern.CASE_INSENSITIVE
    );

    private final TheSandboxCore plugin;

    private BukkitTask countdown;
    private int remaining;
    private String reason = "";
    private String initiator = "";

    public SlowRestartCommand(TheSandboxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(
        CommandSender sender,
        Command command,
        String label,
        String[] args
    ) {
        if (
            !CommandCenter.request(command, sender, LoginService.Rank.OPERATOR)
        ) {
            return true;
        }

        if (args.length > 0) {
            String first = args[0].toLowerCase(Locale.ROOT);
            if (
                first.equals("cancel") ||
                first.equals("abort") ||
                first.equals("stop")
            ) {
                return cancel(sender);
            }
            if (first.equals("status") || first.equals("info")) {
                return status(sender);
            }
        }
        return schedule(sender, label, args);
    }

    // ------------------------------------------------------------------ //

    private boolean schedule(CommandSender sender, String label, String[] args) {
        if (isScheduled()) {
            sendError(
                sender,
                "A restart is already scheduled (" +
                    formatDuration(remaining) +
                    " left). Use /" +
                    label +
                    " cancel first."
            );
            return true;
        }

        int max = Math.max(
            1,
            plugin.getConfig().getInt("slowrestart.max-seconds", FALLBACK_MAX_SECONDS)
        );
        int seconds = Math.min(
            max,
            Math.max(
                1,
                plugin.getConfig().getInt("slowrestart.default-seconds", FALLBACK_DEFAULT_SECONDS)
            )
        );
        int reasonStart = 0;

        if (args.length > 0) {
            Integer parsed = parseDuration(args[0]);
            if (parsed != null) {
                seconds = parsed;
                reasonStart = 1;
            } else if (Character.isDigit(args[0].charAt(0))) {
                // Looks like a time but isn't valid (e.g. "10x") – don't silently treat it as a reason.
                sendError(
                    sender,
                    "\"" + args[0] + "\" is not a valid time. Examples: 30, 90s, 5m, 1h30m."
                );
                return true;
            }
        }

        if (seconds < 1 || seconds > max) {
            sendError(
                sender,
                "Time must be between 1 second and " + formatDuration(max) + "."
            );
            return true;
        }

        String rawReason = String.join(
            " ",
            Arrays.copyOfRange(args, reasonStart, args.length)
        ).trim();
        if (rawReason.length() > 200) {
            rawReason = rawReason.substring(0, 200);
        }

        this.remaining = seconds;
        this.reason = Utils.stripAllColors(rawReason);
        this.initiator = (sender instanceof Player) ? sender.getName() : "Console";

        broadcast(
            initiator +
                " - Server restarting in " +
                formatDuration(remaining) +
                (reason.isEmpty() ? "!" : ": " + reason)
        );
        Utils.blog(
            "Restart scheduled",
            "By : " + initiator,
            "In : " + formatDuration(remaining),
            "Reason : " + (reason.isEmpty() ? "-" : reason)
        );

        countdown = Bukkit.getScheduler().runTaskTimer(
            plugin,
            this::tick,
            20L,
            20L
        );
        return true;
    }

    private boolean cancel(CommandSender sender) {
        if (!isScheduled()) {
            sendError(sender, "No restart is currently scheduled.");
            return true;
        }
        String who = (sender instanceof Player) ? sender.getName() : "Console";
        stopTask();
        broadcast(who + " - Cancelled the scheduled server restart.");
        Utils.blog("Restart cancelled", "By : " + who);
        return true;
    }

    private boolean status(CommandSender sender) {
        if (!isScheduled()) {
            sendCommand(sender, "No restart is scheduled.");
        } else {
            sendCommand(
                sender,
                "Restart in " +
                    formatDuration(remaining) +
                    " (scheduled by " +
                    initiator +
                    (reason.isEmpty() ? "" : ", reason: " + reason) +
                    ")."
            );
        }
        return true;
    }

    // ------------------------------------------------------------------ //

    private void tick() {
        remaining--;

        if (remaining <= 0) {
            stopTask();
            broadcast("Restarting now!");
            // Run on the next tick so the message is flushed before the server shuts down.
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    Bukkit.restart();
                } catch (Throwable t) {
                    Utils.dump(t, "Scheduled restart failed");
                    broadcast("Restart failed - check the console.");
                }
            });
            return;
        }

        if (isMilestone(remaining)) {
            broadcast("Server restarting in " + formatDuration(remaining) + "!");
            if (remaining <= 5) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                }
            }
        }
    }

    private boolean isScheduled() {
        return countdown != null && !countdown.isCancelled();
    }

    private void stopTask() {
        if (countdown != null) {
            countdown.cancel();
            countdown = null;
        }
        remaining = 0;
    }

    private static boolean isMilestone(int seconds) {
        for (int m : MILESTONES) {
            if (m == seconds) return true;
        }
        return false;
    }

    private static void broadcast(String message) {
        Bukkit.broadcast(
            LegacyComponentSerializer.legacySection().deserialize(
                CommandMessages.server(message)
            )
        );
    }

    // ------------------------------------------------------------------ //

    /** "90" -> 90, "5m" -> 300, "1h30m" -> 5400. Returns null when the text isn't a duration. */
    static Integer parseDuration(String text) {
        if (text == null || text.isEmpty()) return null;
        Matcher m = DURATION.matcher(text);
        if (!m.matches()) return null;

        long total = 0;
        if (m.group(1) != null) total += Long.parseLong(m.group(1)) * 3600L;
        if (m.group(2) != null) total += Long.parseLong(m.group(2)) * 60L;
        if (m.group(3) != null) total += Long.parseLong(m.group(3));
        return (int) Math.min(total, Integer.MAX_VALUE);
    }

    static String formatDuration(int totalSeconds) {
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;
        StringBuilder sb = new StringBuilder();
        if (h > 0) sb.append(h).append(h == 1 ? " hour " : " hours ");
        if (m > 0) sb.append(m).append(m == 1 ? " minute " : " minutes ");
        if (s > 0 || sb.length() == 0) {
            sb.append(s).append(s == 1 ? " second" : " seconds");
        }
        return sb.toString().trim();
    }

    @Override
    public List<String> tabComplete(
        CommandSender sender,
        Command command,
        String alias,
        String[] args
    ) {
        if (args.length != 1) return List.of();

        List<String> options = new ArrayList<>();
        if (isScheduled()) {
            options.add("cancel");
            options.add("status");
        } else {
            options.addAll(List.of("10s", "30s", "1m", "5m", "10m", "30m", "cancel", "status"));
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        options.removeIf(o -> !o.startsWith(prefix));
        return options;
    }
}
