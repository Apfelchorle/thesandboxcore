package org.thesandbox.core.fun;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.login.LoginService;
import org.thesandbox.core.util.PlayerDataKeys;
import org.thesandbox.core.util.PlayerDataListener;

public class LoginMessages implements Listener {

    private final PlayerDataListener datalistener;
    private final TheSandboxCore plugin;
    public LoginMessages(PlayerDataListener datalistener, TheSandboxCore plugin) {
        this.datalistener = datalistener;
        this.plugin = plugin;
    }

    private static String RankToString(LoginService.Rank r) {
            if (r == null) return "";
            return switch (r) {
                case OPERATOR -> "Operator";
                case ADMIN    -> "Admin";
                case STAFF    -> "Staff";
                case MB       -> "MasterBuilder";
                case VIP      -> "Very Important Person";
                default       -> "Op"; // for Fake Ops [sandbox.default]
            };
    }

    public String GetRankPrefix(Player player) {
        LoginService.Rank rank = LoginService.Rank.DEFAULT;
        try {
            if (plugin.getLoginService() != null) {
                rank = plugin.getLoginService().getRank(player);
            }
        } catch (Throwable ignored) {
            // Fall back to direct permission checks if LoginService is unavailable.
            if (player != null) {
                if (player.hasPermission("sandbox.operator")) rank = LoginService.Rank.OPERATOR;
                else if (player.hasPermission("sandbox.admin")) rank = LoginService.Rank.ADMIN;
                else if (player.hasPermission("sandbox.staff")) rank = LoginService.Rank.STAFF;
                else if (player.hasPermission("sandbox.mb")) rank = LoginService.Rank.MB;
                else if (player.hasPermission("sandbox.vip")) rank = LoginService.Rank.VIP;
            }
        }
        return RankToString(rank);
    }

    public String FormatLoginMessage(String msg, Player player) {
        String name = player.getName();


        String out = msg
                .replace("%name%", name)
                .replace("%rank%", GetRankPrefix(player));

        return out;
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
            message = FormatLoginMessage(message, player);
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
