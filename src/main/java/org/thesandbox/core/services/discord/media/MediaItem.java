package org.thesandbox.core.services.discord.media;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

/**
 * One image that can be sent to the ASCII renderer.
 *
 * <p>The downloader hides whether the bytes come from an attachment proxy,
 * an embed proxy or a sticker icon. The renderer only sees a stream.</p>
 */
public record MediaItem(
        String cacheKey,
        String formatHint,
        boolean animated,
        int width,
        int height,
        Downloader downloader
) {

    @FunctionalInterface
    public interface Downloader {
        CompletableFuture<InputStream> download(int maxDimension);
    }
}
