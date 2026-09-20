package org.thesandbox.core.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.thesandbox.core.fun.Utils;
import org.thesandbox.core.util.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatFilterListener implements Listener {

    private final PluginConfigManager configManager;
    private final PlayerDataListener playerDataListener;

    public ChatFilterListener(PluginConfigManager configManager, PlayerDataListener playerDataListener) {
        this.configManager = configManager;
        this.playerDataListener = playerDataListener;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player sender = event.getPlayer();
        if (sender.hasPermission(RankKeys.STAFF)) return;

        List<String> blockedWords = configManager.getOrCreate(GenericDataKeys.CHATFILTER, GenericDataKeys.BADWORDS);
        String plain = Utils.plainText(event.message());

        List<String> detectedBadWords = new ArrayList<>();
        boolean triggered = false;

        StringBuilder combinedRegex = new StringBuilder();
        for (String word : blockedWords) {
            if (word == null || word.isBlank()) continue;
            if (!combinedRegex.isEmpty()) combinedRegex.append("|");
            combinedRegex.append("(").append(buildBypassTolerantRegex(word)).append(")");
        }

        if (combinedRegex.isEmpty()) return;

        Pattern pattern = Pattern.compile(combinedRegex.toString(), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(plain);

        while (matcher.find()) {
            detectedBadWords.add(matcher.group());
            triggered = true;
        }

        if (triggered) {
            Component censoredComponent = buildCensoredComponent(plain, pattern, detectedBadWords);

            event.renderer((source, sourceDisplayName, message, viewer) -> {
                if (viewer instanceof Player recipient) {


                    boolean shouldCensor = playerDataListener.get(recipient.getUniqueId(), PlayerDataKeys.CHATFILTER, false);


                    if (!shouldCensor) {
                        return sourceDisplayName.append(Component.text(": ")).append(event.message());
                    }
                }

                return sourceDisplayName.append(Component.text(": ")).append(censoredComponent);
            });

            Component staffAlert = Component.text(
                    "[ChatFilter] " + sender.getName() + " triggered the chat filter. Original: " + plain,
                    NamedTextColor.DARK_GRAY);
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.hasPermission("sandbox.staff")) {
                    staff.sendMessage(staffAlert);
                }
            }
        }
    }

    private Component buildCensoredComponent(String originalText, Pattern pattern, List<String> badWords) {
        TextComponent.Builder builder = Component.text();
        Matcher matcher = pattern.matcher(originalText);

        int lastEnd = 0;
        int wordIndex = 0;

        while (matcher.find() && wordIndex < badWords.size()) {
            builder.append(Component.text(originalText.substring(lastEnd, matcher.start())));

            String originalBadWord = badWords.get(wordIndex++);

            Component censorToken = Component.text("[CENSORED]")
                    .color(NamedTextColor.BLACK)
                    .decorate(TextDecoration.BOLD)
                    .hoverEvent(HoverEvent.showText(Component.text("Censored Word: " + originalBadWord, NamedTextColor.RED)));

            builder.append(censorToken);
            lastEnd = matcher.end();
        }

        if (lastEnd < originalText.length()) {
            builder.append(Component.text(originalText.substring(lastEnd)));
        }

        return builder.build();
    }

    private String buildBypassTolerantRegex(String word) {
        StringBuilder regex = new StringBuilder();
        for (char c : word.toCharArray()) {
            String escaped = Pattern.quote(String.valueOf(c));
            regex.append(escaped).append("+");
            regex.append("[\\s._\\-*]*");
        }
        return regex.toString();
    }
}
