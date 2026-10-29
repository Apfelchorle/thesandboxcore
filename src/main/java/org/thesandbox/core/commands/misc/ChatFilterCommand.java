package org.thesandbox.core.commands.misc;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.util.PlayerDataKeys;
import org.thesandbox.core.util.PlayerDataListener;

public class ChatFilterCommand extends BaseCommand {

    private final PlayerDataListener playerDataListener;

    public ChatFilterCommand(PlayerDataListener playerDataListener) {
        this.playerDataListener = playerDataListener;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            return true;
        }

        boolean current = playerDataListener.get(player.getUniqueId(), PlayerDataKeys.CHATFILTER, true);
        boolean newValue = !current;
        playerDataListener.set(player.getUniqueId(), PlayerDataKeys.CHATFILTER, newValue);
        player.sendMessage(Component.text("ChatFilter is now: " + newValue, NamedTextColor.YELLOW));
        return true;
    }
}
