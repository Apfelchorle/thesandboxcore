package org.thesandbox.core.commands;

/**
 * Backwards-compatible facade over {@link MessageService}.
 *
 * <p>Existing commands speak in legacy strings, so these methods still return
 * Strings. They now read their prefixes from {@code messages.yml}, which gives
 * every command one theme without touching hundreds of call sites.</p>
 */
public final class CommandMessages {

    private CommandMessages() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String command(String message) {
        return MessageService.legacy(MessageService.Prefix.COMMAND, message);
    }

    public static String error(String message) {
        return MessageService.legacy(MessageService.Prefix.ERROR, message);
    }

    public static String usage(String message) {
        return MessageService.legacy(MessageService.Prefix.USAGE, usageBody(message));
    }

    public static String server(String message) {
        return MessageService.legacy(MessageService.Prefix.SERVER, message);
    }

    public static String information(String message) {
        return MessageService.legacy(MessageService.Prefix.INFO, message);
    }

    public static String success(String message) {
        return MessageService.legacy(MessageService.Prefix.SUCCESS, message);
    }

    /**
     * Removes a leading {@code Usage:}/{@code Usage: } so the themed prefix is
     * not followed by a redundant label.
     */
    public static String usageBody(String message) {
        String body = message == null ? "" : stripLeadingColor(message);
        if (body.regionMatches(true, 0, "Usage: ", 0, 7)) {
            return body.substring(7);
        }
        if (body.regionMatches(true, 0, "Usage:", 0, 6)) {
            return body.substring(6).stripLeading();
        }
        return body;
    }

    /**
     * Removes leading legacy colour/formatting codes from a string.
     */
    public static String stripLeadingColor(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        String value = input;
        boolean changed = true;

        while (changed && !value.isEmpty()) {
            changed = false;

            if (value.length() >= 14 && (value.charAt(0) == '§' || value.charAt(0) == '&')
                    && Character.toLowerCase(value.charAt(1)) == 'x') {
                char marker = value.charAt(0);
                boolean valid = true;
                for (int i = 2; i < 14; i += 2) {
                    if (value.charAt(i) != marker || !isHex(value.charAt(i + 1))) {
                        valid = false;
                        break;
                    }
                }
                if (valid) {
                    value = value.substring(14);
                    changed = true;
                    continue;
                }
            }

            // Standard hex (&#ffffff or §#ffffff)
            if (value.length() >= 8 && (value.startsWith("&#") || value.startsWith("§#"))) {
                boolean valid = true;
                for (int i = 2; i < 8; i++) {
                    if (!isHex(value.charAt(i))) {
                        valid = false;
                        break;
                    }
                }
                if (valid) {
                    value = value.substring(8);
                    changed = true;
                    continue;
                }
            }

            // Standard legacy colour/formatting codes (&0-&f, &k-&o, &r)
            if (value.length() >= 2 && (value.charAt(0) == '§' || value.charAt(0) == '&')) {
                char code = Character.toLowerCase(value.charAt(1));
                if (isFormattingOrColor(code)) {
                    value = value.substring(2);
                    changed = true;
                }
            }
        }

        return value;
    }

    private static boolean isFormattingOrColor(char code) {
        return (code >= '0' && code <= '9')
                || (code >= 'a' && code <= 'f')
                || (code >= 'k' && code <= 'o')
                || code == 'r';
    }

    public static boolean isHex(char c) {
        c = Character.toLowerCase(c);
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
    }
}
