package org.thesandbox.core.commands.utility;

import org.bukkit.GameMode;

public class SurvivalCommand extends GamemodeWrapper {

    @Override
    protected GameMode gameMode() {
        return GameMode.SURVIVAL;
    }
}
