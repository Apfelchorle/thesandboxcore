package org.thesandbox.core.services.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.thesandbox.core.services.discord.DiscordBridge;
import org.thesandbox.core.services.moderation.CommandBlockManager;
import org.thesandbox.core.services.moderation.CommandSpyManager;
import org.thesandbox.core.services.chat.ShushService;
import org.thesandbox.core.util.colors.Colors;
import org.thesandbox.core.util.permissions.PermissionTier;

import java.util.function.Supplier;

public class CommandPreListener implements Listener {

    private final CommandBlockManager commandBlockManager;
    private final CommandSpyManager commandSpyManager;
    private final Supplier<ShushService> shushServiceSupplier;
    private final Supplier<DiscordBridge> discordSupplier;
    private final org.thesandbox.core.TheSandboxCore plugin;

    public CommandPreListener(org.thesandbox.core.TheSandboxCore plugin,
                              CommandBlockManager commandBlockManager,
                              CommandSpyManager commandSpyManager,
                              Supplier<ShushService> shushServiceSupplier,
                              Supplier<DiscordBridge> discordSupplier) {
        this.plugin = plugin;
        this.commandBlockManager = commandBlockManager;
        this.commandSpyManager = commandSpyManager;
        this.shushServiceSupplier = shushServiceSupplier;
        this.discordSupplier = discordSupplier;
    }

    /**
     * Runs first so a blocked player's command is canceled before other plugins see it.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommandBlock(PlayerCommandPreprocessEvent event) {
        final Player sender = event.getPlayer();
        if (commandBlockManager.isCommandsBlocked(sender)) {
            sender.sendMessage(Colors.RED + "Your commands are currently blocked.");
            event.setCancelled(true);
        }
    }

    /**
     * Command spy runs at MONITOR so it only reports commands that actually went through
     * (before, commands canceled by other plugins were still broadcast to staff).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommandPre(PlayerCommandPreprocessEvent event) {
        final Player sender = event.getPlayer();
        final ShushService shushService = shushServiceSupplier.get();

        final String rawMessage = event.getMessage();

        final String spyCommand = commandSpyManager.spyDisplayCommand(rawMessage);
        String fmt = plugin.getConfig().getString("commandspy.format",
                plugin.getConfig().getString("commandspy-format", "&7%player%: %command%"));
        String formatted = Colors.color(fmt)
                .replace("%player%", sender.getName())
                .replace("%command%", spyCommand);

        PermissionTier senderTier = PermissionTier.of(sender);
        for (Player viewer : sender.getServer().getOnlinePlayers()) {
            if (!viewer.hasPermission("sandbox.staff")) continue;
            if (shushService != null && shushService.isEnabled(viewer.getUniqueId())) continue;
            if (commandSpyManager.isCmdSpyDisabled(viewer.getUniqueId())) continue;
            if (viewer == sender) continue;
            PermissionTier viewerTier = PermissionTier.of(viewer);
            if (senderTier.weight() <= viewerTier.weight()) {
                viewer.sendMessage(formatted);
            }
        }

        try {
            String raw = rawMessage.trim();
            if (raw.startsWith("/")) raw = raw.substring(1);
            String[] parts = raw.split("\\s+");
            if (parts.length >= 2 && parts[0].equalsIgnoreCase("tpo")) {
                String clickedName = parts[1];
                DiscordBridge bridge = discordSupplier.get();
                if (bridge != null && bridge.isReady()) {
                    bridge.tryArchiveRecentForPlayer(clickedName, sender.getName());
                }
            }
        } catch (Exception ignore) {
        }
    }
}
