package org.thesandbox.core.fun;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.util.PlayerDataKeys;
import org.thesandbox.core.util.PlayerDataListener;

public class LoginMessages implements Listener {

    private final PlayerDataListener datalistener;
    private final TheSandboxCore plugin;
    public LoginMessages(PlayerDataListener datalistener, TheSandboxCore plugin) {
        this.datalistener = datalistener;
        this.plugin = plugin;
    }

    public String GetLoginMessagesState(Player player) {
        String state = datalistener.get(player.getUniqueId(), PlayerDataKeys.LOGIN_MESSAGES_STATE, "not_owned");
        return state;
    }

    public String GetLoginMessage(Player player) {
        String loginmessages_status = datalistener.get(player.getUniqueId(), "LoginMessages","not_owned");
        String msg = datalistener.get(player.getUniqueId(),"LoginMessage","");

        if (loginmessages_status.equals("owned")) {
            return msg;
        }
        return loginmessages_status;
    }

    public void SetLoginMessage(Player player, String message) {
        String loginmessages_status = datalistener.get(player.getUniqueId(), "LoginMessages","not_owned");

        if (loginmessages_status.equals("owned")) {
            datalistener.set(player.getUniqueId(),"LoginMessage",message);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String state = GetLoginMessagesState(player);

        if (state.equalsIgnoreCase("not_owned") || state.equalsIgnoreCase("disabled")) {
            return;
        }

        String message = GetLoginMessage(player);
        if (message.isEmpty()) {
            return; // enabled, but nothing set yet — nothing to broadcast
        }

        Bukkit.broadcast(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
    }
}
