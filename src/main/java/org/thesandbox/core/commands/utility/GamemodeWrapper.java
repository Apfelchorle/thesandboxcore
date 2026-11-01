package org.thesandbox.core.commands.utility;

import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.thesandbox.core.commands.BaseCommand;

import java.util.List;

/**
 * Generic wrapper that delegates to {@link GameModeChangeHelper}.
 * Subclasses specify the game mode via {@link #gameMode()}.
 */
public abstract class GamemodeWrapper extends BaseCommand {

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        return GameModeChangeHelper.execute(sender, args, gameMode());
    }

    /**
     * Returns the game mode this wrapper represents.
     */
    protected abstract GameMode gameMode();

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return GameModeChangeHelper.tabComplete(sender, args);
    }
}
