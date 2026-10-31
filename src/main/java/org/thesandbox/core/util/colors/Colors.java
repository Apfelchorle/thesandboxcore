package org.thesandbox.core.util.colors;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;

import java.awt.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Single entry point for legacy colour handling.
 *
 * <p>Commands still speak in legacy strings, but every conversion now goes through
 * this class instead of the deprecated {@code ChatColor} API. The section-sign
 * constants keep existing string concatenation working while the component helpers
 * provide the Adventure path for newer code.</p>
 */
public final class Colors {

    public static final String BLACK = "\u00a70";
    public static final String DARK_BLUE = "\u00a71";
    public static final String DARK_GREEN = "\u00a72";
    public static final String DARK_AQUA = "\u00a73";
    public static final String DARK_RED = "\u00a74";
    public static final String DARK_PURPLE = "\u00a75";
    public static final String GOLD = "\u00a76";
    public static final String GRAY = "\u00a77";
    public static final String DARK_GRAY = "\u00a78";
    public static final String BLUE = "\u00a79";
    public static final String GREEN = "\u00a7a";
    public static final String AQUA = "\u00a7b";
    public static final String RED = "\u00a7c";
    public static final String LIGHT_PURPLE = "\u00a7d";
    public static final String YELLOW = "\u00a7e";
    public static final String WHITE = "\u00a7f";
    public static final String MAGIC = "\u00a7k";
    public static final String BOLD = "\u00a7l";
    public static final String STRIKETHROUGH = "\u00a7m";
    public static final String UNDERLINE = "\u00a7n";
    public static final String ITALIC = "\u00a7o";
    public static final String RESET = "\u00a7r";
    /**
     * All 16 plain colours, in Bukkit's historical order.
     */
    public static final List<String> LEGACY_COLORS = List.of(
            BLACK, DARK_BLUE, DARK_GREEN, DARK_AQUA,
            DARK_RED, DARK_PURPLE, GOLD, GRAY,
            DARK_GRAY, BLUE, GREEN, AQUA,
            RED, LIGHT_PURPLE, YELLOW, WHITE
    );
    private static final Pattern SECTION_HEX = Pattern.compile("(?i)\u00a7#([0-9a-f]{6})");
    private static final Pattern AMP_HEX = Pattern.compile("(?i)&?#([0-9a-f]{6})");
    private static final Pattern LEGACY_HEX =
            Pattern.compile("(?i)(?:\u00a7x((?:\u00a7[0-9a-f]){6})|&x((?:&[0-9a-f]){6}))");
    private static final LegacyComponentSerializer SECTION_SERIALIZER =
            LegacyComponentSerializer.builder()
                    .character('\u00a7')
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat()
                    .build();

    private Colors() {
    }

    public static String of(Color color) {
        return toLegacyHex(color);
    }

    public static String toLegacyHex(Color color) {
        if (color == null) return "";
        return toLegacyHex(String.format("%02X%02X%02X",
                color.getRed(), color.getGreen(), color.getBlue()));
    }

    public static String toLegacyHex(String hex) {
        if (hex == null) return "";
        hex = hex.replace("#", "");
        if (hex.length() != 6 || !hex.matches("(?i)[0-9a-f]{6}")) return "";
        StringBuilder sb = new StringBuilder("\u00a7x");
        for (char c : hex.toCharArray()) {
            sb.append('\u00a7').append(Character.toUpperCase(c));
        }
        return sb.toString();
    }

    /**
     * Maps a legacy colour string such as {@code §c} to an Adventure colour.
     */
    public static NamedTextColor namedColor(String legacy) {
        if (legacy == null || legacy.length() < 2) return NamedTextColor.WHITE;
        return switch (Character.toLowerCase(legacy.charAt(1))) {
            case '0' -> NamedTextColor.BLACK;
            case '1' -> NamedTextColor.DARK_BLUE;
            case '2' -> NamedTextColor.DARK_GREEN;
            case '3' -> NamedTextColor.DARK_AQUA;
            case '4' -> NamedTextColor.DARK_RED;
            case '5' -> NamedTextColor.DARK_PURPLE;
            case '6' -> NamedTextColor.GOLD;
            case '7' -> NamedTextColor.GRAY;
            case '8' -> NamedTextColor.DARK_GRAY;
            case '9' -> NamedTextColor.BLUE;
            case 'a' -> NamedTextColor.GREEN;
            case 'b' -> NamedTextColor.AQUA;
            case 'c' -> NamedTextColor.RED;
            case 'd' -> NamedTextColor.LIGHT_PURPLE;
            case 'e' -> NamedTextColor.YELLOW;
            default -> NamedTextColor.WHITE;
        };
    }

    public static String plain(Component component) {
        return component == null
                ? ""
                : PlainTextComponentSerializer.plainText().serialize(component);
    }

    /**
     * Compatibility alias for the old guild text helper.
     */
    public static Component c(String legacyAndHex) {
        return component(legacyAndHex);
    }

    /**
     * Serialises a legacy or ampersand string into an Adventure component.
     */
    public static Component component(String input) {
        if (input == null || input.isEmpty()) return Component.empty();
        return SECTION_SERIALIZER.deserialize(color(input));
    }

    /**
     * Converts {@code &}-codes and {@code #RRGGBB}/{@code &#RRGGBB}/{@code §#RRGGBB}
     * into section-sign legacy Colors. The repeated legacy hex form
     * ({@code &x&R&R&G&G&B&B} / {@code §x§R§R§G§G§B§B}) is normalised as well.
     * This replaces {@code ChatColor.translateAlternateColorCodes('&', ...)}.
     */
    public static String color(String input) {
        if (input == null || input.isEmpty()) return input;

        String out = replaceLegacyHex(input);
        out = replaceHex(out, SECTION_HEX);
        out = replaceHex(out, AMP_HEX);

        StringBuilder sb = new StringBuilder(out.length());
        for (int i = 0; i < out.length(); i++) {
            char c = out.charAt(i);
            if (c == '&' && i + 1 < out.length() && isLegacyCode(out.charAt(i + 1))) {
                sb.append('\u00a7').append(Character.toLowerCase(out.charAt(i + 1)));
                i++;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Rewrites the repeated legacy hex format to the canonical {@code §x§R§R§G§G§B§B} form.
     */
    private static String replaceLegacyHex(String input) {
        Matcher matcher = LEGACY_HEX.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String sequence = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            StringBuilder hex = new StringBuilder(6);
            for (int i = 1; i < sequence.length(); i += 2) {
                hex.append(sequence.charAt(i));
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(toLegacyHex(hex.toString())));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String replaceHex(String input, Pattern pattern) {
        Matcher matcher = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement(toLegacyHex(matcher.group(1))));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static boolean isLegacyCode(char c) {
        return (c >= '0' && c <= '9')
                || (c >= 'a' && c <= 'f')
                || (c >= 'k' && c <= 'o')
                || (c >= 'r' && c <= 'r')
                || (c >= 'A' && c <= 'F')
                || (c >= 'K' && c <= 'O')
                || (c >= 'R' && c <= 'R');
    }

    /**
     * Compatibility alias for the old guild text helper.
     */
    public static Component plain(String input) {
        return Component.text(input == null ? "" : input);
    }

    /**
     * Removes legacy colours and hex sequences.
     */
    public static String strip(String input) {
        if (input == null) return "";
        String out = color(input);
        out = LEGACY_HEX.matcher(out).replaceAll("");
        out = out.replaceAll("(?i)\u00a7#[0-9A-F]{6}", "");
        return out.replaceAll("(?i)[\u00a7&][0-9A-FK-OR]", "");
    }

    /**
     * Replaces the deprecated {@code Bukkit.broadcastMessage(String)}.
     */
    public static void broadcast(String input) {
        Bukkit.getServer().broadcast(component(input));
    }
}
