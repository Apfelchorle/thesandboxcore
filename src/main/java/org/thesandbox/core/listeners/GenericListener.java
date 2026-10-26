package org.thesandbox.core.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent.Payload.Custom;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.fun.Utils;
import org.thesandbox.core.util.Configinator;
import org.thesandbox.core.util.CustomPlayer;
import org.thesandbox.core.util.GenericDataKeys;
import org.thesandbox.core.util.PlayerDataKeys;
import org.thesandbox.core.util.PlayerDataListener;
import org.thesandbox.core.util.PluginConfigManager;

public class GenericListener implements Listener {

    private boolean waterFlowBlocked = false;

    private final PluginConfigManager pluginConfigManager;
    private TheSandboxCore core = TheSandboxCore.getInstance();
    private PlayerDataListener pdl = core.getDataListener();

    public GenericListener(PluginConfigManager pluginConfigManager) {
        this.pluginConfigManager = pluginConfigManager;
    }

    @EventHandler
    public void BlockFromTo(BlockFromToEvent event) {
        if (!waterFlowBlocked) return;
        Material material = event.getBlock().getType();
        if (material == Material.WATER || material == Material.LAVA) {
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
            pdl.set(p.getUniqueId(), PlayerDataKeys.FIRST_TIME_JOINING, false);
            Configinator.startWelcomeParty();
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
            Configinator.endWelcomeParty();
        }
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player p = event.getPlayer();
        CustomPlayer cPlayer = new CustomPlayer(p);

        if (Configinator.tryRewardPlayer(p.getUniqueId())) {
            pdl.addCoins(p.getUniqueId(), 15);
            Utils.Feedback(
                p,
                "You've Recieved 15 Coins!",
                NamedTextColor.GOLD,
                Sound.ENTITY_FIREWORK_ROCKET_LAUNCH
            );
        }
    }
}
