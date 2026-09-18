package org.thesandbox.core.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.thesandbox.core.TheSandboxCore;

public class PluginConfigManager {
    private final TheSandboxCore plugin;

    public PluginConfigManager(TheSandboxCore plugin) {
        this.plugin = plugin;
    }

    public <T> T getOrCreate(String key, T defaultValue) {
        FileConfiguration config = plugin.getConfig();

        if (!config.isSet(key)) {
            config.set(key, defaultValue);
            plugin.saveConfig();
            return defaultValue;
        }

        Object raw = config.get(key);
        try {
            @SuppressWarnings("unchecked")
            T cast = (T) raw;
            return cast;
        } catch (ClassCastException e) {
            plugin.getLogger().warning("Config type mismatch for key '" + key + "', returning default.");
            return defaultValue;
        }
    }

    public <T> void safeSet(String key, T value) {
        FileConfiguration config = plugin.getConfig();
        Object existing = config.get(key);

        if (existing != null && value != null && !existing.getClass().isInstance(value)) {
            plugin.getLogger().warning("Refusing to overwrite key '" + key + "' — existing type " + existing.getClass().getSimpleName() + " doesn't match new type " + value.getClass().getSimpleName());
            return;
        }

        config.set(key, value);
        plugin.saveConfig();
    }

    public <T> void forceset(String key, T value) {
        plugin.getConfig().set(key, value);
        plugin.saveConfig();
    }
}