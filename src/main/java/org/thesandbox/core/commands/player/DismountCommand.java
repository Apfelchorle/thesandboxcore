package org.thesandbox.core.commands.player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.services.entities.RideableDragonEntity;
import org.thesandbox.core.services.items.FloatBoatItem;

import java.util.List;

public class DismountCommand extends BaseCommand {

    private final FloatBoatItem floatBoatItem;
    private final RideableDragonEntity rideableDragonEntity;

    public DismountCommand(FloatBoatItem floatBoatItem, RideableDragonEntity rideableDragonEntity) {
        this.floatBoatItem = floatBoatItem;
        this.rideableDragonEntity = rideableDragonEntity;
    }

    public DismountCommand(FloatBoatItem floatBoatItem) {
        this(floatBoatItem, null);
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            return true;
        }

        Entity vehicle = player.getVehicle();
        if (vehicle == null) {
            player.sendMessage(Component.text("You are not inside any vehicle.", NamedTextColor.RED));
            return true;
        }

        boolean isFloatBoat = floatBoatItem != null && vehicle instanceof Boat boat && floatBoatItem.isFloatBoatPublic(boat);
        boolean isTrackedDragon = rideableDragonEntity != null && rideableDragonEntity.isTracked(vehicle);

        if (isFloatBoat) {
            FloatBoatItem.ALLOWED_DISMOUNTS.add(player.getUniqueId());
        }
        if (isTrackedDragon) {
            RideableDragonEntity.ALLOWED_DISMOUNTS.add(player.getUniqueId());
        }

        boolean removed = false;
        try {
            removed = vehicle.removePassenger(player);
        } finally {
            if (!removed) {
                if (isFloatBoat) {
                    FloatBoatItem.ALLOWED_DISMOUNTS.remove(player.getUniqueId());
                }
                if (isTrackedDragon) {
                    RideableDragonEntity.ALLOWED_DISMOUNTS.remove(player.getUniqueId());
                }
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
