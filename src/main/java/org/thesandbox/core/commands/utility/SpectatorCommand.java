package org.thesandbox.core.commands.utility;

import org.bukkit.GameMode;

public class SpectatorCommand extends GamemodeWrapper {

    @Override
    protected GameMode gameMode() {
        return GameMode.SPECTATOR;
    }
}
