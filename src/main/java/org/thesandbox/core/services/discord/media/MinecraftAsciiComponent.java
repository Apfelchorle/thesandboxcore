package org.thesandbox.core.services.discord.media;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.thesandbox.core.util.Utils;
import org.thesandbox.core.util.colors.Colors;

/**
 * Converts an {@link ImageToAscii.Result} into a compact Adventure component.
 */
public final class MinecraftAsciiComponent {

    private static final String BLOCK = "\u2588";

    private MinecraftAsciiComponent() {
    }

    public static Component build(ImageToAscii.Result result, MediaRenderSettings settings) {
        if (result == null) return Component.empty();

        boolean legacy = settings.colorMode() == MediaRenderSettings.ColorMode.LEGACY;
        int[][] colors = result.colors();

        Component out = Component.empty();
        for (int y = 0; y < result.height(); y++) {
            if (y > 0) out = out.append(Component.newline());

            int x = 0;
            while (x < result.width()) {
                int color = colors[y][x];
                int start = x;

                if (color == 0) {
                    while (x < result.width() && colors[y][x] == 0) x++;
                    out = out.append(Component.text(" ".repeat(x - start)));
                    continue;
                }

                TextColor textColor = legacy
                        ? Colors.nearestLegacyColor(color)
                        : Colors.textColor(color);

                if (legacy) {
                    NamedTextColor named = (NamedTextColor) textColor;
                    while (x < result.width()
                            && colors[y][x] != 0
                            && Colors.nearestLegacyColor(colors[y][x]) == named) {
                        x++;
                    }
                } else {
                    while (x < result.width() && colors[y][x] == color) x++;
                }

                out = out.append(
                        Component.text(BLOCK.repeat(x - start)).color(textColor)
                );
            }
        }
        return out;
    }

    public static Component clickable(Component art, String url) {
        if (art == null || url == null || url.isBlank()) return art == null ? Component.empty() : art;
        return art
                .clickEvent(ClickEvent.openUrl(url))
                .hoverEvent(HoverEvent.showText(Utils.legacy("&6&oClick to view in Discord")));
    }
}
