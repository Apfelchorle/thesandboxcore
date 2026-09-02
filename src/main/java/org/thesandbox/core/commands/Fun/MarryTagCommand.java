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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class MarryTagCommand implements ISubCommand
{
    private final JavaPlugin plugin;
    private final PlayerDataListener playerDataListener;
    private final Essentials essentials;

    List<String> genderPrefixes = Arrays.asList("&a ❤ &r", "&c ❤ &r","&1 ❤ &r");

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
        if (!(sender instanceof Player player)) {
            return true;
        }
        String subCommand = args[0];


        switch (subCommand) {
            case "update" -> handleUpdate(player);
        }
        return true;
    }

    // Handlers
    private void handleUpdate(Player player) {
        try {
            UUID playerUuid = player.getUniqueId();
            User user = essentials.getUser(playerUuid);
            if (user == null) return;
            String currentnick = user.getNickname();
            String newnick = (getGenderEmoji(playerUuid) +  currentnick);
            String spouse = playerDataListener.getMarriageSpouse(playerUuid);

            // divorced people stripped of hearts
            if (spouse == null || spouse.isEmpty()) {
                for (String prefix : genderPrefixes) {
                    if (currentnick.startsWith(prefix)) {
                        currentnick = currentnick.substring(prefix.length());
                        user.setNickname(newnick);
                        break;
                    }
                }
            } else {
                String emojiPrefix = getGenderEmoji(playerUuid);

                String currentNick = user.getNickname();
                if (currentNick == null || currentNick.isEmpty()) {
                    currentNick = user.getName();
                }
                for (String prefix : genderPrefixes) {
                    if (currentNick.startsWith(prefix)) {
                        currentNick = currentNick.substring(prefix.length());
                        break;
                    }
                }


                String newNick = emojiPrefix + currentNick;
                user.setNickname(newNick);
            }

        } catch (Error e) {
            player.sendMessage(Component.text("Failed to set nickname for: " + player.getName() + " ERROR : " + e, NamedTextColor.RED));
        }
    }

    // Getters
    private String getGenderEmoji(UUID user) {
        String gender = playerDataListener.getGender(user);

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