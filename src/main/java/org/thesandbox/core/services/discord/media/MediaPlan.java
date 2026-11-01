package org.thesandbox.core.services.discord.media;

import java.util.List;

/**
 * Media extracted from one Discord message.
 *
 * @param images      renderable images, in message order
 * @param fallbackUrl URL used by the legacy {@code [Media]} tag
 * @param jumpUrl     stable Discord message URL for click-through
 * @param extraCount  additional media not represented by the rendered image
 * @param authorId    Discord author id, used for rate limiting
 * @param channelId   Discord channel id, used for logging/ordering
 */
public record MediaPlan(
        List<MediaItem> images,
        String fallbackUrl,
        String jumpUrl,
        int extraCount,
        String authorId,
        String channelId
) {

    public MediaPlan {
        images = images == null ? List.of() : List.copyOf(images);
        fallbackUrl = fallbackUrl == null ? "" : fallbackUrl;
        jumpUrl = jumpUrl == null ? "" : jumpUrl;
        authorId = authorId == null ? "" : authorId;
        channelId = channelId == null ? "" : channelId;
        extraCount = Math.max(0, extraCount);
    }

    public static MediaPlan empty(String jumpUrl) {
        return new MediaPlan(List.of(), "", jumpUrl, 0, "", "");
    }

    public boolean hasRenderableImage() {
        return !images.isEmpty();
    }

    public boolean hasMedia() {
        return !images.isEmpty() || !fallbackUrl.isBlank() || extraCount > 0;
    }
}
