package org.thesandbox.core.commands.utility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.commands.CommandMessages;

import java.io.File;
import java.util.Collections;
import java.util.List;

final class WorldTeleportHelper {

    private WorldTeleportHelper() {
    }

    static boolean teleport(CommandSender sender, String worldName, String displayName) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(CommandMessages.error(ChatColor.RED + "This command can only be used in-game."));
            return true;
        }

        World world = resolveWorld(worldName);
        if (world == null) {
            player.sendMessage(CommandMessages.error(ChatColor.RED + displayName + " is not loaded."));
            return true;
        }

        if (player.teleport(world.getSpawnLocation())) {
            player.sendMessage(CommandMessages.command(ChatColor.GRAY + "Teleporting you to the " + displayName.toLowerCase() + "."));
        } else {
            player.sendMessage(CommandMessages.error(ChatColor.RED + "Failed to teleport."));
        }
        return true;
    }

    static World resolveWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            return world;
        }

        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (!worldFolder.exists() || !worldFolder.isDirectory()) {
            return null;
        }

        try {
            return WorldCreator.name(worldName).createWorld();
        } catch (Throwable ignored) {
            return null;
        }
    }

    static List<String> noCompletions() {
        return Collections.emptyList();
    }
}
