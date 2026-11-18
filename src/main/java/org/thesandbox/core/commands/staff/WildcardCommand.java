package org.thesandbox.core.commands.staff;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.util.commands.CommandGuard;

import java.util.*;

/**
 * /wildcard <command...>  – runs a command once for every online player.
 * Use '?' as the player-name placeholder; if absent the name is appended.
 * <p>
 * {@link CommandGuard} blocks privileged commands and command wrappers
 * ({@code execute}, {@code sudo}, ...) so the blocklist cannot be bypassed by
 * nesting.
 */
public class WildcardCommand extends BaseCommand {
    private static final long COOLDOWN_MS = 2000L;

    private final TheSandboxCore core;
    private final Map<UUID, Long> lastUse = new HashMap<>();

    public WildcardCommand(TheSandboxCore core) {
        this.core = core;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sendError(sender, "This command cannot be used from console.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender, "Usage: /" + label + " <command...> [use '?' as the player placeholder]");
            return true;
        }

        String template = String.join(" ", args).trim();
        while (template.startsWith("/")) {
            template = template.substring(1).trim();
        }
        if (template.isEmpty()) {
            sendError(sender, "Please provide a command to run.");
            return true;
        }

        String blocked = CommandGuard.blockedReason(
                template,
                core.getConfig().getStringList("wildcard.blocked-commands"),
                !sender.hasPermission("sandbox.superuser")
        );
        if (blocked != null) {
            sendError(sender, "Refusing to run /" + blocked + " through /" + label + ".");
            return true;
        }

        if (isOnCooldown(player)) {
            sendError(sender, "Please wait a moment before using /" + label + " again.");
            return true;
        }

        boolean hasPlaceholder = template.indexOf('?') >= 0;

        // Snapshot: commands like /kick modify the live online-player collection.
        List<Player> targets = new ArrayList<>(core.getServer().getOnlinePlayers());
        int ran = 0;
        for (Player target : targets) {
            if (!target.isOnline()) continue;
            String run = hasPlaceholder
                    ? template.replace("?", target.getName())
                    : template + " " + target.getName();

            sendCommand(sender, "Running: /" + run);
            Bukkit.dispatchCommand(sender, run);
            ran++;
        }

        sendCommand(sender, "Ran the command for " + ran + " player(s).");
        return true;
    }

    private boolean isOnCooldown(Player player) {
        long now = System.currentTimeMillis();
        Long last = lastUse.get(player.getUniqueId());
        if (last != null && now - last < COOLDOWN_MS) {
            return true;
        }
        lastUse.put(player.getUniqueId(), now);
        return false;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Collections.singletonList("<command>");
        }
        return Collections.emptyList();
    }
}
