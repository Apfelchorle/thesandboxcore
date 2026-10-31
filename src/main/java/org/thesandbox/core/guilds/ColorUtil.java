package org.thesandbox.core.guilds;

import net.kyori.adventure.text.Component;
import org.thesandbox.core.util.Colors;

/**
 * Guild colour helpers now delegate to the shared {@link Colors} utility.
 */
public final class ColorUtil {

    private ColorUtil() {
    }

    public static Component toComponent(String input) {
        return Colors.component(input);
    }
}
