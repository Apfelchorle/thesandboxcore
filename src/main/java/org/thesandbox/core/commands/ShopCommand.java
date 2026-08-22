package org.thesandbox.core.commands;

import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.thesandbox.core.TheSandboxCore;

import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShopCommand implements Listener,ISubCommand {

    private static class ShopHolder implements InventoryHolder {

        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

    }
    private final Component shoptitle = Component.text("The Shop", NamedTextColor.DARK_GREEN);

    public ShopCommand(TheSandboxCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ShopHolder)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (slot == 11) {
            player.closeInventory();
            player.sendMessage(Component.text("Lightning Wazuhhhhhh!!!!", NamedTextColor.YELLOW));
        }

        event.setCancelled(true);
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players may execute this command.", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;
        ShopHolder holder = new ShopHolder();
        Inventory inventory = Bukkit.createInventory(holder, 9 * 3, shoptitle);
        holder.setInventory(inventory);

        // as shown above the Inventory Menu is 9 * 3 which is 27 slots
        inventory.setItem(11, getItem(new ItemStack(Material.BLAZE_ROD),"Test", "Testing", "&4Testing" , "&aTesting"));
        inventory.setItem(12, getItem(new ItemStack(Material.OAK_BOAT),"Test", "Testing", "&4Testing" , "&aTesting"));

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
