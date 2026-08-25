package org.thesandbox.core.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.util.PlayerDataKeys;
import org.thesandbox.core.util.PlayerDataListener;

import java.util.List;

public class LoginMessageCommand implements ISubCommand {

    private static final int MAX_LENGTH = 100;

    private final PlayerDataListener playerDataListener;

    public LoginMessageCommand(PlayerDataListener playerDataListener) {
        this.playerDataListener = playerDataListener;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can execute this command!", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        String state = playerDataListener.get(player.getUniqueId(), PlayerDataKeys.LOGIN_MESSAGES_STATE, "not_owned");
        if (state.equalsIgnoreCase("not_owned")) {
            player.sendMessage(Component.text("You don't own the Login Messages item! Buy it from /shop.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            String current = playerDataListener.get(player.getUniqueId(), PlayerDataKeys.LOGIN_MESSAGE, "");
            if (current.isEmpty()) {
                player.sendMessage(Component.text("You haven't set a login message yet. Usage: /loginmessage <message>", NamedTextColor.YELLOW));
            } else {
                player.sendMessage(Component.text("Your current login message:", NamedTextColor.YELLOW));
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(current));
            }
            return true;
        }

        String raw = String.join(" ", args);

        if (raw.length() > MAX_LENGTH) {
            player.sendMessage(Component.text("Your message is too long! Max " + MAX_LENGTH + " characters.", NamedTextColor.RED));
            return true;
        }

        String sanitized = player.hasPermission("sandbox.staff") ? raw : raw.replaceAll("(?i)&k", "");

        playerDataListener.set(player.getUniqueId(), PlayerDataKeys.LOGIN_MESSAGE, sanitized);

        player.sendMessage(Component.text("Login message set to:", NamedTextColor.GREEN));
        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(sanitized));

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}