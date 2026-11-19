package org.thesandbox.core.services.discord.media;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageToAsciiTest {

    private static MediaRenderSettings settings(
            int width,
            int height,
            MediaRenderSettings.RenderStyle style,
            int paletteSize
    ) {
        return new MediaRenderSettings(
                true, true, true, false, false, true,
                width, height, 0.9,
                MediaRenderSettings.ColorMode.HEX, style, paletteSize, 32,
                0x1F1F1F, 2000L, 128, 12, 3,
                8 * 1024 * 1024, 20_000_000, 256, false
        );
    }

    private static BufferedImage solid(int width, int height, int argb) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(argb, true));
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    @Test
    void nullInputsReturnNull() {
        assertNull(ImageToAscii.render(null, settings(40, 22, MediaRenderSettings.RenderStyle.BLOCK, 64)));
        assertNull(ImageToAscii.render(solid(8, 8, 0xFFFF0000), null));
    }

    @Test
    void fullyTransparentImageReturnsNull() {
        BufferedImage transparent = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        assertNull(ImageToAscii.render(
                transparent,
                settings(40, 22, MediaRenderSettings.RenderStyle.BLOCK, 64)));
    }

    @Test
    void solidColorSurvivesDownsampling() {
        MediaRenderSettings settings = settings(40, 22, MediaRenderSettings.RenderStyle.BLOCK, 64);
        ImageToAscii.Result result = ImageToAscii.render(solid(100, 100, 0xFFFF0000), settings);

        assertNotNull(result);
        assertEquals(0xFFFF0000, result.colors()[0][0]);
        for (int[] row : result.colors()) {
            for (int color : row) {
                assertEquals(0xFFFF0000, color);
            }
        }
    }

    @Test
    void transparentBorderIsTrimmed() {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(0xFF0000FF, true));
            graphics.fillRect(16, 16, 32, 32);
        } finally {
            graphics.dispose();
        }

        ImageToAscii.Result result = ImageToAscii.render(
                image,
                settings(40, 22, MediaRenderSettings.RenderStyle.BLOCK, 64));

        assertNotNull(result);
        // The untrimmed block target is 24x22 for this aspect; the opaque
        // centre must produce a smaller grid.
        assertTrue(result.width() < 24, "width was " + result.width());
        assertTrue(result.height() < 22, "height was " + result.height());
    }

    @Test
    void paletteQuantizationCapsUniqueColors() {
        BufferedImage gradient = new BufferedImage(120, 80, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = gradient.createGraphics();
        try {
            for (int y = 0; y < gradient.getHeight(); y++) {
                for (int x = 0; x < gradient.getWidth(); x++) {
                    int red = (x * 255) / (gradient.getWidth() - 1);
                    int green = (y * 255) / (gradient.getHeight() - 1);
                    gradient.setRGB(x, y, 0xFF000000 | (red << 16) | (green << 8) | 0x40);
                }
            }
        } finally {
            graphics.dispose();
        }

        ImageToAscii.Result result = ImageToAscii.render(
                gradient,
                settings(40, 22, MediaRenderSettings.RenderStyle.BLOCK, 4));

        assertNotNull(result);
        Set<Integer> unique = new HashSet<>();
        for (int[] row : result.colors()) {
            for (int color : row) {
                if (color != 0) unique.add(color);
            }
        }
        assertTrue(unique.size() <= 4, "unique colors: " + unique.size());
    }

    @Test
    void extremeAspectRatiosStayWithinTargetGrid() {
        int maxBlockWidth = 40;
        int maxBlockHeight = 22;

        ImageToAscii.Result wide = ImageToAscii.render(
                solid(4000, 8, 0xFF00FF00),
                settings(maxBlockWidth, maxBlockHeight, MediaRenderSettings.RenderStyle.BLOCK, 64));
        assertNotNull(wide);
        assertTrue(wide.width() <= maxBlockWidth);
        assertTrue(wide.height() >= 1 && wide.height() <= maxBlockHeight);

        ImageToAscii.Result tall = ImageToAscii.render(
                solid(8, 4000, 0xFF00FF00),
                settings(maxBlockWidth, maxBlockHeight, MediaRenderSettings.RenderStyle.BLOCK, 64));
        assertNotNull(tall);
        assertTrue(tall.width() >= 1 && tall.width() <= maxBlockWidth);
        assertTrue(tall.height() <= maxBlockHeight);
    }

    @Test
    void brailleUsesSubpixelGridWithoutExceedingLimits() {
        ImageToAscii.Result result = ImageToAscii.render(
                solid(128, 128, 0xFFFFFFFF),
                settings(40, 22, MediaRenderSettings.RenderStyle.BRAILLE, 64));

        assertNotNull(result);
        assertTrue(result.width() <= 80, "width was " + result.width());
        assertTrue(result.height() <= 88, "height was " + result.height());
        // Braille reserves 2x4 subpixels, so a square image should produce a
        // noticeably taller grid than the block renderer.
        assertTrue(result.height() > 22, "height was " + result.height());
    }
}
