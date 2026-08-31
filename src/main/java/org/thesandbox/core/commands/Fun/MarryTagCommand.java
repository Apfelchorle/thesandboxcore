package org.thesandbox.core.commands.Fun;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.thesandbox.core.commands.ISubCommand;
import org.thesandbox.core.util.PlayerDataListener;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class MarryTagCommand implements ISubCommand
{
    private final JavaPlugin plugin;
    private final PlayerDataListener playerDataListener;
    private final Essentials essentials;

    public MarryTagCommand(JavaPlugin plugin, PlayerDataListener playerDataListener)
    {
        this.plugin = plugin;
        this.playerDataListener = playerDataListener;

        Essentials found = null;
        try {
            found = JavaPlugin.getPlugin(Essentials.class);
        } catch (Throwable ignored) {
        }
        this.essentials = found;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args)
    {
        if (essentials == null || !essentials.isEnabled()) {
            sender.sendMessage(Component.text("Essentials is not installed or not enabled. Cannot update marriage tags.", NamedTextColor.RED));
            return true;
        }

        final String who = (sender instanceof Player) ? sender.getName() : "CONSOLE";
        Bukkit.broadcastMessage(who + " - Updating marriage tags for online players");

        for (Player target : Bukkit.getOnlinePlayers()) {
            try {
                User user = essentials.getUser(target);
                if (user == null) continue;

                if (playerDataListener.getMarriageSpouse(user.getUUID()).isEmpty()) {
                    continue;
                }

                String emojiPrefix = getGenderEmoji(user);
                String currentNick = user.getNickname();
                String newNick = emojiPrefix + currentNick;
                user.setNickname(newNick);

                target.setDisplayName(target.getName());
                try {
                    target.setPlayerListName(target.getName());
                } catch (Throwable ignored) {
                    target.sendMessage(Component.text("Nickname updated!", NamedTextColor.GREEN));
                }
            } catch (Throwable t) {
                sender.sendMessage(Component.text("Failed to set nickname for: " + target.getName(), NamedTextColor.RED));
            }
        }

        return true;
    }

    private String getGenderEmoji(User user) {
        String gender = playerDataListener.getGender(user.getUUID());

        return switch (gender) {
            case "male" -> "&1 ❤ &r";
            case "female" -> "&c ❤ &r";
            default -> "&a ❤ &r";
        };
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        return Collections.emptyList();
    }
}