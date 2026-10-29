package org.thesandbox.core.commands.Fun;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.thesandbox.core.commands.ISubCommand;
import org.thesandbox.core.fun.items.FloatBoatItem;

import java.util.List;

public class DismountCommand implements ISubCommand {

    private final FloatBoatItem floatBoatItem;

    public DismountCommand(FloatBoatItem floatBoatItem) {
        this.floatBoatItem = floatBoatItem;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Entity vehicle = player.getVehicle();
        if (vehicle == null) {
            player.sendMessage(Component.text("You are not inside any vehicle.", NamedTextColor.RED));
            return true;
        }

        boolean isFloatBoat = vehicle instanceof Boat boat && floatBoatItem.isFloatBoatPublic(boat);

        if (isFloatBoat) {
            FloatBoatItem.ALLOWED_DISMOUNTS.add(player.getUniqueId());
        }

        boolean removed = false;
        try {
            removed = vehicle.removePassenger(player);
        } finally {
            // Memory Leak Prevention: Ensure player reference is cleaned up on failure or exception
            if (!removed && isFloatBoat) {
                FloatBoatItem.ALLOWED_DISMOUNTS.remove(player.getUniqueId());
            }
        }

        if (removed) {
            player.sendMessage(Component.text("You dismounted safely.", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Failed to dismount.", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}