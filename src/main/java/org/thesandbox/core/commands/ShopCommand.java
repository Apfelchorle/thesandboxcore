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
import org.thesandbox.core.fun.items.*;
import org.thesandbox.core.fun.items.itemUTILS.Item;
import org.thesandbox.core.util.PlayerDataListener;

import java.util.*;

public class ShopCommand implements Listener,ISubCommand {

    private final LightningRodItem lightningRodItem;
    private final PlayerDataListener playerDataListener;
    private final LoginMessagesItem loginMessagesItem;
    private final ClownFishItem clownFishItem;
    private final Rideable_Ender_Pearl_Item rideableEnderPearlItem;
    private final GrapplingHookItem grapplingHookItem;
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


    public ShopCommand(TheSandboxCore plugin, PlayerDataListener playerDataListener, LightningRodItem lightningRodItem, LoginMessagesItem loginMessagesItem, ClownFishItem clownFishItem, Rideable_Ender_Pearl_Item rideableEnderPearlItem, GrapplingHookItem grapplingHookItem) {
        this.playerDataListener = playerDataListener;
        this.lightningRodItem = lightningRodItem;
        this.loginMessagesItem = loginMessagesItem;
        this.clownFishItem = clownFishItem;
        this.rideableEnderPearlItem = rideableEnderPearlItem;
        this.grapplingHookItem = grapplingHookItem;

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @SuppressWarnings("unchecked")
    private <T extends Item> T findItem(TheSandboxCore plugin, Class<T> clazz) {
        for (Item item : plugin.getRegisteredItems()) {
            if (clazz.isInstance(item)) {
                return (T) item;
            }
        }
        throw new IllegalStateException("[TheSandboxCore] Required shop item " + clazz.getSimpleName() + " was not auto-registered!");
    }


    // Buy Logic [Called By Click Logic]
    private void buy(Player player, Item item, String itemName) {
        List<String> owned_statuses = Arrays.asList("owned", "bought", "enabled", "disabled");
        int price = item.getPrice();
        UUID puuid = player.getUniqueId();
        int balance = playerDataListener.getCoins(puuid);
        String item_status = playerDataListener.get(puuid,itemName, "not_owned");

        if (owned_statuses.contains(item_status.toLowerCase())) {
            var leftover = player.getInventory().addItem(item.create());

            if (!leftover.isEmpty()) {
                player.sendMessage(Component.text("Your Inventory is full!", NamedTextColor.RED));
                return;
            }
            player.sendMessage(Component.text("You received a " + itemName + "!", NamedTextColor.YELLOW));
            return;
        }

        if (balance < price) {
            player.sendMessage(Component.text("You Do Not Have Enough Coins.", NamedTextColor.DARK_RED));
            player.sendMessage(Component.text("You Need " + (price - balance) + " More Coins!", NamedTextColor.DARK_RED));
            return;
        }

        var leftover = player.getInventory().addItem(item.create());

        if (!leftover.isEmpty()) {
            player.sendMessage(Component.text("Your Inventory is full!", NamedTextColor.RED));
            return;
        }

        playerDataListener.set(player.getUniqueId(),itemName, "owned");
        playerDataListener.setCoins(player.getUniqueId(), playerDataListener.getCoins(player.getUniqueId()) - item.getPrice());
        player.sendMessage(Component.text("You received a " + itemName + "!", NamedTextColor.YELLOW));

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

        shopSlots.put(4, grapplingHookItem);
        inventory.setItem(4, grapplingHookItem.create());

        shopSlots.put(11, lightningRodItem);
        inventory.setItem(11, lightningRodItem.create());

        shopSlots.put(12, lightningRodItem);
        inventory.setItem(12, getItem(new ItemStack(Material.OAK_BOAT),"Test", "Testing", "&4Testing" , "&aTesting"));

        shopSlots.put(13, loginMessagesItem);
        inventory.setItem(13, loginMessagesItem.create());

        shopSlots.put(14, clownFishItem);
        inventory.setItem(14, clownFishItem.create());

        shopSlots.put(15, rideableEnderPearlItem);
        inventory.setItem(15, rideableEnderPearlItem.create());


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
