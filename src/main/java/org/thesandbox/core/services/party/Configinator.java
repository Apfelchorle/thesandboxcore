package org.thesandbox.core.services.party;

import java.io.File;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.util.Utils;
import org.thesandbox.core.util.config.PluginConfigManager;

public class Configinator {

    private static final Set<UUID> rewardedPlayers =
            ConcurrentHashMap.newKeySet();
    private static volatile boolean welcomePartyState = false;
    private static File configFile;

    public static void startWelcomeParty() {
        welcomePartyState = true;
        rewardedPlayers.clear(); // Reset for the new party!

        PluginConfigManager manager = getConfigManager();
        File file = getConfigFile();
        if (manager != null && file != null) {
            manager.safeSet("WelcomeParty", true, file);
        }
        Utils.log("Welcome Party has started!");
    }

    private static PluginConfigManager getConfigManager() {
        TheSandboxCore core = TheSandboxCore.getInstance();
        return core != null ? core.getPluginConfigManager() : null;
    }

    private static File getConfigFile() {
        if (configFile == null) {
            PluginConfigManager manager = getConfigManager();
            if (manager != null) {
                configFile = manager.createFile("party.yml");
            }
        }
        return configFile;
    }

    public static void endWelcomeParty() {
        welcomePartyState = false;
        rewardedPlayers.clear();

        PluginConfigManager manager = getConfigManager();
        File file = getConfigFile();
        if (manager != null && file != null) {
            manager.safeSet("WelcomeParty", false, file);
        }
        Utils.log("Welcome Party has ended!");
    }

    public static boolean tryRewardPlayer(UUID uuid) {
        if (!welcomePartyState) return false;
        return rewardedPlayers.add(uuid);
    }

}
