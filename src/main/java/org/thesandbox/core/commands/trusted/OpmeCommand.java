package org.thesandbox.core.commands.trusted;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.commands.MessageService;
import org.thesandbox.core.util.Utils;

/**
 * Toggles OP for players who are both explicitly permitted and on the
 * server-owner-controlled {@code trusted-uuids} list.
 */
public class OpmeCommand extends BaseCommand {

    private final TheSandboxCore plugin;

    public OpmeCommand(TheSandboxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            return true;
        }

        if (!player.hasPermission("sandbox.opme")) {
            sendError(sender, "You do not have permission to use this command.");
            return true;
        }

        if (!Utils.hasUuid(player)) {
            player.sendMessage(Component.text(
                    MessageService.common(
                            "opme-no-access",
                            "You Do Not Have Access To This Command"
                    ),
                    NamedTextColor.RED,
                    TextDecoration.BOLD
            ));
            return true;
        }

        boolean newState = !player.isOp();
        player.setOp(newState);
        plugin.getLogger().info("[Security] " + player.getName()
                + " (" + player.getUniqueId() + ") set OP=" + newState
                + " via /opme");

        player.sendMessage(Component.text(
                MessageService.formatCommon("opme-status", "OP status is now: %s", newState),
                newState ? NamedTextColor.GREEN : NamedTextColor.DARK_RED
        ));
        return true;
    }
}
