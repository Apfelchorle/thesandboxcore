package org.thesandbox.core.services.discord.media;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * Immutable snapshot of the Discord image-renderer configuration.
 *
 * <p>All keys are read with defaults so the feature works even when the
 * server has no existing {@code config.yml} entry for it.</p>
 */
public record MediaRenderSettings(
        boolean enabled,
        boolean staff,
        boolean publicChat,
        boolean renderNsfw,
        boolean renderSpoilers,
        boolean renderThumbnails,
        int width,
        int height,
        double charAspect,
        ColorMode colorMode,
        RenderStyle renderStyle,
        int paletteSize,
        int alphaThreshold,
        int backgroundColor,
        long timeoutMs,
        int cacheSize,
        int maxPerMinute,
        int maxPerAuthorPerMinute,
        int maxBytes,
        int maxPixels,
        int fetchSize,
        boolean debug
) {

    public static MediaRenderSettings from(Plugin plugin) {
        FileConfiguration config = plugin.getConfig();

        String palette = config.getString("discord.image-render.palette", "hex");
        ColorMode mode = palette != null && palette.equalsIgnoreCase("legacy")
                ? ColorMode.LEGACY
                : ColorMode.HEX;

        String style = config.getString("discord.image-render.style", "block");
        RenderStyle renderStyle;
        if (style != null && style.equalsIgnoreCase("emoji")) {
            renderStyle = RenderStyle.EMOJI;
        } else {
            renderStyle = RenderStyle.BLOCK;
        }

        return new MediaRenderSettings(
                config.getBoolean("discord.image-render.enabled", true),
                config.getBoolean("discord.image-render.staff", true),
                config.getBoolean("discord.image-render.public-chat", true),
                config.getBoolean("discord.image-render.nsfw", false),
                config.getBoolean("discord.image-render.spoilers", false),
                config.getBoolean("discord.image-render.thumbnails", true),
                clamp(config.getInt("discord.image-render.width", 34), 8, 80),
                clamp(config.getInt("discord.image-render.height", 16), 4, 40),
                clamp(config.getDouble("discord.image-render.char-aspect", 0.9), 0.4, 2.0),
                mode,
                renderStyle,
                clamp(config.getInt("discord.image-render.colors", 64), 2, 256),
                clamp(config.getInt("discord.image-render.alpha-threshold", 32), 0, 255),
                parseColor(config.getString("discord.image-render.background", "#1F1F1F"), 0x1F1F1F),
                clamp(config.getLong("discord.image-render.timeout-ms", 2000L), 500L, 15_000L),
                clamp(config.getInt("discord.image-render.cache-size", 128), 0, 1024),
                clamp(config.getInt("discord.image-render.max-per-minute", 12), 1, 240),
                clamp(config.getInt("discord.image-render.max-per-author-per-minute", 3), 1, 120),
                clamp(config.getInt("discord.image-render.max-bytes", 8 * 1024 * 1024), 1024 * 1024, 32 * 1024 * 1024),
                clamp(config.getInt("discord.image-render.max-pixels", 20_000_000), 1_000_000, 50_000_000),
                clamp(config.getInt("discord.image-render.fetch-size", 256), 64, 1024),
                config.getBoolean("discord.image-render.debug", false)
        );
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int parseColor(String raw, int fallback) {
        if (raw == null) return fallback;
        String value = raw.trim();
        if (value.startsWith("#")) value = value.substring(1);
        if (value.length() != 6) return fallback;
        try {
            return Integer.parseInt(value, 16) & 0xFFFFFF;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    public static MediaRenderSettings disabled() {
        return new MediaRenderSettings(
                false, false, false, false, false, false,
                34, 16, 0.9, ColorMode.HEX, RenderStyle.BLOCK, 64, 32, 0x1F1F1F,
                2000L, 128, 12, 3,
                8 * 1024 * 1024, 20_000_000, 256, false
        );
    }

    public String cacheSignature() {
        return width + "x" + height + ":" + charAspect + ":" + colorMode + ":" + renderStyle
                + ":" + paletteSize + ":" + alphaThreshold
                + ":" + Integer.toHexString(backgroundColor) + ":" + fetchSize;
    }

    public enum ColorMode {
        HEX,
        LEGACY
    }

    /**
     * Controls how each pixel is rendered in chat.
     */
    public enum RenderStyle {
        /**
         * Uses a block character (█) with color — the default.
         */
        BLOCK,
        /**
         * Uses colored emoji circles mapped to luminance bands.
         */
        EMOJI
    }
}
