package org.thesandbox.core.commands.utility;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.thesandbox.core.commands.BaseCommand;

import java.util.List;

/**
 * Generic wrapper that delegates to {@link WorldTeleportHelper}.
 * Subclasses specify the world name and display name via {@link #worldName()} and {@link #displayName()}.
 */
public abstract class WorldTeleportWrapper extends BaseCommand {

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        return WorldTeleportHelper.teleport(sender, worldName(), displayName());
    }

    /**
     * Returns the internal world name (e.g. "world_nether").
     */
    protected abstract String worldName();

    /**
     * Returns the human-readable display name (e.g. "Nether").
     */
    protected abstract String displayName();

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return WorldTeleportHelper.noCompletions();
    }
}
