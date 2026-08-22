package org.thesandbox.core.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor; // deprecated ?????
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.TheSandboxCore;

import java.util.List;


public class DiscordCommand implements ISubCommand {
    private final TheSandboxCore plugin;

    public DiscordCommand(TheSandboxCore plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        // you can tell what this does
        String inviteUrl = plugin.getConfig().getString("discord.invite-url", "value not set");
        sender.sendMessage(Component.text("Discord Link: " + inviteUrl, NamedTextColor.BLUE));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
