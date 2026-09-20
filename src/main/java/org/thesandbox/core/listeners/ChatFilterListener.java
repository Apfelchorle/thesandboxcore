package org.thesandbox.core.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.thesandbox.core.fun.Utils;
import org.thesandbox.core.util.GenericDataKeys;
import org.thesandbox.core.util.PluginConfigManager;
import org.thesandbox.core.util.RankKeys;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatFilterListener implements Listener {

    private static final String CENSOR_TEXT = "[ Not So nice Words ;( ]";

    private final PluginConfigManager configManager;

    public ChatFilterListener(PluginConfigManager configManager) {
        this.configManager = configManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission(RankKeys.STAFF)) return;

        List<String> blockedWords = configManager.getOrCreate(GenericDataKeys.CHATFILTER, GenericDataKeys.BADWORDS);
        String plain = Utils.plainText(event.message());

        String censored = plain;
        boolean triggered = false;

        for (String word : blockedWords) {
            if (word == null || word.isBlank()) continue;

            Pattern pattern = buildBypassTolerantPattern(word);
            if (pattern.matcher(censored).find()) {
                censored = pattern.matcher(censored).replaceAll(Matcher.quoteReplacement(CENSOR_TEXT));
                triggered = true;
            }
        }

        if (triggered) {
            event.message(Component.text(censored));

            Component staffAlert = Component.text(
                    "[ChatFilter] " + player.getName() + " triggered the chat filter. Original: " + plain,
                    NamedTextColor.GRAY);
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.hasPermission("sandbox.staff")) {
                    staff.sendMessage(staffAlert);
                }
            }
        }
    }

    private Pattern buildBypassTolerantPattern(String word) {
        StringBuilder regex = new StringBuilder();
        for (char c : word.toCharArray()) {
            String escaped = Pattern.quote(String.valueOf(c));
            regex.append(escaped).append("+");
            regex.append("[\\s._\\-*]*");
        }
        return Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE);
    }
}