package org.thesandbox.core.util;

import java.io.File;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.fun.Utils;

public class Configinator {

    private static boolean wellcomePartyState = false;
    private static File configFile;

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
