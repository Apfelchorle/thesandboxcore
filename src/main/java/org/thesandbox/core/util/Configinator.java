package org.thesandbox.core.util;

import java.io.File;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.fun.Utils;

public class Configinator {

    private static boolean wellcomePartyState = false;
    private static File configFile;

    private static final Set<UUID> rewardedPlayers =
        ConcurrentHashMap.newKeySet();

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

    public static void startWelcomeParty() {
        wellcomePartyState = true;
        rewardedPlayers.clear(); // Reset for the new party!

        PluginConfigManager manager = getConfigManager();
        File file = getConfigFile();
        if (manager != null && file != null) {
            manager.safeSet("WellcomeParty", true, file);
        }
        Utils.log("Welcome Party has started!");
    }

    public static void endWelcomeParty() {
        wellcomePartyState = false;
        rewardedPlayers.clear();

        PluginConfigManager manager = getConfigManager();
        File file = getConfigFile();
        if (manager != null && file != null) {
            manager.safeSet("WellcomeParty", false, file);
        }
        Utils.log("Welcome Party has ended!");
    }

    public static boolean tryRewardPlayer(UUID uuid) {
        if (!wellcomePartyState) return false;
        return rewardedPlayers.add(uuid);
    }

    private static void internal_wellcomeparty(boolean enabled) {
        if (enabled) {
            PluginConfigManager manager = getConfigManager();
            File file = getConfigFile();
            if (manager != null && file != null) {
                manager.safeSet("WellcomeParty", enabled, file);
                wellcomePartyState = enabled;
                Utils.log("Wellcome Party is now: " + enabled);
            }
        }
    }

    public static void wellcomeParty(boolean enabled) {
        internal_wellcomeparty(enabled);
    }

    public static boolean getWellcomePartyState() {
        return wellcomePartyState;
    }
}
