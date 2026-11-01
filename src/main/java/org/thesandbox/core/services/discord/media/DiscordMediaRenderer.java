package org.thesandbox.core.services.discord.media;

import net.kyori.adventure.text.Component;
import org.bukkit.plugin.Plugin;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Downloads and renders Discord images off the main thread.
 *
 * <p>Failures and timeouts deliberately produce an empty result so the caller
 * can fall back to the normal clickable {@code [Media]} tag.</p>
 */
public final class DiscordMediaRenderer implements AutoCloseable {

    private static final int WORKER_COUNT = 2;
    private static final int QUEUE_CAPACITY = 32;

    private final Plugin plugin;
    private final MediaRenderSettings settings;
    private final ThreadPoolExecutor executor;
    private final Map<String, Component> cache;
    private final Map<String, CompletableFuture<Optional<Component>>> inFlight = new ConcurrentHashMap<>();
    private final RateLimiter rateLimiter;
    private final AtomicBoolean closed = new AtomicBoolean();

    public DiscordMediaRenderer(Plugin plugin, MediaRenderSettings settings) {
        this.plugin = plugin;
        this.settings = settings == null ? MediaRenderSettings.disabled() : settings;
        MediaImageIo.register();

        this.executor = new ThreadPoolExecutor(
                WORKER_COUNT,
                WORKER_COUNT,
                30L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                runnable -> {
                    Thread thread = new Thread(runnable, "Sandbox-Media-Render");
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.AbortPolicy()
        );

        int cacheSize = Math.max(1, this.settings.cacheSize());
        final int cacheCapacity = this.settings.cacheSize();
        this.cache = Collections.synchronizedMap(
                new LinkedHashMap<>(cacheSize + 1, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, Component> eldest) {
                        return cacheCapacity <= 0 || size() > cacheCapacity;
                    }
                }
        );
        this.rateLimiter = new RateLimiter(
                this.settings.maxPerMinute(),
                this.settings.maxPerAuthorPerMinute()
        );
    }

    public CompletableFuture<Optional<Component>> render(MediaPlan plan) {
        if (closed.get() || settings == null || !settings.enabled()
                || plan == null || !plan.hasRenderableImage()) {
            return completedEmpty();
        }

        MediaItem item = plan.images().get(0);
        String key = item.cacheKey() + "|" + settings.cacheSignature();
        Component cached = cache.get(key);
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }

        CompletableFuture<Optional<Component>> existing = inFlight.get(key);
        if (existing != null) {
            return existing;
        }

        if (!rateLimiter.tryAcquire(plan.authorId())) {
            debug("rate limited image from " + plan.authorId());
            return completedEmpty();
        }

        final CompletableFuture<Optional<Component>> future;
        try {
            future = CompletableFuture
                    .supplyAsync(() -> renderItem(item, plan.jumpUrl()), executor)
                    .orTimeout(settings.timeoutMs(), TimeUnit.MILLISECONDS)
                    .exceptionally(throwable -> {
                        debug("render failed for " + item.formatHint() + ": " + throwable.getMessage(), throwable);
                        return Optional.empty();
                    })
                    .thenApply(result -> {
                        result.ifPresent(component -> cache.put(key, component));
                        return result;
                    });
        } catch (RejectedExecutionException rejected) {
            debug("image queue is full");
            return completedEmpty();
        }

        CompletableFuture<Optional<Component>> previous = inFlight.putIfAbsent(key, future);
        if (previous != null) {
            return previous;
        }
        future.whenComplete((result, throwable) -> inFlight.remove(key, future));
        return future;
    }

    private static CompletableFuture<Optional<Component>> completedEmpty() {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    private void debug(String message) {
        if (settings.debug()) {
            plugin.getLogger().info("[Discord/Image] " + message);
        } else {
            plugin.getLogger().log(Level.FINE, "[Discord/Image] " + message);
        }
    }

    private Optional<Component> renderItem(MediaItem item, String jumpUrl) {
        if (item == null || item.downloader() == null) return Optional.empty();

        try {
            CompletableFuture<InputStream> download = item.downloader().download(settings.fetchSize());
            if (download == null) return Optional.empty();

            try (InputStream raw = download.get(settings.timeoutMs(), TimeUnit.MILLISECONDS)) {
                if (raw == null) return Optional.empty();

                byte[] bytes = readBounded(raw, settings.maxBytes());
                BufferedImage image = decodeFirstImage(bytes);
                if (image == null) {
                    debug("no ImageIO reader handled " + item.formatHint());
                    return Optional.empty();
                }

                ImageToAscii.Result result = ImageToAscii.render(image, settings);
                if (result == null || result.width() <= 0 || result.height() <= 0) {
                    return Optional.empty();
                }

                return Optional.of(MinecraftAsciiComponent.build(result, settings));
            }
        } catch (Throwable throwable) {
            debug("could not render " + item.formatHint() + ": " + throwable.getMessage(), throwable);
            return Optional.empty();
        }
    }

    private void debug(String message, Throwable throwable) {
        if (settings.debug()) {
            plugin.getLogger().log(Level.WARNING, "[Discord/Image] " + message, throwable);
        } else {
            plugin.getLogger().log(Level.FINE, "[Discord/Image] " + message, throwable);
        }
    }

    private static byte[] readBounded(InputStream input, int maxBytes) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(8192, maxBytes));
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > maxBytes) {
                throw new IOException("image exceeds byte limit");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private BufferedImage decodeFirstImage(byte[] bytes) {
        List<ImageReader> readers = new ArrayList<>();
        try (ImageInputStream probe = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> iterator = ImageIO.getImageReaders(probe);
            while (iterator.hasNext()) {
                readers.add(iterator.next());
            }
        } catch (Throwable throwable) {
            debug("could not inspect image bytes", throwable);
            return null;
        }

        for (ImageReader reader : readers) {
            try (ImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
                reader.setInput(input, true, true);

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0) continue;
                if ((long) width * (long) height > settings.maxPixels()) {
                    debug("image exceeds pixel limit: " + width + "x" + height);
                    continue;
                }

                BufferedImage first = reader.read(0);
                if (first == null) continue;

                if (hasVisiblePixels(first)) {
                    return first;
                }

                // Animated GIFs/APNGs/WebPs can start with a fully transparent
                // frame. Look a little further before giving up.
                int frameCount = 1;
                try {
                    frameCount = Math.min(reader.getNumImages(true), 24);
                } catch (Throwable ignored) {
                }

                for (int frame = 1; frame < frameCount; frame++) {
                    try {
                        BufferedImage next = reader.read(frame);
                        if (next != null && hasVisiblePixels(next)) {
                            return next;
                        }
                    } catch (Throwable ignored) {
                    }
                }
                return first;
            } catch (Throwable throwable) {
                debug("reader failed: " + throwable.getMessage(), throwable);
            } finally {
                reader.dispose();
            }
        }
        return null;
    }

    private boolean hasVisiblePixels(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= 0 || height <= 0) return false;

        int step = Math.max(1, Math.min(width, height) / 512);
        int threshold = settings.alphaThreshold();
        for (int y = 0; y < height; y += step) {
            for (int x = 0; x < width; x += step) {
                if (((image.getRGB(x, y) >>> 24) & 0xFF) >= threshold) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        executor.shutdownNow();
        inFlight.clear();
        cache.clear();
    }

    private static final class RateLimiter {
        private final int globalLimit;
        private final int authorLimit;
        private final Window global = new Window();
        private final Map<String, Window> authors = new ConcurrentHashMap<>();

        private RateLimiter(int globalLimit, int authorLimit) {
            this.globalLimit = globalLimit;
            this.authorLimit = authorLimit;
        }

        private synchronized boolean tryAcquire(String authorId) {
            long now = System.currentTimeMillis();

            Window authorWindow = null;
            if (authorId != null && !authorId.isBlank()) {
                authorWindow = authors.computeIfAbsent(authorId, ignored -> new Window());
                if (!authorWindow.canAllow(now, authorLimit)) return false;
            }

            if (!global.canAllow(now, globalLimit)) return false;

            global.record(now, globalLimit);
            if (authorWindow != null) {
                authorWindow.record(now, authorLimit);
            }

            if (authors.size() > 512) {
                authors.entrySet().removeIf(entry -> now - entry.getValue().start > 120_000L);
            }
            return true;
        }
    }

    private static final class Window {
        private long start;
        private int count;

        private boolean canAllow(long now, int limit) {
            if (limit <= 0) return true;
            return now - start >= 60_000L || count < limit;
        }

        private void record(long now, int limit) {
            if (limit <= 0) return;
            if (now - start >= 60_000L) {
                start = now;
                count = 0;
            }
            count++;
        }
    }
}
