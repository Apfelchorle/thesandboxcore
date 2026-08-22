package org.thesandbox.core.guilds;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {
    private ColorUtil() {}

    // after & -> §, user hex looks like §#RRGGBB
    private static final Pattern HEX = Pattern.compile("§#([0-9a-fA-F]{6})");

    private static final LegacyComponentSerializer SERIALIZER =
            LegacyComponentSerializer.builder()
                    .character('§')
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat() // supports §x§R§R§G§G§B§B
                    .build();

    public static Component toComponent(String input) {
        if (input == null) return Component.empty();
        String s = input.replace('&', '§');
        s = expandHex(s);
        return SERIALIZER.deserialize(s);
    }

    private static String expandHex(String s) {
        Matcher m = HEX.matcher(s);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String hex = m.group(1);
            String repl = "§x"
                    + "§" + hex.charAt(0) + "§" + hex.charAt(1)
                    + "§" + hex.charAt(2) + "§" + hex.charAt(3)
                    + "§" + hex.charAt(4) + "§" + hex.charAt(5);
            m.appendReplacement(out, Matcher.quoteReplacement(repl));
        }
        m.appendTail(out);
        return out.toString();
    }
}
