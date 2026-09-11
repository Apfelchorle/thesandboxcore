package org.thesandbox.core.commands.Fun;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.thesandbox.core.fun.items.FloatBoatItem;

public class DismountCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (player.getVehicle() == null) {
            player.sendMessage(Component.text("You are not inside any vehicle.", NamedTextColor.RED));
            return true;
        }

        // Allow the upcoming dismount event to pass through
        FloatBoatItem.ALLOWED_DISMOUNTS.add(player.getUniqueId());

        player.getVehicle().removePassenger(player);
        player.sendMessage(Component.text("You dismounted safely.", NamedTextColor.GREEN));
        return true;
    }
}