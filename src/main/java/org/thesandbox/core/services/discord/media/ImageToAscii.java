package org.thesandbox.core.services.discord.media;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * Downsamples an image to a small grid of opaque ARGB values.
 *
 * <p>A value of {@code 0} in the result means "transparent". Opaque colors
 * always have a non-zero alpha channel. Hex mode also applies a small
 * median-cut palette so chat components stay compact.</p>
 */
public final class ImageToAscii {

    private ImageToAscii() {
    }

    public static Result render(BufferedImage source, MediaRenderSettings settings) {
        if (source == null || settings == null) return null;

        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        if (sourceWidth <= 0 || sourceHeight <= 0) return null;

        double ratio = ((double) sourceHeight / (double) sourceWidth) * settings.charAspect();
        int targetWidth = settings.width();
        int targetHeight = Math.max(1, (int) Math.round(targetWidth * ratio));
        if (targetHeight > settings.height()) {
            targetHeight = settings.height();
            targetWidth = Math.max(1, (int) Math.round(targetHeight / ratio));
        }
        targetWidth = Math.max(1, Math.min(settings.width(), targetWidth));
        targetHeight = Math.max(1, Math.min(settings.height(), targetHeight));

        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );
            graphics.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );
            graphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );
            graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }

        int background = settings.backgroundColor();
        int bgRed = (background >> 16) & 0xFF;
        int bgGreen = (background >> 8) & 0xFF;
        int bgBlue = background & 0xFF;

        int[][] colors = new int[targetHeight][targetWidth];
        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                int argb = scaled.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xFF;
                if (alpha < settings.alphaThreshold()) {
                    colors[y][x] = 0;
                    continue;
                }

                int red = (argb >> 16) & 0xFF;
                int green = (argb >> 8) & 0xFF;
                int blue = argb & 0xFF;
                if (alpha < 255) {
                    red = (red * alpha + bgRed * (255 - alpha) + 127) / 255;
                    green = (green * alpha + bgGreen * (255 - alpha) + 127) / 255;
                    blue = (blue * alpha + bgBlue * (255 - alpha) + 127) / 255;
                }
                colors[y][x] = 0xFF000000 | (red << 16) | (green << 8) | blue;
            }
        }

        int minX = targetWidth;
        int minY = targetHeight;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                if (colors[y][x] == 0) continue;
                if (x < minX) minX = x;
                if (x > maxX) maxX = x;
                if (y < minY) minY = y;
                if (y > maxY) maxY = y;
            }
        }
        if (maxX < minX || maxY < minY) return null;

        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        int[][] trimmed = new int[height][width];
        for (int y = 0; y < height; y++) {
            System.arraycopy(colors[minY + y], minX, trimmed[y], 0, width);
        }

        if (settings.colorMode() == MediaRenderSettings.ColorMode.HEX && settings.paletteSize() > 0) {
            trimmed = quantize(trimmed, settings.paletteSize());
        }

        return new Result(width, height, trimmed);
    }

    private static int[][] quantize(int[][] colors, int maxColors) {
        List<Integer> pixels = new ArrayList<>();
        Set<Integer> unique = new HashSet<>();

        for (int[] row : colors) {
            for (int color : row) {
                if (color == 0) continue;
                pixels.add(color);
                unique.add(color);
            }
        }

        if (unique.size() <= maxColors || pixels.isEmpty()) {
            return colors;
        }

        List<Box> boxes = new ArrayList<>();
        boxes.add(new Box(pixels));

        while (boxes.size() < maxColors) {
            Box best = null;
            int bestRange = 0;
            for (Box box : boxes) {
                if (box.colors.size() < 2) continue;
                int range = box.range();
                if (range > bestRange) {
                    bestRange = range;
                    best = box;
                }
            }
            if (best == null) break;

            boxes.remove(best);
            Box[] split = best.split();
            if (split == null) break;
            boxes.add(split[0]);
            boxes.add(split[1]);
        }

        int[] palette = new int[boxes.size()];
        for (int i = 0; i < boxes.size(); i++) {
            palette[i] = boxes.get(i).average();
        }

        int[][] quantized = new int[colors.length][];
        for (int y = 0; y < colors.length; y++) {
            quantized[y] = new int[colors[y].length];
            for (int x = 0; x < colors[y].length; x++) {
                int color = colors[y][x];
                quantized[y][x] = color == 0 ? 0 : nearest(color, palette);
            }
        }
        return quantized;
    }

    private static int nearest(int color, int[] palette) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;

        int best = palette[0];
        long bestDistance = Long.MAX_VALUE;
        for (int candidate : palette) {
            int dr = red - ((candidate >> 16) & 0xFF);
            int dg = green - ((candidate >> 8) & 0xFF);
            int db = blue - (candidate & 0xFF);
            long distance = (long) dr * dr * 3L + (long) dg * dg * 4L + (long) db * db * 2L;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    public record Result(int width, int height, int[][] colors) {
    }

    private static final class Box {
        private final List<Integer> colors;
        private final int minRed;
        private final int maxRed;
        private final int minGreen;
        private final int maxGreen;
        private final int minBlue;
        private final int maxBlue;

        private Box(List<Integer> colors) {
            this.colors = colors;

            int redMin = 255;
            int redMax = 0;
            int greenMin = 255;
            int greenMax = 0;
            int blueMin = 255;
            int blueMax = 0;

            for (int color : colors) {
                int red = (color >> 16) & 0xFF;
                int green = (color >> 8) & 0xFF;
                int blue = color & 0xFF;
                if (red < redMin) redMin = red;
                if (red > redMax) redMax = red;
                if (green < greenMin) greenMin = green;
                if (green > greenMax) greenMax = green;
                if (blue < blueMin) blueMin = blue;
                if (blue > blueMax) blueMax = blue;
            }

            this.minRed = redMin;
            this.maxRed = redMax;
            this.minGreen = greenMin;
            this.maxGreen = greenMax;
            this.minBlue = blueMin;
            this.maxBlue = blueMax;
        }

        private int range() {
            return Math.max(maxRed - minRed, Math.max(maxGreen - minGreen, maxBlue - minBlue));
        }

        private Box[] split() {
            if (colors.size() < 2) return null;

            int redRange = maxRed - minRed;
            int greenRange = maxGreen - minGreen;
            int blueRange = maxBlue - minBlue;

            int shift;
            if (redRange >= greenRange && redRange >= blueRange) {
                shift = 16;
            } else if (greenRange >= blueRange) {
                shift = 8;
            } else {
                shift = 0;
            }

            colors.sort(Comparator.comparingInt(color -> (color >> shift) & 0xFF));
            int middle = colors.size() / 2;
            if (middle <= 0 || middle >= colors.size()) return null;

            return new Box[]{
                    new Box(new ArrayList<>(colors.subList(0, middle))),
                    new Box(new ArrayList<>(colors.subList(middle, colors.size())))
            };
        }

        private int average() {
            long red = 0;
            long green = 0;
            long blue = 0;
            for (int color : colors) {
                red += (color >> 16) & 0xFF;
                green += (color >> 8) & 0xFF;
                blue += color & 0xFF;
            }
            int count = colors.size();
            return 0xFF000000
                    | ((int) (red / count) << 16)
                    | ((int) (green / count) << 8)
                    | (int) (blue / count);
        }
    }
}
