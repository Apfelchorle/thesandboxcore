package org.thesandbox.core.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DismountCommand implements ISubCommand {

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can use this command.");
                return true;
            }

            if (player.getVehicle() == null) {
                player.sendMessage("You are not inside any vehicle.");
                return true;
            }


            player.getVehicle().removePassenger(player);
            player.sendMessage("You have dismounted.");
            return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}