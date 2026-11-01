package org.thesandbox.core.services.discord.media;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.sticker.Sticker;
import net.dv8tion.jda.api.entities.sticker.StickerItem;
import net.dv8tion.jda.api.utils.AttachmentProxy;
import net.dv8tion.jda.api.utils.ImageProxy;
import org.thesandbox.core.util.Utils;

import java.util.*;

/**
 * Turns a Discord message into a render plan without doing any network or
 * image work on the JDA event thread.
 */
public final class MediaCollector {

    private static final Set<String> UNSUPPORTED_EXTENSIONS = Set.of(
            "svg", "heic", "heif", "jxl", "json", "lottie"
    );

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            "png", "apng", "jpg", "jpeg", "jfif", "gif", "webp",
            "bmp", "tif", "tiff", "ico", "cur", "psd", "tga", "avif"
    );

    private static final Set<String> VIDEO_EXTENSIONS = Set.of(
            "mp4", "mov", "webm", "m4v"
    );

    private final MediaRenderSettings settings;

    public MediaCollector(MediaRenderSettings settings) {
        this.settings = settings == null ? MediaRenderSettings.disabled() : settings;
    }

    public MediaPlan collect(Message message, TextChannel channel, boolean staff) {
        if (message == null) return MediaPlan.empty("");

        String jumpUrl = safe(message.getJumpUrl());
        String authorId = message.getAuthor() == null ? "" : safe(message.getAuthor().getId());
        String channelId = channel == null
                ? (message.getChannel() == null ? "" : safe(message.getChannel().getId()))
                : safe(channel.getId());

        LinkedHashSet<String> mediaUrls = new LinkedHashSet<>();
        for (Message.Attachment attachment : message.getAttachments()) {
            mediaUrls.add(attachment.getUrl());
        }
        List<String> textUrls = Utils.extractAllUrls(message.getContentDisplay());
        mediaUrls.addAll(textUrls);
        addEmbedMediaPlaceholders(message, mediaUrls);

        List<MediaItem> images = new ArrayList<>();
        boolean allowed = settings.enabled() && (staff ? settings.staff() : settings.publicChat());
        boolean nsfwAllowed = settings.renderNsfw()
                || channel == null
                || !channel.isNSFW();

        if (allowed && nsfwAllowed) {
            collectAttachments(message, images);
            collectStickers(message, images);
            collectEmbeds(message, images);
        }

        int stickerCount = message.getStickers().size();
        int extraCount = images.isEmpty()
                ? 0
                : Math.max(0, countMedia(message, textUrls) - 1);

        String fallbackUrl = firstHttpUrl(mediaUrls);
        if (fallbackUrl.isBlank() && (stickerCount > 0 || !images.isEmpty())) {
            fallbackUrl = jumpUrl;
        }

        return new MediaPlan(images, fallbackUrl, jumpUrl, extraCount, authorId, channelId);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static void addEmbedMediaPlaceholders(Message message, LinkedHashSet<String> mediaUrls) {
        for (MessageEmbed embed : message.getEmbeds()) {
            MessageEmbed.VideoInfo video = embed.getVideoInfo();
            if (video != null && video.getUrl() != null) {
                mediaUrls.add(video.getUrl());
                continue;
            }
            if (embed.getImage() != null && embed.getImage().getUrl() != null) {
                mediaUrls.add(embed.getImage().getUrl());
            } else if (embed.getThumbnail() != null && embed.getThumbnail().getUrl() != null) {
                mediaUrls.add(embed.getThumbnail().getUrl());
            } else if (embed.getUrl() != null) {
                mediaUrls.add(embed.getUrl());
            }
        }
    }

    private void collectAttachments(Message message, List<MediaItem> images) {
        for (Message.Attachment attachment : message.getAttachments()) {
            if (attachment.isSpoiler() && !settings.renderSpoilers()) continue;
            if (attachment.isVideo()) continue;
            if (attachment.getSize() > settings.maxBytes()) continue;

            String extension = lower(attachment.getFileExtension());
            String contentType = lower(attachment.getContentType());
            boolean looksLikeImage = attachment.isImage() || IMAGE_EXTENSIONS.contains(extension);
            if (!looksLikeImage || isUnsupported(extension, contentType)) continue;

            int width = attachment.getWidth();
            int height = attachment.getHeight();
            AttachmentProxy proxy = attachment.getProxy();

            images.add(new MediaItem(
                    "attachment:" + attachment.getId(),
                    extension.isBlank() ? contentType : extension,
                    isAnimated(extension, contentType),
                    width,
                    height,
                    maxDimension -> downloadAttachment(proxy, width, height, maxDimension)
            ));
        }
    }

    private void collectStickers(Message message, List<MediaItem> images) {
        for (StickerItem sticker : message.getStickers()) {
            Sticker.StickerFormat format = sticker.getFormatType();
            if (format != Sticker.StickerFormat.PNG
                    && format != Sticker.StickerFormat.APNG
                    && format != Sticker.StickerFormat.GIF) {
                continue;
            }

            ImageProxy icon = sticker.getIcon();
            boolean animated = format == Sticker.StickerFormat.APNG
                    || format == Sticker.StickerFormat.GIF;

            images.add(new MediaItem(
                    "sticker:" + sticker.getId(),
                    format.name().toLowerCase(Locale.ROOT),
                    animated,
                    0,
                    0,
                    icon::download
            ));
        }
    }

    private void collectEmbeds(Message message, List<MediaItem> images) {
        for (MessageEmbed embed : message.getEmbeds()) {
            // Video embeds deliberately stay as clickable [Media].
            if (embed.getVideoInfo() != null) continue;

            boolean thumbnail = false;
            MessageEmbed.ImageInfo image = embed.getImage();
            MessageEmbed.Thumbnail thumb = null;
            if (image == null && settings.renderThumbnails()) {
                thumb = embed.getThumbnail();
                thumbnail = thumb != null;
            }
            if (image == null && thumb == null) continue;

            String url = image != null
                    ? firstNonBlank(image.getProxyUrl(), image.getUrl())
                    : firstNonBlank(thumb.getProxyUrl(), thumb.getUrl());
            if (url.isBlank()) continue;

            String extension = extensionOf(url);
            if (isUnsupported(extension, "")) continue;

            int width = image != null ? image.getWidth() : thumb.getWidth();
            int height = image != null ? image.getHeight() : thumb.getHeight();
            if (thumbnail && width > 0 && height > 0 && (width < 64 || height < 64)) {
                continue;
            }

            AttachmentProxy proxy = image != null ? image.getProxy() : thumb.getProxy();
            images.add(new MediaItem(
                    "embed:" + url,
                    extension,
                    isAnimated(extension, ""),
                    width,
                    height,
                    maxDimension -> downloadAttachment(proxy, width, height, maxDimension)
            ));
        }
    }

    private static int countMedia(Message message, List<String> textUrls) {
        int count = 0;

        for (Message.Attachment attachment : message.getAttachments()) {
            if (isMediaAttachment(attachment)) count++;
        }

        count += message.getStickers().size();

        for (MessageEmbed embed : message.getEmbeds()) {
            if (embed.getVideoInfo() != null
                    || embed.getImage() != null
                    || embed.getThumbnail() != null) {
                count++;
            }
        }

        for (String url : textUrls) {
            if (Utils.isMediaUrl(url)) count++;
        }
        return count;
    }

    private static String firstHttpUrl(Set<String> urls) {
        for (String url : urls) {
            if (url != null && url.toLowerCase(Locale.ROOT).startsWith("http")) {
                return url;
            }
        }
        return "";
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static boolean isUnsupported(String extension, String contentType) {
        if (UNSUPPORTED_EXTENSIONS.contains(extension)) return true;
        if (contentType == null || contentType.isBlank()) return false;
        return contentType.contains("svg")
                || contentType.contains("heic")
                || contentType.contains("heif")
                || contentType.contains("jxl")
                || contentType.contains("json")
                || contentType.contains("lottie");
    }

    private static boolean isAnimated(String extension, String contentType) {
        return "gif".equals(extension)
                || "apng".equals(extension)
                || "webp".equals(extension)
                || (contentType != null && contentType.contains("gif"));
    }

    private static java.util.concurrent.CompletableFuture<java.io.InputStream> downloadAttachment(
            AttachmentProxy proxy,
            int width,
            int height,
            int maxDimension
    ) {
        if (width <= 0 || height <= 0) {
            return proxy.download(maxDimension, maxDimension);
        }

        double scale = Math.min(
                (double) maxDimension / (double) width,
                (double) maxDimension / (double) height
        );
        int requestWidth = Math.max(1, (int) Math.round(width * scale));
        int requestHeight = Math.max(1, (int) Math.round(height * scale));
        return proxy.download(requestWidth, requestHeight);
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) return first;
        return second == null ? "" : second;
    }

    private static String extensionOf(String url) {
        if (url == null) return "";
        String clean = url;
        int query = clean.indexOf('?');
        if (query >= 0) clean = clean.substring(0, query);
        int slash = clean.lastIndexOf('/');
        int dot = clean.lastIndexOf('.');
        if (dot < 0 || dot < slash) return "";
        return lower(clean.substring(dot + 1));
    }

    private static boolean isMediaAttachment(Message.Attachment attachment) {
        if (attachment.isImage() || attachment.isVideo()) return true;

        String extension = lower(attachment.getFileExtension());
        if (IMAGE_EXTENSIONS.contains(extension) || VIDEO_EXTENSIONS.contains(extension)) {
            return true;
        }

        String contentType = lower(attachment.getContentType());
        return contentType.startsWith("image/") || contentType.startsWith("video/");
    }
}
