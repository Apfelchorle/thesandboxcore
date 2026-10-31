package org.thesandbox.core.services.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.thesandbox.core.services.chat.ManageChatService;
import org.thesandbox.core.util.colors.Colors;

/**
 * Cancels chat while muted and informs the speaker in red.
 * Runs early so ChatMentionFormatListener sees the cancelled event and does not
 * rebroadcast the message.
 */
public class ManageChatListener implements Listener {
    private final ManageChatService service;

    public ManageChatListener(ManageChatService service) {
        this.service = service;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent e) {
        if (!service.isMuted()) return;

        e.setCancelled(true);
        e.getPlayer().sendMessage(Colors.component(Colors.RED + "A staff member has muted the chat. You cannot speak at this time."));
    }
}
