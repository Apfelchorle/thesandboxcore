package org.thesandbox.core.commands.player;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.commands.CommandMessages;
import org.thesandbox.core.util.colors.Colors;

import java.util.List;

public class DeNickCommand extends BaseCommand {
    private final Essentials essentials;

    public DeNickCommand(JavaPlugin plugin) {
        Essentials found = null;
        try {
            found = JavaPlugin.getPlugin(Essentials.class);
        } catch (Throwable ignored) {
        }
        this.essentials = found;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (essentials == null || !essentials.isEnabled()) {
            sendError(sender, Colors.RED + "Essentials is not installed or not enabled. Cannot remove nicknames.");
            return true;
        }

        final String who = (sender instanceof Player) ? sender.getName() : "CONSOLE";
        Bukkit.getServer().broadcast(org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.SERVER, Colors.RED + who + " - Removing all online nicknames"));

        for (Player target : Bukkit.getOnlinePlayers()) {
            try {
                User user = essentials.getUser(target);
                if (user != null) {
                    user.setNickname(null);
                    target.displayName(Component.text(target.getName()));
                    try {
                        target.playerListName(Component.text(target.getName()));
                    } catch (Throwable ignored) {
                        sendCommand(target, Colors.RED + "Your nickname has been cleared by a staff member.");
                    }
                }
            } catch (Throwable t) {
                sendError(sender, Colors.RED + "Failed to clear nickname for: " + target.getName());
            }
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
