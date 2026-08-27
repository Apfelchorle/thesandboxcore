package org.thesandbox.core.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class gtfoCommand implements ISubCommand {
    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if  (args.length == 0) {
            String targetname = args[0];
            Player targetplayer = Bukkit.getPlayer(targetname);

            sender.sendMessage(Component.text("This is a Stub for now :)", NamedTextColor.BLACK));
            targetplayer.sendMessage(Component.text("This is a Stub for now :)", NamedTextColor.BLACK));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
