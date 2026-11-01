package org.thesandbox.core.commands.player;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.commands.CommandMessages;
import org.thesandbox.core.util.colors.Colors;

import java.util.List;

public class GlowCommand extends BaseCommand {

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            return true;
        }

        // Toggle glowing
        boolean nowGlowing;
        if (player.hasPotionEffect(PotionEffectType.GLOWING)) {
            player.removePotionEffect(PotionEffectType.GLOWING);
            nowGlowing = false;
        } else {
            // Long duration, no particles, not ambient
            PotionEffect glow = new PotionEffect(PotionEffectType.GLOWING, 1_000_000, 0, false, false, false);
            player.addPotionEffect(glow);
            nowGlowing = true;
        }

        player.sendMessage(CommandMessages.command(Colors.GRAY + "You " + (nowGlowing ? "now" : "no longer") + " have the glowing outline effect."));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
