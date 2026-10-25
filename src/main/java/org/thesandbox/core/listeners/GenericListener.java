package org.thesandbox.core.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.fun.Utils;
import org.thesandbox.core.util.Configinator;
import org.thesandbox.core.util.GenericDataKeys;
import org.thesandbox.core.util.PlayerDataKeys;
import org.thesandbox.core.util.PlayerDataListener;
import org.thesandbox.core.util.PluginConfigManager;

public class GenericListener implements Listener {

    private final PluginConfigManager pluginConfigManager;
    private TheSandboxCore core = TheSandboxCore.getInstance();
    private PlayerDataListener pdl = core.getDataListener();

    public GenericListener(PluginConfigManager pluginConfigManager) {
        this.pluginConfigManager = pluginConfigManager;
    }

    @EventHandler
    public void BlockFromTo(BlockFromToEvent event) {
        Material material = event.getBlock().getType();
        boolean config = pluginConfigManager.getOrCreate(
            GenericDataKeys.WATER_FLOW,
            GenericDataKeys.WATER_FLOW_DEFAULT
        );
        boolean target =
            material == Material.WATER || material == Material.LAVA;

        if (target && config) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        boolean join_state = pdl.get(
            p.getUniqueId(),
            PlayerDataKeys.FIRST_TIME_JOINING,
            true
        );
        if (join_state) {
            Configinator.wellcomeParty(join_state);
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        boolean join_state = pdl.get(
            p.getUniqueId(),
            PlayerDataKeys.FIRST_TIME_JOINING,
            true
        );
        if (join_state) {
            pdl.set(p.getUniqueId(), PlayerDataKeys.FIRST_TIME_JOINING, false);
            Configinator.wellcomeParty(join_state);
        }
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player p = event.getPlayer();
        if (Configinator.getWellcomePartyState()) {
            pdl.addCoins(p.getUniqueId(), 15);
            Utils.SendMessage(
                p,
                "You've Recieved 15 Coins!",
                NamedTextColor.GOLD
            );
        }
    }
}
