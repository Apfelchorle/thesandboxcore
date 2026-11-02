package org.thesandbox.core.commands.guilds;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.commands.CommandMessages;
import org.thesandbox.core.commands.meta.CommandAliases;
import org.thesandbox.core.commands.meta.CommandName;
import org.thesandbox.core.services.guilds.GuildManager;
import org.thesandbox.core.util.colors.Colors;

import java.util.List;

@CommandName("gchat")
@CommandAliases({"gc"})
public final class GChatCommand extends BaseCommand {
    private final JavaPlugin plugin;
    private final GuildManager guilds;

    public GChatCommand(JavaPlugin plugin, GuildManager guilds) {
        this.plugin = plugin;
        this.guilds = guilds;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;

        if (!guilds.isInGuild(p.getUniqueId())) {
            sendError(p, "&cYou are not in a guild.");
            return true;
        }

        if (args.length == 0) {
            guilds.toggleGuildChat(p.getUniqueId());
            sendCommand(p, "&aGuild chat: " + (guilds.isGuildChatToggled(p.getUniqueId()) ? "&2ON" : "&cOFF"));
            return true;
        }

        guilds.sendGuildChat(p, String.join(" ", args));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
