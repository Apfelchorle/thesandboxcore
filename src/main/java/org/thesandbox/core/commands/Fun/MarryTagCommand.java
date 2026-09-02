package org.thesandbox.core.commands.Fun;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

public class MarryTagCommand implements ISubCommand {
    private final JavaPlugin plugin;
    private final PlayerDataListener playerDataListener;
    private final Essentials essentials;

    // old and new prefixes
    private final List<String> legacyPrefixes = Arrays.asList(
            "&1 ❤ &r", "§1 ❤ §r", // Old Male
            "&c ❤ &r", "§c ❤ §r", // Old Female
            "&a ❤ &r", "§a ❤ §r", // Old Default aka ZOINKLEBIRDS
            "&#6dd0f7 ❤ &r", "§x§6§d§d§0§f§7 ❤ §r", // New Hex Male
            "&#ff98cb ❤ &r", "§x§f§f§9§8§c§b ❤ §r"  // New Hex Female
    );

    public MarryTagCommand(JavaPlugin plugin, PlayerDataListener playerDataListener) {
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
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (essentials == null || !essentials.isEnabled()) {
            sender.sendMessage(Component.text("Essentials is not installed or not enabled. Cannot update marriage tags.", NamedTextColor.RED));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can run this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /" + label + " update", NamedTextColor.RED));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        //noinspection SwitchStatementWithTooFewBranches
        switch (subCommand) {
            case "update" -> handleUpdate(player);
            default -> player.sendMessage(Component.text("Unknown subcommand. Usage: /" + label + " update", NamedTextColor.RED));
        }
        return true;
    }

    private void handleUpdate(Player player) {
        try {
            UUID playerUuid = player.getUniqueId();
            User user = essentials.getUser(playerUuid);
            if (user == null) return;

            String currentNick = user.getNickname();
            if (currentNick == null || currentNick.isEmpty()) {
                currentNick = user.getName();
            }

            // strippers
            for (String prefix : legacyPrefixes) {
                if (currentNick.startsWith(prefix)) {
                    currentNick = currentNick.substring(prefix.length());
                    break;
                }
            }

            String status = playerDataListener.getMarriageStatus(playerUuid);

            if (status != null && status.equalsIgnoreCase("Divorced")) {
                user.setNickname(currentNick.equalsIgnoreCase(user.getName()) ? null : currentNick);
                player.sendMessage(Component.text("Your marriage tags were removed.", NamedTextColor.YELLOW));
            } else {
                String emojiPrefix = getGenderEmoji(playerUuid);
                String newNick = emojiPrefix + currentNick;
                user.setNickname(newNick);
                player.sendMessage(Component.text("Your marriage tags have been updated!", NamedTextColor.GREEN));
            }

        } catch (Exception e) {
            player.sendMessage(Component.text("Failed to set nickname for: " + player.getName() + " ERROR: " + e.getMessage(), NamedTextColor.RED));
        }
    }

    // uses hex now!
    private String getGenderEmoji(UUID user) {
        String gender = playerDataListener.getGender(user);
        if (gender == null) return "&a ❤ &r";

        return switch (gender.toLowerCase()) {
            case "male" -> "&#6dd0f7 ❤ &r";   // malers!
            case "female" -> "&#ff98cb ❤ &r"; // fremales
            default -> "&a ❤ &r";            // zoinklebirds
        };
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            if ("update".startsWith(args[0].toLowerCase())) {
                return List.of("update");
            }
        }
        return Collections.emptyList();
    }
}
