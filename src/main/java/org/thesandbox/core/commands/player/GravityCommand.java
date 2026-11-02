package org.thesandbox.core.commands.player;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.commands.CommandMessages;
import org.thesandbox.core.util.colors.Colors;

import java.util.List;

public class GravityCommand extends BaseCommand {

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            return true;
        }

        // Toggle gravity
        boolean newState = !player.hasGravity();
        player.setGravity(newState);

        player.sendMessage(org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.COMMAND, (newState ? Colors.GREEN : Colors.RED) +
                (newState ? "Enabled your gravity!" : "Disabled your gravity!")));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
