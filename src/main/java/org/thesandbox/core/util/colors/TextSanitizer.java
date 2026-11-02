package org.thesandbox.core.util.colors;

import java.util.Locale;
import java.util.Set;

/**
 * Sanitises player-supplied MiniMessage before it is deserialised.
 *
 * <p>Only colour and decoration tags survive. Interactive/structural tags
 * ({@code click}, {@code hover}, {@code font}, {@code keybind}, {@code score},
 * {@code selector}, {@code nbt}, {@code translatable}, ...) are removed so a
 * player cannot create clickable commands, phishing links, or spoofed UI.</p>
 *
 * <p>The scanner is deliberately not a simple regex. A tag can contain nested
 * angle brackets (for example {@code <hover:show_text:'<click:...>'>}), and a
 * regex that strips only the inner tag can leave a valid interactive tag
 * behind. This implementation finds the matching {@code >} for each tag and
 * drops the entire tag when its name is not allowlisted.</p>
 */
public final class TextSanitizer {

    private static final Set<String> ALLOWED_TAGS = Set.of(
            // Colours
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red",
            "dark_purple", "gold", "gray", "grey", "dark_gray", "dark_grey",
            "blue", "green", "aqua", "red", "light_purple", "yellow", "white",
            // Decorations
            "bold", "b", "italic", "i", "underlined", "u",
            "strikethrough", "st", "obfuscated", "obf", "reset", "r",
            // Gradients / special colour tags
            "gradient", "rainbow", "transition", "color", "c", "newline"
    );

    private TextSanitizer() {
    }

    /**
     * Removes every tag that is not a colour or decoration tag. Text outside
     * tags is left untouched, including ordinary comparison symbols such as
     * {@code 1 < 2}.
     */
    public static String sanitizeMiniMessage(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        StringBuilder out = new StringBuilder(input.length());
        int index = 0;
        while (index < input.length()) {
            char current = input.charAt(index);
            if (current != '<') {
                out.append(current);
                index++;
                continue;
            }

            int end = findTagEnd(input, index);
            if (end < 0) {
                out.append(current);
                index++;
                continue;
            }

            String body = input.substring(index + 1, end);
            if (!looksLikeTag(body)) {
                // Not a MiniMessage tag (for example "1 < 2 and 3 > 2").
                out.append(current);
                index++;
                continue;
            }

            boolean nestedMarkup = body.indexOf('<') >= 0 || body.indexOf('>') >= 0;
            if (!nestedMarkup && isAllowed(tagName(body))) {
                out.append(input, index, end + 1);
            }
            index = end + 1;
        }
        return out.toString();
    }

    /**
     * True when the input contains something that actually looks like a
     * MiniMessage tag. Plain comparisons such as {@code 1 < 2} do not count.
     */
    public static boolean looksLikeMiniMessage(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        int index = 0;
        while (index < input.length()) {
            int start = input.indexOf('<', index);
            if (start < 0) {
                return false;
            }
            int end = findTagEnd(input, start);
            if (end < 0) {
                return false;
            }
            if (looksLikeTag(input.substring(start + 1, end))) {
                return true;
            }
            index = end + 1;
        }
        return false;
    }

    /**
     * Finds the {@code >} that closes the tag starting at {@code start}. Nested
     * angle brackets are counted so the whole outer tag is treated as one unit.
     */
    private static int findTagEnd(String input, int start) {
        int depth = 0;
        for (int i = start; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * MiniMessage tag names start with a letter, {@code #}, {@code /},
     * {@code !} or {@code ?}. Everything else is ordinary text.
     */
    private static boolean looksLikeTag(String body) {
        if (body.isEmpty()) {
            return false;
        }
        char first = body.charAt(0);
        return Character.isLetter(first)
                || first == '#'
                || first == '/'
                || first == '!'
                || first == '?';
    }

    private static boolean isAllowed(String name) {
        // <#ff00ff> true-colour tags have no name before the colon.
        return "#".equals(name) || ALLOWED_TAGS.contains(name);
    }

    private static String tagName(String body) {
        String value = body;
        if (value.startsWith("/") || value.startsWith("!")) {
            value = value.substring(1);
        }
        if (value.startsWith("#")) {
            return "#";
        }

        int end = value.length();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == ':' || Character.isWhitespace(c)) {
                end = i;
                break;
            }
        }
        return value.substring(0, end).toLowerCase(Locale.ROOT);
    }
}
