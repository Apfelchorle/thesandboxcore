package org.thesandbox.core.commands;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;

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
import org.thesandbox.core.items.Item;
import org.thesandbox.core.items.LightningRodItem;
import org.thesandbox.core.util.PlayerDataListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopCommand implements Listener,ISubCommand {

    private final LightningRodItem lightningRodItem;
    private final PlayerDataListener playerDataListener;

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
    private final Map<Integer, Item> shopSlots = new HashMap<>();

    public ShopCommand(TheSandboxCore plugin, LightningRodItem lightningRodItem, PlayerDataListener playerDataListener) {
        this.lightningRodItem = lightningRodItem;
        this.playerDataListener = playerDataListener;

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // Buy Logic [Called By Click Logic]
    private void buy(Player player, Item item, String itemName) {
        var leftover = player.getInventory().addItem(item.create());

        if (playerDataListener.getCoins(player.getUniqueId()) < item.getPrice()) {
            player.sendMessage(Component.text("You Do Not Have Enough Coins.", NamedTextColor.DARK_RED));
            return;
        }
        if (!leftover.isEmpty()) {
            player.sendMessage(Component.text("Your Inventory is full!", NamedTextColor.RED));
        } else if (playerDataListener.getCoins(player.getUniqueId()) > item.getPrice()) {
            playerDataListener.setCoins(player.getUniqueId(), playerDataListener.getCoins(player.getUniqueId()) - item.getPrice());
            player.sendMessage(Component.text("You received a " + itemName + "!", NamedTextColor.YELLOW));
        }
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ShopHolder)) {
            return;
        }

        if (event.getClickedInventory() == null || (!(event.getClickedInventory().getHolder() instanceof ShopHolder))) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();



        // Click Logic
        Item item = shopSlots.get(slot);
        if (item != null) {
            buy(player, item, item.getName());
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
        shopSlots.put(11, lightningRodItem);
        inventory.setItem(11, lightningRodItem.create());

        shopSlots.put(12, lightningRodItem);
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
