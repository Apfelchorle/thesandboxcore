package org.thesandbox.core.services.login;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.data.PlayerDataKeys;
import org.thesandbox.core.data.PlayerDataListener;
import org.thesandbox.core.util.colors.Colors;

public class LoginMessages implements Listener {

    private final PlayerDataListener datalistener;
    private final TheSandboxCore plugin;

    public LoginMessages(PlayerDataListener datalistener, TheSandboxCore plugin) {
        this.datalistener = datalistener;
        this.plugin = plugin;
    }

    public void SetLoginMessagesState(Player player, String state) {
        datalistener.set(player.getUniqueId(), PlayerDataKeys.LOGIN_MESSAGES_STATE, state);
    }

    /**
     * Stores a player's login message.
     *
     * @return {@code false} if the player does not own the Login Messages item,
     * {@code true} once the message has been stored.
     */
    public boolean SetLoginMessage(Player player, String message) {
        String state = GetLoginMessagesState(player);

        // Disabled messages can still be edited; only ownership is required.
        if ("not_owned".equalsIgnoreCase(state)) {
            return false;
        }

        datalistener.set(player.getUniqueId(), PlayerDataKeys.LOGIN_MESSAGE, message);
        return true;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String state = GetLoginMessagesState(player);

        if ("not_owned".equalsIgnoreCase(state) || "disabled".equalsIgnoreCase(state)) {
            return;
        }
        SendLoginMessage(player);
    }

    public String GetLoginMessagesState(Player player) {
        return datalistener.get(player.getUniqueId(), PlayerDataKeys.LOGIN_MESSAGES_STATE, "not_owned");
    }

    public void SendLoginMessage(Player player) {
        String message = GetLoginMessage(player);
        if (message != null && !message.isEmpty()) {
            message = FormatLoginMessage(message, player);
            Bukkit.broadcast(Colors.component(message));
        }
    }

    public String GetLoginMessage(Player player) {
        String loginmessages_status = GetLoginMessagesState(player);
        String msg = datalistener.get(player.getUniqueId(), PlayerDataKeys.LOGIN_MESSAGE, "");

        if (!"not_owned".equalsIgnoreCase(loginmessages_status)) {
            return msg == null ? "" : msg;
        }
        return "";
    }

    public String FormatLoginMessage(String msg, Player player) {
        String name = player.getName();


        String out = msg
                .replace("%name%", name)
                .replace("%rank%", GetRankPrefix(player));

        return out;
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

    private static String RankToString(LoginService.Rank r) {
        if (r == null) return "";
        return switch (r) {
            case OPERATOR -> "Operator";
            case ADMIN -> "Admin";
            case STAFF -> "Staff";
            case MB -> "MasterBuilder";
            case VIP -> "Very Important Person";
            default -> "Op"; // for Fake Ops [sandbox.default]
        };
    }
}
