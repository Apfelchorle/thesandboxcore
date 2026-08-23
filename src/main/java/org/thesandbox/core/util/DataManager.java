package org.thesandbox.core.util;

import com.google.protobuf.Any;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.thesandbox.core.TheSandboxCore;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class DataManager {

    private final TheSandboxCore plugin;
    private final File playerFolder;

    public DataManager(TheSandboxCore plugin) {
        this.plugin = plugin;
        this.playerFolder = new File(plugin.getDataFolder(), "player_data");
        if (!playerFolder.exists()) {
            playerFolder.mkdirs();
        }
    }

    // load
    public int loadData(UUID uuid, String data) {
        File file = new File(playerFolder, uuid.toString() + ".yml");
        if (!file.exists()) return 0; // New player gets 0 coins

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        return config.getInt(data, 0);
    }

    // Save
    public void saveData(UUID uuid, String data, int value) {
        File file = new File(playerFolder, uuid.toString() + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        config.set(data, value);

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save data for " + uuid);
        }
    }
}
