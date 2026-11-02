package org.thesandbox.core.commands.misc;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.commands.CommandMessages;
import org.thesandbox.core.services.discord.DiscordBridge;

import java.util.List;

/**
 * Toggles Discord media-to-ASCII rendering on/off.
 * When disabled, messages with media show a clickable [Media] tag instead of ASCII art.
 */
public class MediaToggleCommand extends BaseCommand {

    private final TheSandboxCore plugin;

    public MediaToggleCommand(TheSandboxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!hasStaffPermission(sender)) {
            return true;
        }

        DiscordBridge discord = plugin.getDiscord();
        if (discord == null) {
            sendError(sender, "Discord bridge is not available.");
            return true;
        }

        boolean currentlyEnabled = discord.isMediaRenderEnabled();
        discord.setMediaRenderEnabled(!currentlyEnabled);
        boolean newState = discord.isMediaRenderEnabled();

        Component status = newState
                ? org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.INFO, "Discord media-to-ASCII rendering is now ENABLED.")
                : org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.INFO, "Discord media-to-ASCII rendering is now DISABLED.");
        sender.sendMessage(status);

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
