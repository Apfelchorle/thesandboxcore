package org.thesandbox.core.commands.utility;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.commands.CommandMessages;
import org.thesandbox.core.services.chat.ManageChatService;
import org.thesandbox.core.util.colors.Colors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /managechat mute | clear
 * - mute: toggles global mute, broadcasts in red.
 * - clear: clear chat for everyone EXCEPT players with "sandbox.staff", then broadcast in red.
 */
public class ManageChatCommand extends BaseCommand implements TabCompleter {

    private final JavaPlugin plugin;
    private final ManageChatService service;

    public ManageChatCommand(JavaPlugin plugin, ManageChatService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {

        if (args.length < 1) {
            sendUsage(sender, "/" + label + " <mute|clear|forceclear>");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "mute": {
                if (!service.isMuted()) {
                    service.mute();
                    Bukkit.getServer().broadcast(org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.SERVER, Colors.RED + sender.getName() + " - Muting the chat"));
                } else {
                    service.unmute();
                    Bukkit.getServer().broadcast(org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.SERVER, Colors.RED + sender.getName() + " - Unmuting the chat"));
                }
                return true;
            }
            case "clear": {
                // One multi-line component = one packet per player instead of 100.
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.hasPermission("sandbox.staff")) continue;
                    p.sendMessage(Component.text("\n".repeat(100)));
                }
                Bukkit.getServer().broadcast(org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.SERVER, Colors.RED + sender.getName() + " - Cleared the chat"));
                return true;
            }
            case "forceclear": {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage(Component.text("\n".repeat(100)));
                }
                Bukkit.getServer().broadcast(org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.SERVER, "&c" + sender.getName() + " - Force Cleared the chat"));
                return true;
            }
            default: {
                sendUsage(sender, "/" + label + " <mute|clear|forceclear>");
                return true;
            }
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String pref = args[0].toLowerCase(Locale.ROOT);
            if ("mute".startsWith(pref)) out.add("mute");
            if ("clear".startsWith(pref)) out.add("clear");
        }
        return out;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> r = tabComplete(sender, command, alias, args);
        return (r != null) ? r : java.util.Collections.emptyList();
    }
}
