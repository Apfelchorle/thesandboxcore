package org.thesandbox.core.services.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.data.GenericDataKeys;
import org.thesandbox.core.util.config.PluginConfigManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatFilterEngine {

    /**
     * Upper bound on text inspected by the filter. Chat messages are already
     * shorter than this; the cap keeps a pathological caller from running the
     * combined regex over an unbounded string.
     */
    private static final int MAX_SCAN_LENGTH = 4096;

    private final PluginConfigManager configManager;
    private final TheSandboxCore plugin;
    private volatile Pattern cachedPattern;

    public ChatFilterEngine(
            PluginConfigManager configManager,
            TheSandboxCore plugin
    ) {
        this.configManager = configManager;
        this.plugin = plugin;
        reload();
    }

    public synchronized void reload() {
        List<String> blockedWords = configManager.getOrCreateCustom(
                GenericDataKeys.CHATFILTER,
                GenericDataKeys.BADWORDS,
                file()
        );
        StringBuilder combinedRegex = new StringBuilder();
        for (String word : blockedWords) {
            if (word == null || word.isBlank()) continue;
            if (!combinedRegex.isEmpty()) combinedRegex.append("|");
            combinedRegex
                    .append("(")
                    .append(buildBypassTolerantRegex(word))
                    .append(")");
        }

        if (combinedRegex.isEmpty()) {
            this.cachedPattern = null;
        } else {
            this.cachedPattern = Pattern.compile(
                    combinedRegex.toString(),
                    Pattern.CASE_INSENSITIVE
            );
        }
    }

    private File file() {
        return new File(plugin.getDataFolder(), "words.yml");
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

    public Result scan(String plain) {
        Pattern pattern = this.cachedPattern;
        List<String> detectedBadWords = new ArrayList<>();

        if (pattern == null || plain == null || plain.isBlank()) {
            return new Result(false, plain, plain, null, detectedBadWords);
        }

        String scanText = plain.length() > MAX_SCAN_LENGTH
                ? plain.substring(0, MAX_SCAN_LENGTH)
                : plain;
        String remainder = scanText.length() < plain.length()
                ? plain.substring(scanText.length())
                : "";

        Matcher matcher = pattern.matcher(scanText);
        boolean triggered = false;

        StringBuilder censoredPlain = new StringBuilder(scanText.length() + 16);
        TextComponent.Builder censored = Component.text();
        int lastEnd = 0;

        while (matcher.find()) {
            triggered = true;
            String match = matcher.group();
            detectedBadWords.add(match);

            censoredPlain
                    .append(scanText, lastEnd, matcher.start())
                    .append("&4****&r");

            censored.append(Component.text(scanText.substring(lastEnd, matcher.start())));
            censored.append(censorToken(match));

            lastEnd = matcher.end();
        }

        if (!triggered) {
            return new Result(false, plain, plain, null, detectedBadWords);
        }

        censoredPlain.append(scanText, lastEnd, scanText.length()).append(remainder);
        if (lastEnd < scanText.length()) {
            censored.append(Component.text(scanText.substring(lastEnd)));
        }
        if (!remainder.isEmpty()) {
            censored.append(Component.text(remainder));
        }

        return new Result(
                true,
                plain,
                censoredPlain.toString(),
                censored.build(),
                detectedBadWords
        );
    }

    private Component censorToken(String originalBadWord) {
        return Component.text("****")
                .color(TextColor.fromHexString("#FF5555"))
                .hoverEvent(
                        HoverEvent.showText(
                                Component.text(
                                        "Censored Word: " + originalBadWord,
                                        NamedTextColor.RED
                                )
                        )
                );
    }

    public String generateGrawlix(int wordLength) {
        String symbols = "$%#&!";
        java.util.Random random = new java.util.Random();
        int variance = random.nextInt(5) - 2;
        int finalLength = Math.max(3, wordLength + variance);
        StringBuilder result = new StringBuilder(finalLength);
        for (int i = 0; i < finalLength; i++) {
            result.append(symbols.charAt(random.nextInt(symbols.length())));
        }
        return result.toString();
    }

    public static class Result {

        public final boolean triggered;
        public final String plain;
        public final String censoredPlain;
        public final Component censoredComponent;
        public final List<String> detectedBadWords;

        Result(
                boolean triggered,
                String plain,
                String censoredPlain,
                Component censoredComponent,
                List<String> detectedBadWords
        ) {
            this.triggered = triggered;
            this.plain = plain;
            this.censoredPlain = censoredPlain;
            this.censoredComponent = censoredComponent;
            this.detectedBadWords = detectedBadWords;
        }
    }
}
