package org.thesandbox.core.fun.items;

import java.util.*;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.fun.Utils;
import org.thesandbox.core.fun.items.itemUTILS.Item;
import org.thesandbox.core.fun.items.itemUTILS.ItemKeys;

public class ClownFishItem implements Item {

    private static final String NAME = "ClownFish";
    private final ItemKeys keys;

    private static final int RADIUS_HIT = 5;
    private static final int STRENGTH = 4;

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final TheSandboxCore plugin;



    public ClownFishItem(ItemKeys keys, TheSandboxCore plugin) {
        this.keys = keys;
        this.plugin = plugin;
    }

    @Override
    public ItemStack create() {
        ItemStack item = new ItemStack(Material.TROPICAL_FISH);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(keys.clownFish, PersistentDataType.BYTE, (byte) 1);
            meta.displayName(Component.text("ClownFish").color(net.kyori.adventure.text.format.NamedTextColor.GOLD));
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(keys.clownFish, PersistentDataType.BYTE);
    }

    @Override
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
            return;
        }

        Player player = e.getPlayer();

        e.setCancelled(true);

        long cooldownMs = player.hasPermission("sandbox.staff")
                ? plugin.getConfig().getLong("items.clownFish.staff.cooldown", 0)
                : plugin.getConfig().getLong("items.clownFish.cooldown", 10000);

        long now = System.currentTimeMillis();
        Long last = cooldowns.get(player.getUniqueId());
        if (last != null && now - last < cooldownMs) {
            return;
        }
        cooldowns.put(player.getUniqueId(), now);

        boolean didHit = false;

        Location playerLoc = player.getLocation();
        Vector playerLocVec = playerLoc.toVector();
        List<Player> players = player.getWorld().getPlayers();

        for (Player target : players) {
            if (target == player) {
                continue;
            }

            Location targetPos = target.getLocation();
            Vector targetPosVec = targetPos.toVector();

            try {
                if (targetPosVec.distanceSquared(playerLocVec) < (RADIUS_HIT * RADIUS_HIT)) {
                    //target.setAllowFlight(false); per request this has been disabled
                    target.setFlying(false);

                    Vector blastDirection = targetPosVec.subtract(playerLocVec).normalize().multiply(STRENGTH);
                    target.setVelocity(blastDirection);

                    playClownfishSound(target, targetPos);
                    didHit = true;
                }
            } catch (IllegalArgumentException ex) {
                // Safeguard for distinct world vectors
            }
        }

        if (didHit) {
            playClownfishSound(player, playerLoc);
            sendClownfishMessage(player, playerLoc);
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getPrice() {
        return plugin.getConfig().getInt("items.clownFish.price", 150);
    }

    private void playClownfishSound(Player listener, Location playerLoc) {
        Utils.playSound(listener, playerLoc, Sound.ENTITY_PLAYER_ATTACK_WEAK);
        Utils.playSound(listener, playerLoc, Sound.ENTITY_FISH_SWIM);
    }

    private void sendClownfishMessage(Player player, Location playerLoc) {
        Component clownMsg = Component.empty();
        for (char c : "You are a clown.".toCharArray()) {
            clownMsg = clownMsg.append(Component.text(String.valueOf(c)).color(Utils.getRandomChatColor()));
        }
        player.sendMessage(clownMsg);
        Utils.SendMessage(player, "You've Been Clowned By, You've Been Clowned By, A Smooth Clown!", NamedTextColor.DARK_PURPLE);
        Utils.WeAllKnowWhatThisIs(plugin, playerLoc, Sound.BLOCK_NOTE_BLOCK_BELL);
    }
}
