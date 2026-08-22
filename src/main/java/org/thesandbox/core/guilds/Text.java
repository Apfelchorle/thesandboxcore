package org.thesandbox.core.guilds;

import net.kyori.adventure.text.Component;

public final class Text {
    private Text() {}

    public static Component c(String legacyAndHex) {
        return ColorUtil.toComponent(legacyAndHex);
    }

    public static Component plain(String s) {
        return Component.text(s);
    }
}
