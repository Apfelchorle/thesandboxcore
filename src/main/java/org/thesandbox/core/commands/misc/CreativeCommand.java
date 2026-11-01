package org.thesandbox.core.commands.misc;

import org.bukkit.GameMode;
import org.thesandbox.core.commands.utility.GamemodeWrapper;

public class CreativeCommand extends GamemodeWrapper {

    @Override
    protected GameMode gameMode() {
        return GameMode.CREATIVE;
    }
}
