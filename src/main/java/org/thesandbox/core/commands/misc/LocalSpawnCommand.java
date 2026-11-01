package org.thesandbox.core.commands.misc;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.commands.CommandMessages;
import org.thesandbox.core.util.colors.Colors;

import java.util.List;

public class LocalSpawnCommand extends BaseCommand {

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            return true;
        }

        Location spawn = player.getWorld().getSpawnLocation();
        player.teleport(spawn);
        player.sendMessage(CommandMessages.command(Colors.YELLOW + "Teleported to spawnpoint for world \""
                + player.getWorld().getName() + "\"."));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
