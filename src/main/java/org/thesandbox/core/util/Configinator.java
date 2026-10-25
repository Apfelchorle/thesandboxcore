package org.thesandbox.core.util;

import java.io.File;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.fun.Utils;

public class Configinator {

    private static boolean WellcomePartyState = false;
    private static TheSandboxCore core = TheSandboxCore.getInstance();
    private static PluginConfigManager configManager = core.getPluginConfigManager();
    private static File configfile = configManager.createFile("party.yml");
    private static void internal_wellcomeparty(boolean enabled) {
        if (enabled) {
            configManager.safeSet("WellcomeParty", enabled, configfile);
            WellcomePartyState = enabled;
            Utils.log("Wellcome Party is now :" + enabled);
        }
    }

    public static void wellcomeParty(boolean enabled) {
        internal_wellcomeparty(enabled);
    }

    public static boolean getWellcomePartyState() {
        return WellcomePartyState;
    }
}
