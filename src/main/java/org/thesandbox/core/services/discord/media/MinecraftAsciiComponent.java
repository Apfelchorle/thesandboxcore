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
 * <p>Supports four render styles:
 * <ul>
 *   <li>{@code BLOCK} — uses a colored block character (█) for each pixel</li>
 *   <li>{@code EMOJI} — uses colored circle emojis mapped to luminance bands</li>
 *   <li>{@code BRAILLE} — packs 2×4 subpixels into braille characters</li>
 *   <li>{@code SHADE} — uses {@code ░ ▒ ▓ █} for smooth block-resolution shading</li>
 * </ul>
 */
public final class MinecraftAsciiComponent {

    private static final TheSandboxCore core = TheSandboxCore.getInstance();
    private static final String BLOCK = core.getPluginConfigManager().getOrCreate(GenericDataKeys.DISCORD_MEDIA_BLOCK, "█");
    private static final char BRAILLE_BASE = 0x2800;
    private static final int BRAILLE_DOT_THRESHOLD = 48;
    private static final char[] SHADE_CHARS = {' ', '░', '▒', '▓', '█'};

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

        return switch (style) {
            case EMOJI -> buildEmoji(result, colors);
            case BRAILLE -> buildBraille(result, colors, legacy);
            case SHADE -> buildShade(result, colors, legacy);
            case BLOCK -> buildBlock(result, colors, legacy);
        };
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

    /**
     * Packs 2×4 source pixels into one braille character. Each dot keeps the
     * average color of the pixels that contribute to it, and same-color
     * cells are merged into a single component run so the chat JSON stays
     * compact.
     */
    private static Component buildBraille(ImageToAscii.Result result, int[][] colors, boolean legacy) {
        int cellsX = (result.width() + 1) / 2;
        int cellsY = (result.height() + 3) / 4;

        Component out = Component.empty();
        for (int cy = 0; cy < cellsY; cy++) {
            if (cy > 0) out = out.append(Component.newline());

            StringBuilder run = new StringBuilder();
            TextColor runColor = null;

            for (int cx = 0; cx < cellsX; cx++) {
                int pattern = 0;
                int sumR = 0;
                int sumG = 0;
                int sumB = 0;
                int count = 0;

                for (int dy = 0; dy < 4; dy++) {
                    int py = cy * 4 + dy;
                    if (py >= result.height()) break;

                    for (int dx = 0; dx < 2; dx++) {
                        int px = cx * 2 + dx;
                        if (px >= result.width()) continue;

                        int color = colors[py][px];
                        if (color == 0) continue;

                        int red = (color >> 16) & 0xFF;
                        int green = (color >> 8) & 0xFF;
                        int blue = color & 0xFF;
                        int luminance = (red * 54 + green * 183 + blue * 19) >> 8;
                        if (luminance < BRAILLE_DOT_THRESHOLD) continue;

                        pattern |= brailleBit(dx, dy);
                        sumR += red;
                        sumG += green;
                        sumB += blue;
                        count++;
                    }
                }

                if (pattern == 0) {
                    // Blank cell: merge with surrounding spaces regardless of color.
                    if (!run.isEmpty()) {
                        out = out.append(coloredRun(run.toString(), runColor));
                        run.setLength(0);
                    }
                    runColor = null;
                    run.append(' ');
                    continue;
                }

                int average = 0xFF000000
                        | ((sumR / count) << 16)
                        | ((sumG / count) << 8)
                        | (sumB / count);
                TextColor cellColor = resolveColor(average, legacy);

                if (!cellColor.equals(runColor)) {
                    if (!run.isEmpty()) {
                        out = out.append(coloredRun(run.toString(), runColor));
                        run.setLength(0);
                    }
                    runColor = cellColor;
                }
                run.append((char) (BRAILLE_BASE + pattern));
            }

            if (!run.isEmpty()) {
                out = out.append(coloredRun(run.toString(), runColor));
            }
        }
        return out;
    }

    /**
     * Renders one {@code ░ ▒ ▓ █} character per pixel, choosing density from
     * luminance while keeping the pixel color.
     */
    private static Component buildShade(ImageToAscii.Result result, int[][] colors, boolean legacy) {
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

                TextColor textColor = resolveColor(color, legacy);
                int shade = shadeIndex(color);
                while (x < result.width()) {
                    int next = colors[y][x];
                    if (next == 0 || shadeIndex(next) != shade || !resolveColor(next, legacy).equals(textColor)) {
                        break;
                    }
                    x++;
                }

                out = out.append(
                        Component.text(String.valueOf(SHADE_CHARS[shade]).repeat(x - start)).color(textColor)
                );
            }
        }
        return out;
    }

    /** Standard 8-dot braille mapping (dot 1 = bit 0 … dot 8 = bit 7). */
    private static int brailleBit(int dx, int dy) {
        if (dx == 0) {
            return switch (dy) {
                case 0 -> 1; // dot 1
                case 1 -> 1 << 1; // dot 2
                case 2 -> 1 << 2; // dot 3
                case 3 -> 1 << 6; // dot 7
                default -> 0;
            };
        }
        return switch (dy) {
            case 0 -> 1 << 3; // dot 4
            case 1 -> 1 << 4; // dot 5
            case 2 -> 1 << 5; // dot 6
            case 3 -> 1 << 7; // dot 8
            default -> 0;
        };
    }

    /** Maps a packed RGB value to a shade index (1 = ░ … 4 = █). */
    private static int shadeIndex(int color) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        int luminance = (red * 54 + green * 183 + blue * 19) >> 8;
        return 1 + Math.min(3, luminance * 4 / 256);
    }

    private static TextColor resolveColor(int color, boolean legacy) {
        return legacy ? Colors.nearestLegacyColor(color) : Colors.textColor(color);
    }

    private static Component coloredRun(String text, TextColor color) {
        return color == null ? Component.text(text) : Component.text(text).color(color);
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
