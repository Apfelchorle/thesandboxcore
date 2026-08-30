package org.thesandbox.core.fun;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.thesandbox.core.TheSandboxCore;

import java.awt.*;
import java.util.Random;


// Built to reduce redundancy
public class Utils {

    private static final Random random = new Random();

    public static TextColor getRandomChatColor() {
        Color awtColor = new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));
        return TextColor.color(awtColor.getRGB());
    }

    public static void playSound(Player listener, Location location, Sound sound) {
        float randomPitch = randomDoubleRange(0.5, 2.0).floatValue();
        listener.playSound(randomOffset(location, 2.0), sound, SoundCategory.MASTER, 1.0F, randomPitch);
    }

    public static Location randomOffset(Location a, double magnitude) {
        return a.clone().add(
                randomDoubleRange(-1.0, 1.0) * magnitude,
                randomDoubleRange(-1.0, 1.0) * magnitude,
                randomDoubleRange(-1.0, 1.0) * magnitude
        );
    }

    public static void WeAllKnowWhatThisIs(TheSandboxCore plugin, Location location, Sound instrument) {
        float[] notes = { 1.0f, 1.0f, 1.2f, 1.5f, 1.2f, 2.0f };

        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                if (index >= notes.length) {
                    cancel();
                    return;
                }
                location.getWorld().playSound(location, instrument, SoundCategory.MASTER, 1.0f, notes[index]);
                index++;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }



    public static Double randomDoubleRange(double min, double max) {
        return min + (random.nextDouble() * (max - min));
    }


    // will likely be useless for complex stuff but for times
    // when u just need to send colored message it'll prove usfl! (get it, get it)
    public static void SendMessage(Player player, String message, TextColor color) {
        Component msg = Component.empty();
        msg = msg.append(Component.text(message).color(color));
        Bukkit.broadcast(msg);
    }
}