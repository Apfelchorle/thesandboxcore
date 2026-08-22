package org.thesandbox.core.commands;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShopCommand implements ISubCommand {
    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can execute this command.", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        Inventory inventory = Bukkit.createInventory(player, 9 * 3, Component.text("Shop Command", NamedTextColor.DARK_GREEN));

        // as shown above the Inventory Menu is 9 * 3 which is 27 slots
        inventory.setItem(11, getItem(new ItemStack(Material.BLAZE_ROD),"Test", "Testing", "&4Testing" , "&aTesting"));


        player.openInventory(inventory);

        return true;
    }

    private ItemStack getItem(ItemStack item, String name, String ... lore) {
        ItemMeta meta = item.getItemMeta();

        // meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(name));

        List<Component> loreList = new ArrayList<>();

        for (String s : lore) {
            // loreList.add(ChatColor.translateAlternateColorCodes('&', s));
            loreList.add(LegacyComponentSerializer.legacyAmpersand().deserialize(s));
        }
        meta.lore(loreList);

        item.setItemMeta(meta);
        return item;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
