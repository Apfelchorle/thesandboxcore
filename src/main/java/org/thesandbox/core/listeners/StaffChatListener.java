package org.thesandbox.core.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.fun.Utils;
import org.thesandbox.core.managers.CommandSpyManager;
import org.thesandbox.core.managers.LegacySqlStore;
import org.thesandbox.core.managers.StaffChatManager;

public class StaffChatListener implements Listener {

    private final TheSandboxCore plugin;
    private final StaffChatManager staffChatManager;
    private final CommandSpyManager commandSpyManager;
    private final LegacySqlStore legacySqlStore;

    public StaffChatListener(
        TheSandboxCore plugin,
        StaffChatManager staffChatManager,
        CommandSpyManager commandSpyManager,
        LegacySqlStore legacySqlStore
    ) {
        this.plugin = plugin;
        this.staffChatManager = staffChatManager;
        this.commandSpyManager = commandSpyManager;
        this.legacySqlStore = legacySqlStore;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        loadPlayerData(event.getPlayer());
    }

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        Player p = event.getPlayer();
        if (staffChatManager.isStaffChatEnabled(p.getUniqueId())) {
            event.setCancelled(true);

            if (staffChatManager.isStaffChatHidden(p.getUniqueId())) {
                p.sendMessage(
                    Component.text(
                        "You cannot talk in staff chat while it is hidden. Use /staffchat hide off.",
                        NamedTextColor.DARK_RED
                    )
                );
                return;
            }

            String name = p.getName();
            String msg = Utils.plainText(event.message());

            Bukkit.getScheduler().runTask(plugin, () ->
                staffChatManager.broadcastStaffChat(
                    name,
                    msg,
                    "",
                    "",
                    StaffChatManager.Source.MINECRAFT
                )
            );
        }
    }

    private void loadPlayerData(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            javax.sql.DataSource dataSource = legacySqlStore.getDataSource();
            if (dataSource == null) return;

            try (Connection conn = dataSource.getConnection()) {
                // CommandSpy
                try (
                    PreparedStatement ps = conn.prepareStatement(
                        "SELECT enabled FROM commandspy WHERE uuid=?"
                    )
                ) {
                    ps.setString(1, player.getUniqueId().toString());
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        commandSpyManager.applyLoadedState(
                            player.getUniqueId(),
                            rs.getBoolean("enabled")
                        );
                    } else {
                        try (
                            PreparedStatement ins = conn.prepareStatement(
                                "INSERT INTO commandspy (uuid, enabled) VALUES (?, ?)"
                            )
                        ) {
                            ins.setString(1, player.getUniqueId().toString());
                            ins.setBoolean(2, true);
                            ins.executeUpdate();
                        }
                    }
                }

                // StaffChat (speak + hidden)
                try (
                    PreparedStatement ps = conn.prepareStatement(
                        "SELECT enabled, hidden FROM staffchat WHERE uuid=?"
                    )
                ) {
                    ps.setString(1, player.getUniqueId().toString());
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        boolean enabled = rs.getBoolean("enabled");
                        boolean hidden = false;
                        try {
                            hidden = rs.getBoolean("hidden");
                        } catch (SQLException ignore) {}
                        staffChatManager.applyLoadedState(
                            player.getUniqueId(),
                            enabled,
                            hidden
                        );
                    } else {
                        try (
                            PreparedStatement ins = conn.prepareStatement(
                                "INSERT INTO staffchat (uuid, enabled, hidden) VALUES (?, ?, ?)"
                            )
                        ) {
                            ins.setString(1, player.getUniqueId().toString());
                            ins.setBoolean(2, false);
                            ins.setBoolean(3, false);
                            ins.executeUpdate();
                        } catch (SQLException fallback) {
                            try (
                                PreparedStatement ins2 = conn.prepareStatement(
                                    "INSERT INTO staffchat (uuid, enabled) VALUES (?, ?)"
                                )
                            ) {
                                ins2.setString(
                                    1,
                                    player.getUniqueId().toString()
                                );
                                ins2.setBoolean(2, false);
                                ins2.executeUpdate();
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                Utils.dump(e, "SQLException");
            }
        });
    }
}
