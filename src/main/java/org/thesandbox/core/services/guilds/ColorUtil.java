package org.thesandbox.core.services.guilds;

import net.kyori.adventure.text.Component;
import org.thesandbox.core.util.colors.Colors;

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
