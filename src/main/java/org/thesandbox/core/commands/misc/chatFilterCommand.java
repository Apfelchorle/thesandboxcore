package org.thesandbox.core.commands.misc;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.commands.ISubCommand;
import org.thesandbox.core.util.PlayerDataKeys;
import org.thesandbox.core.util.PlayerDataListener;

import java.util.List;

public class chatFilterCommand implements ISubCommand {

    private final PlayerDataListener playerDataListener;

    public chatFilterCommand(PlayerDataListener playerDataListener) {
        this.playerDataListener = playerDataListener;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }


        boolean current = playerDataListener.get(player.getUniqueId(), PlayerDataKeys.CHATFILTER, false);
        playerDataListener.set(player.getUniqueId(), PlayerDataKeys.CHATFILTER, !current);
        player.sendMessage(Component.text("ChatFilter is now: " + current, NamedTextColor.YELLOW));
        return false;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
