package org.thesandbox.core.commands.misc;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.thesandbox.core.commands.BaseCommand;

import java.util.List;

public class DiscordCommand extends BaseCommand {

    private final JavaPlugin plugin;

    public DiscordCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        String inviteUrl = plugin.getConfig().getString("discord.invite-url", "value not set");
        sender.sendMessage(Component.text("Discord Link: " + inviteUrl, NamedTextColor.BLUE)
                .clickEvent(ClickEvent.openUrl(inviteUrl)));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
