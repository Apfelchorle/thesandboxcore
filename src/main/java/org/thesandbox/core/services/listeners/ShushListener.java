package org.thesandbox.core.services.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.util.colors.Colors;

public class ShushListener implements Listener {

    private final TheSandboxCore plugin;

    public ShushListener(TheSandboxCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        if (plugin.getShushService() != null && plugin.getShushService().isEnabled(p.getUniqueId())) {
            p.sendMessage(Colors.RED + "Shush mode is enabled, you will not receive staff notifications.");
        }
    }
}
