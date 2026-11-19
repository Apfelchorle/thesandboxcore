package org.thesandbox.core.services.discord.media;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftAsciiComponentTest {

    private static MediaRenderSettings settings(MediaRenderSettings.RenderStyle style) {
        return new MediaRenderSettings(
                true, true, true, false, false, true,
                40, 22, 0.9,
                MediaRenderSettings.ColorMode.HEX, style, 64, 32,
                0x1F1F1F, 2000L, 128, 12, 3,
                8 * 1024 * 1024, 20_000_000, 256, false
        );
    }

    @Test
    void nullResultIsEmpty() {
        assertEquals(Component.empty(), MinecraftAsciiComponent.build(null, settings(MediaRenderSettings.RenderStyle.BLOCK)));
    }

    @Test
    void oversizedResultIsGuarded() {
        int[][] colors = new int[65][64];
        for (int[] row : colors) {
            for (int i = 0; i < row.length; i++) row[i] = 0xFFFF0000;
        }
        ImageToAscii.Result result = new ImageToAscii.Result(64, 65, colors);
        Component built = MinecraftAsciiComponent.build(result, settings(MediaRenderSettings.RenderStyle.BLOCK));
        assertFalse(built.toString().isBlank());
        assertTrue(built.toString().contains("too large"));
    }

    @Test
    void darkBraillePixelsAreNotDropped() {
        // A single very dark (but opaque) subpixel must still produce a dot.
        int[][] colors = new int[4][2];
        colors[0][0] = 0xFF101010;
        ImageToAscii.Result result = new ImageToAscii.Result(2, 4, colors);

        Component built = MinecraftAsciiComponent.build(result, settings(MediaRenderSettings.RenderStyle.BRAILLE));

        assertFalse(built.toString().isBlank());
    }

    @Test
    void halfBlockRenders() {
        int[][] colors = new int[2][2];
        colors[0][0] = 0xFFFF0000;
        colors[1][1] = 0xFF00FF00;
        ImageToAscii.Result result = new ImageToAscii.Result(2, 2, colors);

        Component built = MinecraftAsciiComponent.build(result, settings(MediaRenderSettings.RenderStyle.HALF_BLOCK));

        assertFalse(built.toString().isBlank());
    }
}
