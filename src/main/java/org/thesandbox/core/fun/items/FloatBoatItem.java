package org.thesandbox.core.fun.items;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.fun.items.itemUTILS.Item;
import org.thesandbox.core.fun.items.itemUTILS.ItemKeys;
import org.thesandbox.core.util.PlayerDataKeys;

import java.util.List;

public class FloatBoatItem implements Item, Listener {

    private static final String NAME = PlayerDataKeys.FLOAT_BOAT;

    private final TheSandboxCore plugin;
    private final ItemKeys keys;

    public FloatBoatItem(TheSandboxCore plugin, ItemKeys keys) {
        this.plugin = plugin;
        this.keys = keys;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public ItemStack create() {
        ItemStack item = new ItemStack(Material.OAK_BOAT);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(NAME, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("A boat that has no gravity.", NamedTextColor.DARK_GRAY)
        ));
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(keys.Float_Boat, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(keys.Float_Boat, PersistentDataType.BYTE);
    }

    @Override
    public void onInteract(PlayerInteractEvent e) {
        // Vanilla boat placement must proceed so EntityPlaceEvent can mark it as no-gravity.
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getPrice() {
        return plugin.getConfig().getInt("items." + NAME + ".price", 30);
    }

    @EventHandler
    public void onBoatPlace(EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Boat boat)) {
            return;
        }

        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        ItemStack used = player.getInventory().getItem(event.getHand());
        if (!matches(used)) {
            return;
        }

        applyNoGravity(boat);
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof Boat boat && isFloatBoat(boat)) {
                boat.setGravity(false);
            }
        }
    }

    @EventHandler
    public void onSneakToggle(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!(player.getVehicle() instanceof Boat boat) || !isFloatBoat(boat)) return;
        if (event.isSneaking()) {
            boat.setVelocity(boat.getVelocity().setY(-0.2));
        }
    }

    @EventHandler
    public void onJump(PlayerJumpEvent event) {
        Player player = event.getPlayer();
        if (!(player.getVehicle() instanceof Boat boat) || !isFloatBoat(boat)) return;
        boat.setVelocity(boat.getVelocity().setY(0.2));
    }

    private void applyNoGravity(Boat boat) {
        boat.setGravity(false);
        boat.getPersistentDataContainer().set(keys.Float_Boat, PersistentDataType.BYTE, (byte) 1);
    }

    private boolean isFloatBoat(Boat boat) {
        return boat.getPersistentDataContainer().has(keys.Float_Boat, PersistentDataType.BYTE);
    }
}
