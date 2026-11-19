package org.thesandbox.core.commands.misc;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.services.discord.media.AnimatedMapService;
import org.thesandbox.core.util.net.SafeHttpUrlValidator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Allows players to create animated Minecraft maps from GIF URLs.
 * <p>
 * Usage: /gifmap &lt;url&gt;
 *        /gifmap example
 * The command downloads the GIF, decodes all frames, and gives the player
 * a filled map item that animates when held.
 */
public class GifMapCommand extends BaseCommand {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://\\S+",
            Pattern.CASE_INSENSITIVE
    );

    private static final String EXAMPLE_GIF_URL = "https://media.giphy.com/media/3o7aD2nsqoQv8arVU8/giphy.gif";

    private static final int MAX_BYTES = 8 * 1024 * 1024; // 8MB limit
    private static final int CONNECT_TIMEOUT = 15000;     // 15 seconds
    private static final int READ_TIMEOUT = 45000;        // 45 seconds
    private static final int MAX_REDIRECTS = 3;
    private static final long REQUEST_COOLDOWN_MS = 30_000L;
    private static final int MAX_CONCURRENT_DOWNLOADS = 3;
    private static final int DOWNLOAD_QUEUE_CAPACITY = 8;

    /**
     * Downloads are bounded and daemon-backed. They must never be raw
     * {@code new Thread(...)} calls: a player could otherwise exhaust threads.
     */
    private static final ThreadPoolExecutor DOWNLOAD_EXECUTOR = new ThreadPoolExecutor(
            MAX_CONCURRENT_DOWNLOADS,
            MAX_CONCURRENT_DOWNLOADS,
            30L,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(DOWNLOAD_QUEUE_CAPACITY),
            runnable -> {
                Thread thread = new Thread(runnable, "Sandbox-GifMap-Download");
                thread.setDaemon(true);
                return thread;
            },
            new ThreadPoolExecutor.AbortPolicy()
    );

    static {
        DOWNLOAD_EXECUTOR.allowCoreThreadTimeOut(true);
    }

    private final TheSandboxCore plugin;
    private final Map<UUID, Long> lastRequests = new ConcurrentHashMap<>();

    public GifMapCommand(TheSandboxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("[GifMap] Only players can use this command.", NamedTextColor.RED));
            logInfo("Console attempted to use /gifmap — rejected");
            return true;
        }

        if (args.length < 1 || args[0].isBlank()) {
            sendUsage(player);
            return true;
        }

        // Handle /gifmap example
        if (args[0].equalsIgnoreCase("example")) {
            if (!tryAcquire(player)) return true;
            logInfo(player.getName() + " ran /gifmap example");
            processGifUrl(player, EXAMPLE_GIF_URL, "example GIF");
            return true;
        }

        // Extract URL from arguments
        String input = String.join(" ", args);
        logFine("Parsing input for URL: " + input);

        Matcher matcher = URL_PATTERN.matcher(input);
        if (!matcher.find()) {
            logWarning("No URL found in input: " + input);
            player.sendMessage(Component.text(
                    "[GifMap] Could not find a valid URL in your input.", NamedTextColor.RED));
            player.sendMessage(Component.text(
                    "  Usage: /gifmap <gif-url>  (e.g. /gifmap https://example.com/animation.gif)", NamedTextColor.GRAY));
            return true;
        }

        String url = matcher.group(0);
        logInfo(player.getName() + " requested GIF map from: " + url);

        // Validate the URL looks like it could be a GIF
        if (!isLikelyGifUrl(url)) {
            logWarning("URL does not appear to be a GIF: " + url);
            player.sendMessage(Component.text(
                    "[GifMap] The URL does not appear to point to a GIF file.", NamedTextColor.RED));
            player.sendMessage(Component.text(
                    "  Make sure the URL ends with .gif or contains 'gif' in the path.", NamedTextColor.GRAY));
            player.sendMessage(Component.text(
                    "  Tip: Use /gifmap example to test with a sample GIF.", NamedTextColor.GRAY));
            return true;
        }

        if (!tryAcquire(player)) return true;
        processGifUrl(player, url, "GIF");
        return true;
    }

    private boolean tryAcquire(Player player) {
        long now = System.currentTimeMillis();
        Long previous = lastRequests.put(player.getUniqueId(), now);
        if (previous != null && now - previous < REQUEST_COOLDOWN_MS) {
            long waitSeconds = Math.max(1L, (REQUEST_COOLDOWN_MS - (now - previous) + 999L) / 1000L);
            player.sendMessage(Component.text(
                    "[GifMap] Please wait " + waitSeconds + "s before requesting another GIF.",
                    NamedTextColor.RED));
            return false;
        }

        if (lastRequests.size() > 512) {
            lastRequests.entrySet().removeIf(entry -> now - entry.getValue() > 10 * 60_000L);
        }
        return true;
    }

    private void logInfo(String message) {
        plugin.getLogger().info("[GifMap] " + message);
    }

    private void sendUsage(Player player) {
        player.sendMessage(Component.text(
                "[GifMap] Usage: /gifmap <gif-url>", NamedTextColor.YELLOW));
        player.sendMessage(Component.text(
                "  Creates an animated map from a GIF URL.", NamedTextColor.GRAY));
        player.sendMessage(Component.text(
                "  Use /gifmap example to test with a sample GIF.", NamedTextColor.GRAY));
        player.sendMessage(Component.text(
                "  Supported: Giphy, Tenor, Discord attachments, any direct .gif link.", NamedTextColor.GRAY));
    }

    /**
     * Central processing: validates service availability, starts download, handles the result.
     */
    private void processGifUrl(Player player, String url, String description) {
        // Get the animated map service
        AnimatedMapService mapService = getAnimatedMapService();
        if (mapService == null) {
            plugin.getLogger().severe("[GifMap] AnimatedMapService is not available - Discord bridge may not be initialized");
            player.sendMessage(Component.text(
                    "[GifMap] Animated map service is not available. The plugin may still be loading.", NamedTextColor.RED));
            return;
        }

        player.sendMessage(Component.text(
                "[GifMap] Downloading " + description + "... this may take a moment.", NamedTextColor.YELLOW));
        logInfo("Starting download for player " + player.getName() + ": " + url);

        // Download the GIF and create the map asynchronously
        downloadAndCreateMap(player, url, mapService);
    }

    private void logFine(String message) {
        plugin.getLogger().log(Level.FINE, "[GifMap] " + message);
    }

    private void logWarning(String message) {
        plugin.getLogger().warning("[GifMap] " + message);
    }

    /**
     * Checks if a URL looks like it might point to a GIF file.
     * Accepts URLs containing "gif" in the path or query, or ending with .gif.
     */
    private boolean isLikelyGifUrl(String url) {
        String lower = url.toLowerCase();
        // Check path for .gif extension or /gif/ segment
        String path = lower.split("\\?")[0]; // strip query params
        return path.endsWith(".gif") || path.contains("/gif") || path.contains("giphy") || path.contains("tenor");
    }

    private AnimatedMapService getAnimatedMapService() {
        if (plugin.getDiscord() != null) {
            return plugin.getDiscord().getAnimatedMapService();
        }
        return null;
    }

    // ==================== Logging helpers ====================

    private void downloadAndCreateMap(Player player, String url, AnimatedMapService mapService) {
        String playerName = player.getName();
        try {
            DOWNLOAD_EXECUTOR.execute(() ->
                    doDownloadAndCreate(player, playerName, url, mapService));
        } catch (RejectedExecutionException rejected) {
            logWarning("Download queue is full; rejecting request from " + playerName);
            player.sendMessage(Component.text(
                    "[GifMap] Too many downloads are already running. Please try again shortly.",
                    NamedTextColor.RED));
        }
    }

    private void doDownloadAndCreate(
            Player player,
            String playerName,
            String url,
            AnimatedMapService mapService
    ) {
        try {
            logFine("Opening connection to: " + url);
            long startTime = System.currentTimeMillis();

            byte[] gifBytes = downloadUrl(url);
            long downloadTime = System.currentTimeMillis() - startTime;

            if (gifBytes == null || gifBytes.length == 0) {
                logWarning("Download returned null/empty for: " + url);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.sendMessage(Component.text(
                                "[GifMap] Failed to download the GIF. The URL may be invalid, blocked, or the file may not exist.", NamedTextColor.RED));
                        player.sendMessage(Component.text(
                                "  Try /gifmap example to verify the feature works.", NamedTextColor.GRAY));
                    }
                });
                return;
            }

            // Verify GIF magic bytes
            if (!isGifFormat(gifBytes)) {
                logWarning("Downloaded content is not a valid GIF (bad magic bytes): " + url);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.sendMessage(Component.text(
                                "[GifMap] The downloaded file is not a valid GIF format.", NamedTextColor.RED));
                        player.sendMessage(Component.text(
                                "  The server expected GIF data but got something else.", NamedTextColor.GRAY));
                    }
                });
                return;
            }

            logInfo("Downloaded " + formatBytes(gifBytes.length) + " in " + downloadTime + "ms from: " + url);

            // Pass to AnimatedMapService for decoding and map creation
            logFine("Passing GIF bytes to AnimatedMapService for player " + playerName);
            mapService.createAndGiveMap(player, gifBytes)
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            logSevere("AnimatedMapService failed for " + playerName + ": " + throwable.getMessage(), throwable);
                        } else if (result == null) {
                            logWarning("AnimatedMapService returned null for " + playerName);
                        } else {
                            logInfo("Successfully created animated map for " + playerName);
                        }
                    });

        } catch (Exception exception) {
            logSevere("Unexpected error downloading GIF for " + playerName + ": " + exception.getMessage(), exception);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.sendMessage(Component.text(
                            "[GifMap] Unexpected error: " + exception.getMessage(), NamedTextColor.RED));
                }
            });
        }
    }

    private byte[] downloadUrl(String url) {
        final URI initial;
        try {
            initial = SafeHttpUrlValidator.validate(url);
        } catch (IllegalArgumentException blocked) {
            logWarning("Blocked download for " + url + ": " + blocked.getMessage());
            return null;
        }

        URI current = initial;
        try {
            for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
                logFine("Opening connection to: " + current);
                HttpURLConnection connection = (HttpURLConnection) current.toURL().openConnection();
                try {
                    connection.setConnectTimeout(CONNECT_TIMEOUT);
                    connection.setReadTimeout(READ_TIMEOUT);
                    connection.setInstanceFollowRedirects(false);
                    connection.setRequestProperty("User-Agent", "TheSandboxCore/1.0 Minecraft Plugin");
                    connection.setRequestProperty("Accept", "image/gif, image/*");

                    int response = connection.getResponseCode();
                    if (response >= 300 && response < 400) {
                        String location = connection.getHeaderField("Location");
                        if (location == null || location.isBlank()) {
                            logWarning("Redirect without a Location header from: " + current);
                            return null;
                        }

                        URI next = current.resolve(location.trim());
                        try {
                            current = SafeHttpUrlValidator.validate(next.toString());
                        } catch (IllegalArgumentException blocked) {
                            logWarning("Blocked redirect target " + next + ": " + blocked.getMessage());
                            return null;
                        }
                        logFine("Following redirect to: " + current);
                        continue;
                    }

                    if (response != HttpURLConnection.HTTP_OK) {
                        logWarning("HTTP " + response + " while downloading: " + current);
                        return null;
                    }

                    String contentType = connection.getContentType();
                    if (contentType != null) {
                        String lowerType = contentType.toLowerCase(Locale.ROOT);
                        logFine("Server response content-type: " + contentType);
                        if (lowerType.startsWith("text/")
                                || lowerType.contains("html")
                                || lowerType.contains("json")) {
                            logWarning("Refusing non-image content type: " + contentType);
                            return null;
                        }
                    }

                    long contentLength = connection.getContentLengthLong();
                    if (contentLength > MAX_BYTES) {
                        logWarning("File too large: " + formatBytes(contentLength)
                                + " (max " + formatBytes(MAX_BYTES) + ")");
                        return null;
                    }

                    logFine("Reading input stream...");
                    try (InputStream input = connection.getInputStream()) {
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream(Math.min(8192, MAX_BYTES));
                        byte[] chunk = new byte[8192];
                        int bytesRead;
                        int total = 0;

                        while ((bytesRead = input.read(chunk)) != -1) {
                            total += bytesRead;
                            if (total > MAX_BYTES) {
                                logWarning("Download exceeded size limit at "
                                        + formatBytes(total) + " (max " + formatBytes(MAX_BYTES) + ")");
                                return null;
                            }
                            buffer.write(chunk, 0, bytesRead);
                        }

                        logFine("Finished reading " + formatBytes(total) + " bytes");
                        return buffer.toByteArray();
                    }
                } finally {
                    connection.disconnect();
                }
            }

            logWarning("Too many redirects while downloading: " + url);
            return null;
        } catch (java.net.ConnectException e) {
            logWarning("Connection refused or timed out: " + current + " — " + e.getMessage());
            return null;
        } catch (java.net.SocketTimeoutException e) {
            logWarning("Download timed out: " + current + " — " + e.getMessage());
            return null;
        } catch (IOException e) {
            logWarning("I/O error downloading " + current + ": " + e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            logWarning("Blocked or invalid URL " + current + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Checks if the byte array starts with GIF87a or GIF89a magic bytes.
     */
    private boolean isGifFormat(byte[] data) {
        if (data == null || data.length < 6) return false;
        return (data[0] == 'G' && data[1] == 'I' && data[2] == 'F' && data[3] == '8'
                && (data[4] == '7' || data[4] == '9') && data[5] == 'a');
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024.0) + " KB";
        return (bytes / (1024.0 * 1024.0)) + " MB";
    }

    private void logSevere(String message, Throwable throwable) {
        plugin.getLogger().log(Level.SEVERE, "[GifMap] " + message, throwable);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = List.of("example");
            if (args[0].isEmpty()) return suggestions;
            String lower = args[0].toLowerCase();
            return suggestions.stream().filter(s -> s.startsWith(lower)).toList();
        }
        return List.of();
    }
}
