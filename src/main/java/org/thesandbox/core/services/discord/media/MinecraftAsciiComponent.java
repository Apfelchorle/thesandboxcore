package org.thesandbox.core.services.discord.media;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.data.GenericDataKeys;
import org.thesandbox.core.util.Utils;
import org.thesandbox.core.util.colors.Colors;

/**
 * Converts an {@link ImageToAscii.Result} into a compact Adventure component.
 *
 * <p>Supports two render styles:
 * <ul>
 *   <li>{@code BLOCK} — uses a colored block character (█) for each pixel</li>
 *   <li>{@code EMOJI} — uses colored circle emojis mapped to luminance bands</li>
 * </ul>
 */
public final class MinecraftAsciiComponent {

    private static final TheSandboxCore core = TheSandboxCore.getInstance();
    private static final String BLOCK = core.getPluginConfigManager().getOrCreate(GenericDataKeys.DISCORD_MEDIA_BLOCK, "█");

    // Emoji palette: 10 luminance bands mapped to colored circle emojis.
    // Ordered from darkest (index 0) to brightest (index 9).
    private static final String[] EMOJI_PALETTE = {
            "⚫",  // 0  — black (darkest)
            "🟤",  // 1  — brown
            "🔵",  // 2  — blue
            "🟣",  // 3  — purple
            "🟢",  // 4  — green
            "🟡",  // 5  — yellow
            "🟠",  // 6  — orange
            "🔴",  // 7  — red
            "⚪",  // 8  — white (light)
            "🩷"   // 9  — pink (brightest / accent)
    };

    private MinecraftAsciiComponent() {
    }

    public static Component build(ImageToAscii.Result result, MediaRenderSettings settings) {
        if (result == null) return Component.empty();

        boolean legacy = settings.colorMode() == MediaRenderSettings.ColorMode.LEGACY;
        MediaRenderSettings.RenderStyle style = settings.renderStyle();
        int[][] colors = result.colors();

        if (style == MediaRenderSettings.RenderStyle.EMOJI) {
            return buildEmoji(result, colors);
        }

        return buildBlock(result, colors, legacy);
    }

    /**
     * Renders the image using emoji circles mapped to luminance bands.
     * Each pixel's relative luminance determines which emoji is used.
     */
    private static Component buildEmoji(ImageToAscii.Result result, int[][] colors) {
        Component out = Component.empty();
        for (int y = 0; y < result.height(); y++) {
            if (y > 0) out = out.append(Component.newline());

            for (int x = 0; x < result.width(); x++) {
                int color = colors[y][x];
                if (color == 0) {
                    out = out.append(Component.text(" "));
                    continue;
                }

                // Compute relative luminance (BT.709 weights)
                int red = (color >> 16) & 0xFF;
                int green = (color >> 8) & 0xFF;
                int blue = color & 0xFF;
                double luminance = 0.2126 * red + 0.7152 * green + 0.0722 * blue;

                // Map to emoji band (0-9)
                int band = (int) (luminance / 26.6); // 256 / 10 ≈ 25.6, use 26.6 for slight dark bias
                if (band < 0) band = 0;
                if (band >= EMOJI_PALETTE.length) band = EMOJI_PALETTE.length - 1;

                out = out.append(Component.text(EMOJI_PALETTE[band]));
            }
        }
        return out;
    }

    private static Component buildBlock(ImageToAscii.Result result, int[][] colors, boolean legacy) {
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
