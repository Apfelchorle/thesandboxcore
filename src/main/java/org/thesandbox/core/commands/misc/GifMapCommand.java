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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Allows players to create animated Minecraft maps from Discord GIF URLs.
 * <p>
 * Usage: /gifmap &lt;url&gt;
 * The command downloads the GIF, decodes all frames, and gives the player
 * a filled map item that animates when held.
 */
public class GifMapCommand extends BaseCommand {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern DISCORD_GIF_PATTERN = Pattern.compile(
            "https?://(?:[a-z0-9-]+\\.)?discord(?:app)?\\.(?:com|net|app)/attachments/\\d+/\\d+/[^\\s?#]+\\.gif(?:[?#]\\S*)?",
            Pattern.CASE_INSENSITIVE
    );

    private final TheSandboxCore plugin;

    public GifMapCommand(TheSandboxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1 || args[0].isBlank()) {
            sender.sendMessage(Component.text(
                    "[GifMap] Usage: /gifmap <gif-url>", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text(
                    "  Creates an animated map from a Discord GIF URL.", NamedTextColor.GRAY));
            return true;
        }

        // Extract URL from arguments
        String input = String.join(" ", args);
        Matcher matcher = URL_PATTERN.matcher(input);
        if (!matcher.find()) {
            sender.sendMessage(Component.text(
                    "[GifMap] Could not find a valid URL.", NamedTextColor.RED));
            return true;
        }

        String url = matcher.group(0);

        // Check if it's a Discord GIF URL
        if (!DISCORD_GIF_PATTERN.matcher(url).find()) {
            sender.sendMessage(Component.text(
                    "[GifMap] Please provide a Discord GIF attachment URL.", NamedTextColor.RED));
            sender.sendMessage(Component.text(
                    "  Tip: Right-click a GIF in Discord and copy the link.", NamedTextColor.GRAY));
            return true;
        }

        // Get the animated map service from Discord bridge
        AnimatedMapService mapService = plugin.getDiscord() != null
                ? plugin.getDiscord().getAnimatedMapService()
                : null;

        if (mapService == null) {
            sender.sendMessage(Component.text(
                    "[GifMap] Animated map service is not available.", NamedTextColor.RED));
            return true;
        }

        sender.sendMessage(Component.text(
                "[GifMap] Downloading and creating animated map...", NamedTextColor.YELLOW));

        // Download the GIF and create the map
        downloadAndCreateMap(player, url, mapService);

        return true;
    }

    private void downloadAndCreateMap(Player player, String url, AnimatedMapService mapService) {
        // Download the GIF bytes asynchronously
        new Thread(() -> {
            try {
                byte[] gifBytes = downloadUrl(url);
                if (gifBytes == null || gifBytes.length == 0) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        player.sendMessage(Component.text(
                                "[GifMap] Failed to download the GIF.", NamedTextColor.RED));
                    });
                    return;
                }

                mapService.createAndGiveMap(player, gifBytes);
            } catch (Throwable t) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage(Component.text(
                            "[GifMap] Error: " + t.getMessage(), NamedTextColor.RED));
                });
            }
        }, "GifMap-Download").start();
    }

    private byte[] downloadUrl(String url) {
        try {
            java.net.URL uri = new java.net.URL(url);
            java.net.URLConnection connection = uri.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent", "TheSandboxCore/1.0");

            try (var input = connection.getInputStream()) {
                java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                byte[] chunk = new byte[8192];
                int bytesRead;
                int total = 0;
                int maxBytes = 8 * 1024 * 1024; // 8MB limit

                while ((bytesRead = input.read(chunk)) != -1) {
                    total += bytesRead;
                    if (total > maxBytes) {
                        return null; // Too large
                    }
                    buffer.write(chunk, 0, bytesRead);
                }
                return buffer.toByteArray();
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
