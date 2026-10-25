package org.thesandbox.core.util;

import java.io.File;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.fun.Utils;

public class Configinator {

    private static boolean WellcomePartyState = false;

    public static void wellcomeParty(boolean enabled) {
        TheSandboxCore core = TheSandboxCore.getInstance();
        PluginConfigManager configManager = core.getPluginConfigManager();
        File configfile = configManager.createFile("events.yml");
        if (enabled) {
            configManager.safeSet("WellcomeParty", enabled, configfile);
            WellcomePartyState = enabled;
            Utils.log("Wellcome Party is now :" + enabled);
        }
    }

    public static boolean getWellcomePartyState() {
        return WellcomePartyState;
    }
}
