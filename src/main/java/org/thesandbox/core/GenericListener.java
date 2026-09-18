package org.thesandbox.core;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFormEvent;
import org.thesandbox.core.util.GenericDataKeys;
import org.thesandbox.core.util.PluginConfigManager;

public class GenericListener implements Listener {

    private final PluginConfigManager pluginConfigManager;

    public GenericListener(PluginConfigManager pluginConfigManager) {
        this.pluginConfigManager = pluginConfigManager;
    }

    @EventHandler
    public void onBlockForm(BlockFormEvent event) {

        boolean config = pluginConfigManager.getOrCreate(GenericDataKeys.WATER_FLOW, GenericDataKeys.WATER_FLOW_DEFAULT);
        boolean target = event.getBlock().getType() == Material.WATER || event.getBlock().getType() == Material.LAVA;


        if (target && config) {
            event.setCancelled(true);
        }
    }
}
