package org.thesandbox.core.util;

/**
 * Compatibility facade kept for older call sites. All work is done by {@link Colors}.
 */
public final class HexColorUtil {

    private HexColorUtil() {
    }

    public static String toLegacyHex(String hex) {
        return Colors.toLegacyHex(hex);
    }

    public static String translate(String input) {
        return Colors.color(input);
    }

    public static String stripColors(String input) {
        return Colors.strip(input);
    }
}
