package org.thesandbox.core.commands.Fun;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.thesandbox.core.fun.items.FloatBoatItem;

public class DismountCommand implements CommandExecutor {

    private final FloatBoatItem floatBoatItem;

    public DismountCommand(FloatBoatItem floatBoatItem) {
        this.floatBoatItem = floatBoatItem;
    }

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

        boolean isFloatBoat = player.getVehicle() instanceof Boat boat && floatBoatItem.isFloatBoatPublic(boat);
        if (isFloatBoat) {
            FloatBoatItem.ALLOWED_DISMOUNTS.add(player.getUniqueId());
        }

        boolean removed = player.getVehicle().removePassenger(player);
        if (removed) {
            player.sendMessage(Component.text("You dismounted safely.", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Failed to dismount.", NamedTextColor.RED));
            if (isFloatBoat) {
                FloatBoatItem.ALLOWED_DISMOUNTS.remove(player.getUniqueId());
            }
        }
        return true;
    }
}