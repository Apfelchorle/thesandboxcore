package org.thesandbox.core.commands;

import org.thesandbox.core.data.PlayerDataKeys;
import org.thesandbox.core.util.colors.Colors;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Allowlist and type system for {@code /config} and
 * {@code /thesandboxcore config}.
 *
 * <p>The old implementation accepted any config path, which let staff read
 * tokens/database credentials and write {@code trusted-uuids}. This class only
 * exposes explicitly registered, typed keys. Secrets and identity lists are
 * deliberately absent.</p>
 */
public final class SafeConfigService {

    public enum Type {
        BOOLEAN,
        INTEGER,
        STRING,
        COINS
    }

    public record KeySpec(
            String key,
            Type type,
            boolean readable,
            boolean writable,
            int min,
            int max,
            int maxLength,
            Set<String> allowed,
            String description
    ) {
        public static KeySpec bool(String key, boolean readable, boolean writable, String description) {
            return new KeySpec(key, Type.BOOLEAN, readable, writable, 0, 0, 0, Set.of(), description);
        }

        public static KeySpec integer(String key, int min, int max, String description) {
            return new KeySpec(key, Type.INTEGER, true, true, min, max, 0, Set.of(), description);
        }

        public static KeySpec string(String key, int maxLength, String description) {
            return new KeySpec(key, Type.STRING, true, true, 0, 0, maxLength, Set.of(), description);
        }

        public static KeySpec enumOf(String key, Set<String> allowed, String description) {
            // Preserve the configured spelling so stored values match what the
            // rest of the plugin expects (e.g. JumpPadMode.valueOf).
            return new KeySpec(key, Type.STRING, true, true, 0, 0, 64,
                    Collections.unmodifiableSet(new LinkedHashSet<>(allowed)), description);
        }

        public static KeySpec coins(String key, String description) {
            return new KeySpec(key, Type.COINS, true, true, 0, Integer.MAX_VALUE, 0, Set.of(), description);
        }
    }

    public record KeyMatch(KeySpec spec, int endIndex) {
    }

    private static final Map<String, KeySpec> PLAYER_KEYS = new LinkedHashMap<>();
    private static final Map<String, KeySpec> GLOBAL_KEYS = new LinkedHashMap<>();

    static {
        // ---- Player data (allowlisted) ----
        player(KeySpec.bool(PlayerDataKeys.FIRST_TIME_JOINING, true, true, "First-join flag"));
        player(KeySpec.coins(PlayerDataKeys.COINS, "Coin balance (goes through BalanceManager)"));
        player(KeySpec.enumOf(PlayerDataKeys.JUMPPADS_MODE,
                Set.of("OFF", "NORMAL", "NORMAL_AND_SIDEWAYS"), "Jump pad mode"));
        player(KeySpec.string(PlayerDataKeys.LOGIN_MESSAGE, 256, "Login message"));
        player(KeySpec.enumOf(PlayerDataKeys.LIGHTNING_ROD,
                Set.of("owned", "not_owned"), "Lightning rod ownership"));
        player(KeySpec.enumOf(PlayerDataKeys.LOGIN_MESSAGES_STATE,
                Set.of("owned", "not_owned"), "Login-messages item ownership"));
        player(KeySpec.enumOf(PlayerDataKeys.CLOWN_FISH,
                Set.of("owned", "not_owned"), "Clownfish ownership"));
        player(KeySpec.enumOf(PlayerDataKeys.RIDEABLE_ENDER_PEARL,
                Set.of("owned", "not_owned"), "Rideable ender pearl ownership"));
        player(KeySpec.enumOf(PlayerDataKeys.GRAPPLING_HOOK,
                Set.of("owned", "not_owned"), "Grappling hook ownership"));
        player(KeySpec.enumOf(PlayerDataKeys.Stacking_Potato,
                Set.of("owned", "not_owned"), "Stacking potato ownership"));
        player(KeySpec.enumOf(PlayerDataKeys.FLOAT_BOAT,
                Set.of("owned", "not_owned"), "Float boat ownership"));
        player(KeySpec.enumOf(PlayerDataKeys.WIND_ROD,
                Set.of("owned", "not_owned"), "Wind rod ownership"));
        player(KeySpec.enumOf(PlayerDataKeys.MARRIAGE_STATUS,
                Set.of("Single", "married", "divorced"), "Marriage status"));
        player(KeySpec.string(PlayerDataKeys.MARRIAGE_SPOUSE, 16, "Marriage spouse"));
        player(KeySpec.string(PlayerDataKeys.GENDER, 32, "Gender"));
        player(KeySpec.bool(PlayerDataKeys.VANISHED, true, true, "Vanish flag"));
        player(KeySpec.bool(PlayerDataKeys.CHATFILTER, true, true, "Chat-filter preference"));
        player(new KeySpec(PlayerDataKeys.LAST_RANK, Type.STRING, true, false,
                0, 0, 64, Set.of(), "Last known rank (read-only)"));
        player(new KeySpec(PlayerDataKeys.LAST_IP, Type.STRING, false, false,
                0, 0, 64, Set.of(), "Last IP (private)"));
        player(new KeySpec(PlayerDataKeys.LAST_LOCATION, Type.STRING, false, false,
                0, 0, 512, Set.of(), "Last location (private)"));

        // ---- Global config (allowlisted, non-sensitive) ----
        global(KeySpec.bool("discord.enabled", true, true, "Enable the Discord bridge"));
        global(KeySpec.bool("discord.chat-bridge.enabled", true, true, "Enable the Minecraft/Discord chat bridge"));
        global(KeySpec.bool("discord.use-embeds", true, true, "Use Discord embeds"));
        global(KeySpec.string("discord.invite-url", 512, "Discord invite URL"));
        global(KeySpec.string("commandspy.format", 512, "CommandSpy message format"));
        global(KeySpec.string("commandspy.redact_placeholder", 64, "CommandSpy redaction placeholder"));
        global(KeySpec.integer("loop.max-iterations", 1, 100000, "Maximum /loop iterations"));
        global(KeySpec.integer("slowrestart.max-seconds", 1, 86400, "Maximum restart countdown"));
        global(KeySpec.integer("slowrestart.default-seconds", 1, 86400, "Default restart countdown"));
        global(KeySpec.string("gtfo.duration", 32, "GTFO ban duration"));
        global(KeySpec.bool("mysql.sandbox.use-ssl", true, true, "Use TLS for MySQL"));
        global(KeySpec.bool("mysql.sandbox.require-ssl", true, true, "Require TLS for MySQL"));
        global(KeySpec.bool("mysql.sandbox.allow-public-key-retrieval", true, true, "Allow MySQL public-key retrieval"));
        global(KeySpec.bool("update.allow-unverified", true, true, "Allow updates without a GitHub digest"));
    }

    private SafeConfigService() {
    }

    private static void player(KeySpec spec) {
        PLAYER_KEYS.put(spec.key().toLowerCase(Locale.ROOT), spec);
    }

    private static void global(KeySpec spec) {
        GLOBAL_KEYS.put(spec.key().toLowerCase(Locale.ROOT), spec);
    }

    public static Optional<KeySpec> playerKey(String key) {
        return key == null ? Optional.empty() : Optional.ofNullable(PLAYER_KEYS.get(key.toLowerCase(Locale.ROOT)));
    }

    public static Optional<KeySpec> globalKey(String key) {
        return key == null ? Optional.empty() : Optional.ofNullable(GLOBAL_KEYS.get(key.toLowerCase(Locale.ROOT)));
    }

    public static List<String> playerKeyNames() {
        return PLAYER_KEYS.values().stream().map(KeySpec::key).toList();
    }

    public static List<String> globalKeyNames() {
        return GLOBAL_KEYS.values().stream().map(KeySpec::key).toList();
    }

    public static List<KeySpec> playerSpecs() {
        return List.copyOf(PLAYER_KEYS.values());
    }

    public static List<KeySpec> globalSpecs() {
        return List.copyOf(GLOBAL_KEYS.values());
    }

    /**
     * Finds the longest key (keys may contain spaces) starting at {@code start}.
     */
    public static Optional<KeyMatch> longestMatch(String[] args, int start, boolean global) {
        if (args == null || start < 0 || start >= args.length) {
            return Optional.empty();
        }
        Map<String, KeySpec> keys = global ? GLOBAL_KEYS : PLAYER_KEYS;
        KeyMatch best = null;
        StringBuilder candidate = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) {
                candidate.append(' ');
            }
            candidate.append(args[i]);
            KeySpec spec = keys.get(candidate.toString().toLowerCase(Locale.ROOT));
            if (spec != null) {
                best = new KeyMatch(spec, i);
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * Parses and validates a raw value against a key specification.
     *
     * @throws IllegalArgumentException when the value is not valid for the key
     */
    public static Object parse(KeySpec spec, String raw) {
        if (spec == null) {
            throw new IllegalArgumentException("Unknown key");
        }
        String value = raw == null ? "" : raw.strip();

        return switch (spec.type()) {
            case BOOLEAN -> {
                if (value.equalsIgnoreCase("true")) yield Boolean.TRUE;
                if (value.equalsIgnoreCase("false")) yield Boolean.FALSE;
                throw new IllegalArgumentException("Expected true or false");
            }
            case INTEGER, COINS -> {
                int parsed;
                try {
                    parsed = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Expected a whole number");
                }
                if (parsed < spec.min() || parsed > spec.max()) {
                    throw new IllegalArgumentException(
                            "Expected a number between " + spec.min() + " and " + spec.max()
                    );
                }
                yield parsed;
            }
            case STRING -> {
                if (value.length() > spec.maxLength()) {
                    throw new IllegalArgumentException(
                            "Value is too long (max " + spec.maxLength() + " characters)"
                    );
                }
                if (value.chars().anyMatch(Character::isISOControl)) {
                    throw new IllegalArgumentException("Value contains control characters");
                }
                if (!spec.allowed().isEmpty()) {
                    String canonical = spec.allowed().stream()
                            .filter(allowed -> allowed.equalsIgnoreCase(value))
                            .findFirst()
                            .orElse(null);
                    if (canonical == null) {
                        throw new IllegalArgumentException(
                                "Allowed values: " + String.join(", ", spec.allowed())
                        );
                    }
                    yield canonical;
                }
                yield value;
            }
        };
    }

    /**
     * Safe display form for values shown in chat. Colour codes are removed so
     * player-entered text cannot re-colour or spoof the command prefix.
     */
    public static String display(Object value) {
        if (value == null) {
            return "(not set)";
        }
        // Strip legacy colours and collapse control characters so stored text
        // cannot re-colour or break the command output.
        return Colors.strip(String.valueOf(value))
                .replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", " ")
                .replace('\n', ' ')
                .replace('\t', ' ');
    }

    public static String describe(KeySpec spec) {
        String access = spec.readable() && spec.writable()
                ? "read/write"
                : spec.readable() ? "read-only" : "hidden";
        return spec.key() + " (" + spec.type().name().toLowerCase(Locale.ROOT)
                + ", " + access + ") - " + spec.description();
    }
}
