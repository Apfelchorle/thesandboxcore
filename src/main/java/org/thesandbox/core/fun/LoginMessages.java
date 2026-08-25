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
        return datalistener.get(player.getUniqueId(), PlayerDataKeys.LOGIN_MESSAGES_STATE, "not_owned");
    }

    public void SetLoginMessagesState(Player player, String state) {
        datalistener.set(player.getUniqueId(), PlayerDataKeys.LOGIN_MESSAGES_STATE, state);
    }

    public String GetLoginMessage(Player player) {
        String loginmessages_status = GetLoginMessagesState(player);
        String msg = datalistener.get(player.getUniqueId(), PlayerDataKeys.LOGIN_MESSAGE,"");

        if (!loginmessages_status.equalsIgnoreCase("not_owned")) {
            return msg;
        }
        return "";
    }

    public void SetLoginMessage(Player player, String message) {
        String loginmessages_status = datalistener.get(player.getUniqueId(), PlayerDataKeys.LOGIN_MESSAGES_STATE,"not_owned");

        if (loginmessages_status.equals("owned") || loginmessages_status.equalsIgnoreCase("enabled")) {
            datalistener.set(player.getUniqueId(),PlayerDataKeys.LOGIN_MESSAGE,message);
        }
    }

    public void SendLoginMessage(Player player) {
        String message = GetLoginMessage(player);
        if (message != null && !message.isEmpty()) {
            Bukkit.broadcast(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String state = GetLoginMessagesState(player);

        if (state.equalsIgnoreCase("not_owned") || state.equalsIgnoreCase("disabled")) {
            return;
        }
        SendLoginMessage(player);
    }
}
