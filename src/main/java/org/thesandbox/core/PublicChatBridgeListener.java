package org.thesandbox.core;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.entity.Player;

public class PublicChatBridgeListener implements Listener
{
    private final TheSandboxCore plugin;
    private final DiscordBridge bridge;

    public PublicChatBridgeListener(TheSandboxCore plugin, DiscordBridge bridge) {
        this.plugin = plugin;
        this.bridge = bridge;
    }

    // IMPORTANT: ignoreCancelled = true so plugins like ChatReaction can cancel chat before it mirrors to Discord
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (bridge == null || !bridge.isReady()) return;

        Player p = event.getPlayer();
        String msg = event.getMessage();

        // Mirror to Discord (does not modify your in-game chat)
        bridge.sendPublicMessageFromMinecraft(p, msg);
    }
}