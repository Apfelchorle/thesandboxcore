package org.thesandbox.core.commands.Fun;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.fun.Utils;
import org.thesandbox.core.util.PlayerDataKeys;
import org.thesandbox.core.util.PlayerDataListener;

import java.util.UUID;

public class BirthCommand extends BaseCommand {

    private final TheSandboxCore core = TheSandboxCore.getInstance();
    private final PlayerDataListener pdl = core.getDataListener();
    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        // hardcoded because i forgor
        if (sender instanceof Player player) {
            if (player.getName().equals("1ToL")) {
                sender.sendMessage("You are not allowed to perform this command.");
            } else {
                handlebirth(player);
            }
        }
        sender.sendMessage("❤ You've Been birthed by Livi ❤");
        return false;
    }

    public void handlebirth(Player player) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer("1ToL");
        pdl.set(player.getUniqueId(), PlayerDataKeys.BirthedByLivi, true);
        Utils.Feedback(player,"❤ You've Been birthed by " + offlinePlayer.getName() + " ❤", NamedTextColor.RED, Sound.ENTITY_CAT_AMBIENT);
    }

}
