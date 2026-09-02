package org.thesandbox.core.commands.Fun;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.commands.ISubCommand;

import java.util.List;


// fake ass command lel - usfl [02/09/2026] 15:13
public class OpAllCommand implements ISubCommand {
    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        String msg = sender.getName() +  " - Opping all players on the server";
        Bukkit.broadcast(Component.text(msg, TextColor.color(0, 187, 255)));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(Component.text("You Are Op", TextColor.color(0, 187, 255)));
            if (player.hasPermission("sandbox.default")) {
                player.sendMessage(Component.text("You Are Op", TextColor.color(0, 187, 255)));
            }
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
