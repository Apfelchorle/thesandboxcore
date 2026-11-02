package org.thesandbox.core.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.thesandbox.core.util.colors.Colors;

import java.io.File;
import java.util.Locale;

/**
 * Central theme/message layer for every command and feature.
 *
 * <p>Prefixes and common messages live in {@code messages.yml}. Values use the
 * same legacy {@code &}/{@code &#RRGGBB} syntax the rest of the plugin already
 * speaks, so server owners can re-theme the plugin without touching code.</p>
 *
 * <p>Player-controlled text must never be passed through MiniMessage here. This
 * class only parses trusted, config-supplied strings.</p>
 */
public final class MessageService {

    public enum Prefix {
        SERVER,
        INFO,
        ERROR,
        USAGE,
        COMMAND,
        SUCCESS
    }

    private static final LegacyComponentSerializer SECTION_SERIALIZER =
            LegacyComponentSerializer.legacySection();

    private static volatile YamlConfiguration config;
    private static volatile JavaPlugin plugin;

    private MessageService() {
    }

    public static synchronized void load(JavaPlugin owner) {
        plugin = owner;
        File file = new File(owner.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            owner.saveResource("messages.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public static synchronized void reload() {
        if (plugin == null) {
            return;
        }
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Returns the raw configured prefix (legacy syntax) for a message type.
     */
    public static String prefix(Prefix prefix) {
        String path = "prefixes." + prefix.name().toLowerCase(Locale.ROOT);
        YamlConfiguration current = config;
        return current == null
                ? defaultPrefix(prefix)
                : current.getString(path, defaultPrefix(prefix));
    }

    /**
     * Builds a prefixed component. The body is treated as legacy text, not
     * MiniMessage, so player-supplied values cannot inject click/hover tags.
     */
    public static Component component(Prefix prefix, String body) {
        return Colors.component(prefix(prefix) + (body == null ? "" : body));
    }

    /**
     * Legacy-string form, kept for the existing {@code CommandMessages} API.
     */
    public static String legacy(Prefix prefix, String body) {
        return SECTION_SERIALIZER.serialize(component(prefix, body));
    }

    /**
     * Parses a trusted legacy string (config/theme values only).
     */
    public static Component plain(String legacyText) {
        return Colors.component(legacyText);
    }

    /**
     * Reads a common message from {@code messages.<key>}.
     */
    public static String common(String key, String fallback) {
        YamlConfiguration current = config;
        return current == null
                ? fallback
                : current.getString("messages." + key, fallback);
    }

    public static Component commonComponent(String key, String fallback) {
        return Colors.component(common(key, fallback));
    }

    /**
     * Formats a common message. Placeholders are {@code %s} in messages.yml.
     * The result is parsed as legacy text, and callers must pass already-safe
     * values (plain player names, numbers, etc.).
     */
    public static String formatCommon(String key, String fallback, Object... args) {
        String template = common(key, fallback);
        try {
            return String.format(template, args);
        } catch (java.util.IllegalFormatException e) {
            // A server owner may put a literal '%' in messages.yml. Keep the
            // command usable instead of throwing from the message layer.
            return template;
        }
    }

    private static String defaultPrefix(Prefix prefix) {
        return switch (prefix) {
            case SERVER -> "&c&lServer &8• &c";
            case INFO -> "&b&lInfo &8» &b";
            case ERROR -> "&c&lError &8» &c";
            case USAGE -> "&e&lUsage &8» &e";
            case COMMAND -> "&7&lCommand &8» &7";
            case SUCCESS -> "&a&lSuccess &8» &a";
        };
    }
}
