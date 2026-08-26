package org.thesandbox.core.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.thesandbox.core.TheSandboxCore;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

public class UpdateCommand implements ISubCommand {

    private final TheSandboxCore plugin;


    public UpdateCommand(TheSandboxCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sandbox.superuser")) {
            sender.sendMessage(Component.text("You do not have permission to use this command!", NamedTextColor.RED));
            return true;
        }

        String token = plugin.getConfig().getString("update.github-token", "");
        String repo = plugin.getConfig().getString("update.repo", "");

        if (token.isEmpty() || repo.isEmpty()) {
            sender.sendMessage(Component.text("Update is not configured (missing token or repo).", NamedTextColor.RED));
            return true;
        }

        sender.sendMessage(Component.text("Checking for updates...", NamedTextColor.YELLOW));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String apiUrl = "https://api.github.com/repos/" + repo + "/releases/latest";
                HttpURLConnection metaConn = openAuthedConnection(apiUrl, token);

                String json;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(metaConn.getInputStream()))) {
                    json = reader.lines().collect(Collectors.joining());
                }

                String latestVersion = extractTagName(json);
                long assetId = extractAssetId(json);

                String lastKnownTag = plugin.getConfig().getString("update.last-installed-tag", "");

                if (latestVersion.equals(lastKnownTag)) {
                    sender.sendMessage(Component.text("Already up to date! (" + latestVersion + ")", NamedTextColor.GREEN));
                    return;
                }

                String assetUrl = "https://api.github.com/repos/" + repo + "/releases/assets/" + assetId;
                HttpURLConnection assetConn = openAuthedConnection(assetUrl, token);
                assetConn.setRequestProperty("Accept", "application/octet-stream");

                File updateFolder = plugin.getServer().getUpdateFolderFile();
                if (!updateFolder.exists()) updateFolder.mkdirs();
                File targetJar = new File(updateFolder, "thesandboxcore-1.0.0.jar");

                try (InputStream in = assetConn.getInputStream()) {
                    Files.copy(in, targetJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                sender.sendMessage(Component.text("Downloaded update " + latestVersion + ". Restart to apply.", NamedTextColor.GREEN));
                plugin.getConfig().set("update.last-installed-tag", latestVersion);
                plugin.saveConfig();

            } catch (Exception e) {
                sender.sendMessage(Component.text("Update check failed: " + e.getMessage(), NamedTextColor.RED));
                plugin.getLogger().severe("Update failed: " + e);
            }
        });

        return true;
    }

    private HttpURLConnection openAuthedConnection(String urlStr, String token) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setRequestProperty("User-Agent", "TheSandboxCore-Updater");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        return conn;
    }

    private String extractTagName(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        return root.get("tag_name").getAsString();
    }

    private long extractAssetId(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray assets = root.getAsJsonArray("assets");

        for (var element : assets) {
            JsonObject asset = element.getAsJsonObject();
            if (asset.get("name").getAsString().equals("thesandboxcore-1.0.0.jar")) {
                return asset.get("id").getAsLong();
            }
        }

        throw new IllegalStateException("Could not find thesandboxcore-1.0.0.jar in the latest release's assets.");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}