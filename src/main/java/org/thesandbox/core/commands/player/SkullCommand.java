package org.thesandbox.core.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.SkullMeta;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.commands.CommandMessages;
import org.thesandbox.core.util.colors.Colors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SkullCommand extends BaseCommand {

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            return true;
        }

        String targetName = (args.length == 0) ? player.getName() : args[0];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        // Guard against null name (never-played player)
        if (target.getName() == null) {
            sendError(sender, "Unknown player: " + targetName);
            return true;
        }

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(target);
            meta.displayName(Colors.component(Colors.GOLD + target.getName() + "'s Head"));
            skull.setItemMeta(meta);
        }

        PlayerInventory inv = player.getInventory();
        Map<Integer, ItemStack> leftovers = inv.addItem(skull);

        if (!leftovers.isEmpty()) {
            leftovers.values().forEach(item ->
                    player.getWorld().dropItemNaturally(player.getLocation(), item)
            );
            sendCommand(player, Colors.YELLOW + "Your inventory was full, so the skull was dropped at your feet.");
        } else {
            player.sendMessage(org.thesandbox.core.commands.MessageService.component(org.thesandbox.core.commands.MessageService.Prefix.COMMAND, Colors.GREEN + "You have been given " +
                    Colors.GOLD + target.getName() + "'s head" + Colors.GREEN + "."));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> matches = new ArrayList<>();

            for (Player p : Bukkit.getOnlinePlayers()) {
                String name = p.getName();
                if (name.toLowerCase().startsWith(prefix)) {
                    matches.add(name);
                }
            }

            matches.sort(String.CASE_INSENSITIVE_ORDER);
            return matches;
        }

        return List.of();
    }
}
