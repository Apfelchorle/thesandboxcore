package org.thesandbox.core.fun.items;

import java.util.List;
import java.util.Random;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

public class ClownFishItem implements Item {

    private static final String NAME = "ClownFish";
    private final ItemKeys keys;
    private final Random random = new Random();

    private static final int RADIUS_HIT = 5;
    private static final int STRENGTH = 4;

    public ClownFishItem(ItemKeys keys) {
        this.keys = keys;
    }

    @Override
    public ItemStack create() {
        ItemStack item = new ItemStack(Material.TROPICAL_FISH);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(keys.clownFish, PersistentDataType.BYTE, (byte) 1);
            meta.displayName(Component.text("Clown Fish").color(net.kyori.adventure.text.format.NamedTextColor.GOLD));
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

        if (!player.hasPermission("sandbox.staff")) {
            Component clownMsg = Component.empty();
            for (char c : "You are a clown.".toCharArray()) {
                clownMsg = clownMsg.append(Component.text(String.valueOf(c)).color(getRandomChatColor()));
            }
            player.sendMessage(clownMsg);
            player.getEquipment().setItemInMainHand(new ItemStack(Material.POTATO));
            return;
        }

        e.setCancelled(true);
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
                    target.setAllowFlight(false);
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
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getPrice() {
        return 150;
    }

    private void playClownfishSound(Player listener, Location location) {
        float randomPitch = randomDoubleRange(0.5, 2.0).floatValue();
        listener.playSound(randomOffset(location, 2.0), Sound.ENTITY_PLAYER_ATTACK_WEAK, SoundCategory.MASTER, 1.0F, randomPitch);
    }

    private Location randomOffset(Location a, double magnitude) {
        return a.clone().add(
                randomDoubleRange(-1.0, 1.0) * magnitude,
                randomDoubleRange(-1.0, 1.0) * magnitude,
                randomDoubleRange(-1.0, 1.0) * magnitude
        );
    }

    private Double randomDoubleRange(double min, double max) {
        return min + (random.nextDouble() * (max - min));
    }

    private net.kyori.adventure.text.format.TextColor getRandomChatColor() {
        java.awt.Color awtColor = new java.awt.Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));
        return net.kyori.adventure.text.format.TextColor.color(awtColor.getRGB());
    }
}
