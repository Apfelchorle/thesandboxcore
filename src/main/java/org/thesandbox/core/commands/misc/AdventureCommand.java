package org.thesandbox.core.commands.misc;

import org.bukkit.GameMode;
import org.thesandbox.core.commands.utility.GamemodeWrapper;

public class AdventureCommand extends GamemodeWrapper {

    @Override
    protected GameMode gameMode() {
        return GameMode.ADVENTURE;
    }
}
