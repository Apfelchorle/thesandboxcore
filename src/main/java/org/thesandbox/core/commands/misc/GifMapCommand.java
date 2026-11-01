package org.thesandbox.core.commands.misc;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.services.discord.media.AnimatedMapService;

import java.util.List;
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

    private final TheSandboxCore plugin;

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
            logInfo(player.getName() + " ran /gifmap example");
            return processGifUrl(player, EXAMPLE_GIF_URL, "example GIF");
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

        return processGifUrl(player, url, "GIF");
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
    private boolean processGifUrl(Player player, String url, String description) {
        // Get the animated map service
        AnimatedMapService mapService = getAnimatedMapService();
        if (mapService == null) {
            logSevere("AnimatedMapService is not available — Discord bridge may not be initialized");
            player.sendMessage(Component.text(
                    "[GifMap] Animated map service is not available. The plugin may still be loading.", NamedTextColor.RED));
            return true;
        }

        player.sendMessage(Component.text(
                "[GifMap] Downloading " + description + "... this may take a moment.", NamedTextColor.YELLOW));
        logInfo("Starting download for player " + player.getName() + ": " + url);

        // Download the GIF and create the map asynchronously
        downloadAndCreateMap(player, url, mapService);

        return true;
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
            AnimatedMapService service = plugin.getDiscord().getAnimatedMapService();
            return service;
        }
        return null;
    }

    private void logSevere(String message) {
        plugin.getLogger().severe("[GifMap] " + message);
    }

    // ==================== Logging helpers ====================

    private void downloadAndCreateMap(Player player, String url, AnimatedMapService mapService) {
        new Thread(() -> {
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
                logFine("Passing GIF bytes to AnimatedMapService for player " + player.getName());
                mapService.createAndGiveMap(player, gifBytes)
                        .whenComplete((result, throwable) -> {
                            if (throwable != null) {
                                logSevere("AnimatedMapService failed for " + player.getName() + ": " + throwable.getMessage(), throwable);
                            } else if (result == null) {
                                logWarning("AnimatedMapService returned null for " + player.getName());
                            } else {
                                logInfo("Successfully created animated map for " + player.getName());
                            }
                        });

            } catch (Throwable t) {
                logSevere("Unexpected error downloading GIF for " + player.getName() + ": " + t.getMessage(), t);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.sendMessage(Component.text(
                                "[GifMap] Unexpected error: " + t.getMessage(), NamedTextColor.RED));
                    }
                });
            }
        }, "GifMap-Download-" + System.currentTimeMillis()).start();
    }

    private byte[] downloadUrl(String url) {
        try {
            java.net.URL uri = java.net.URI.create(url).toURL();
            logFine("Created URL object: protocol=" + uri.getProtocol() + ", host=" + uri.getHost());

            java.net.URLConnection connection = uri.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("User-Agent", "TheSandboxCore/1.0 Minecraft Plugin");
            connection.setRequestProperty("Accept", "image/gif, image/*");

            // Check content type if available
            String contentType = connection.getContentType();
            if (contentType != null) {
                logFine("Server response content-type: " + contentType);
                if (!contentType.toLowerCase().contains("gif") && !contentType.toLowerCase().contains("image")) {
                    logWarning("Unexpected content type: " + contentType + " (expected image/gif)");
                }
            }

            // Check content length if available
            int contentLength = connection.getContentLength();
            if (contentLength > 0) {
                logFine("Expected content length: " + formatBytes(contentLength));
                if (contentLength > MAX_BYTES) {
                    logWarning("File too large: " + formatBytes(contentLength) + " (max " + formatBytes(MAX_BYTES) + ")");
                    return null;
                }
            }

            logFine("Reading input stream...");
            try (var input = connection.getInputStream()) {
                java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                byte[] chunk = new byte[8192];
                int bytesRead;
                int total = 0;

                while ((bytesRead = input.read(chunk)) != -1) {
                    total += bytesRead;
                    if (total > MAX_BYTES) {
                        logWarning("Download exceeded size limit at " + formatBytes(total) + " (max " + formatBytes(MAX_BYTES) + ")");
                        return null;
                    }
                    buffer.write(chunk, 0, bytesRead);
                }

                logFine("Finished reading " + formatBytes(total) + " bytes");
                return buffer.toByteArray();
            }
        } catch (java.net.ConnectException e) {
            logWarning("Connection refused or timed out: " + url + " — " + e.getMessage());
            return null;
        } catch (java.net.SocketTimeoutException e) {
            logWarning("Download timed out: " + url + " — " + e.getMessage());
            return null;
        } catch (java.io.IOException e) {
            logWarning("I/O error downloading: " + url + " — " + e.getMessage());
            return null;
        } catch (Exception e) {
            logSevere("Unexpected exception downloading: " + url + " — " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
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

    private static String formatBytes(int bytes) {
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
