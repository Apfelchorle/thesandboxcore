package org.thesandbox.core.commands.trusted;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.commands.MessageService;
import org.thesandbox.core.commands.SafeConfigService;
import org.thesandbox.core.data.PlayerDataListener;
import org.thesandbox.core.services.economy.BalanceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Safe, typed configuration command.
 *
 * <p>Only keys registered in {@link SafeConfigService} can be read or written.
 * Secrets, database credentials, API keys, and {@code trusted-uuids} are not
 * reachable through this command.</p>
 */
public class ConfigCommand extends BaseCommand {

    private static final String USAGE = "/%s <get|set|list|keys> [key] [value] [player]";

    private final TheSandboxCore plugin;
    private final PlayerDataListener playerDataListener;

    public ConfigCommand(TheSandboxCore plugin, PlayerDataListener playerDataListener) {
        this.plugin = plugin;
        this.playerDataListener = playerDataListener;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender, String.format(USAGE, label));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "get" -> handleGet(sender, label, args);
            case "set" -> handleSet(sender, label, args);
            case "list" -> handleList(sender, label, args);
            case "keys" -> handleKeys(sender);
            default -> sendUsage(sender, String.format(USAGE, label));
        }
        return true;
    }

    // ------------------------------------------------------------------
    // get
    // ------------------------------------------------------------------

    private void handleGet(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sendUsage(sender, "/" + label + " get <key> [player]");
            return;
        }

        // Player form: /config get <key...> <player>
        Optional<SafeConfigService.KeySpec> playerKey =
                SafeConfigService.longestMatch(args, 1, false)
                        .filter(match -> match.endIndex() == args.length - 2)
                        .map(SafeConfigService.KeyMatch::spec);

        if (playerKey.isPresent()) {
            if (!hasPermission(sender, "sandbox.staff")) {
                return;
            }
            SafeConfigService.KeySpec spec = playerKey.get();
            if (!spec.readable()) {
                sendError(sender, MessageService.common(
                        "config-key-not-readable",
                        "That key is not readable."
                ));
                return;
            }
            String playerName = args[args.length - 1];
            OfflinePlayer target = resolveOfflineTarget(sender, playerName);
            if (target == null) {
                sendError(sender, MessageService.formatCommon(
                        "player-not-found",
                        "Player not found: %s",
                        playerName
                ));
                return;
            }
            readPlayerValue(sender, target, playerName, spec);
            return;
        }

        // Global form: /config get <key...>
        Optional<SafeConfigService.KeySpec> globalKey =
                SafeConfigService.longestMatch(args, 1, true)
                        .filter(match -> match.endIndex() == args.length - 1)
                        .map(SafeConfigService.KeyMatch::spec);

        if (globalKey.isPresent()) {
            if (!hasPermission(sender, "sandbox.admin")) {
                return;
            }
            SafeConfigService.KeySpec spec = globalKey.get();
            Object value = plugin.getConfig().get(spec.key());
            sendInformation(sender, MessageService.formatCommon(
                    "config-global-get",
                    "%s = %s",
                    spec.key(),
                    SafeConfigService.display(value)
            ));
            return;
        }

        sendError(sender, MessageService.common(
                "config-key-not-allowed",
                "That key is not editable through this command. Use /config list to see the supported keys."
        ));
    }

    private void readPlayerValue(CommandSender sender, OfflinePlayer target,
                                 String playerName, SafeConfigService.KeySpec spec) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Object value;
            if (spec.type() == SafeConfigService.Type.COINS) {
                value = plugin.getBalanceManager().getCoins(target.getUniqueId());
            } else {
                value = playerDataListener.get(target.getUniqueId(), spec.key(), null);
            }

            String message = MessageService.formatCommon(
                    "config-player-get",
                    "%s (%s) = %s",
                    spec.key(),
                    playerName,
                    SafeConfigService.display(value)
            );
            Bukkit.getScheduler().runTask(plugin, () -> sendInformation(sender, message));
        });
    }

    // ------------------------------------------------------------------
    // set
    // ------------------------------------------------------------------

    private void handleSet(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            sendUsage(sender, "/" + label + " set <key> <value> [player]");
            return;
        }

        // Player form: /config set <key...> <value...> <player>
        Optional<SafeConfigService.KeyMatch> playerMatch =
                SafeConfigService.longestMatch(args, 1, false)
                        .filter(match -> match.endIndex() < args.length - 2);

        if (playerMatch.isPresent()) {
            if (!hasPermission(sender, "sandbox.admin")) {
                return;
            }
            SafeConfigService.KeyMatch match = playerMatch.get();
            SafeConfigService.KeySpec spec = match.spec();
            if (!spec.writable()) {
                sendError(sender, MessageService.common(
                        "config-key-read-only",
                        "That key is read-only."
                ));
                return;
            }

            String playerName = args[args.length - 1];
            String rawValue = String.join(" ", Arrays.copyOfRange(
                    args, match.endIndex() + 1, args.length - 1
            ));

            OfflinePlayer target = resolveOfflineTarget(sender, playerName);
            if (target == null) {
                sendError(sender, MessageService.formatCommon(
                        "player-not-found",
                        "Player not found: %s",
                        playerName
                ));
                return;
            }

            Object parsed;
            try {
                parsed = SafeConfigService.parse(spec, rawValue);
            } catch (IllegalArgumentException e) {
                sendError(sender, MessageService.formatCommon(
                        "config-invalid-value",
                        "Invalid value for %s: %s",
                        spec.key(),
                        e.getMessage()
                ));
                return;
            }

            writePlayerValue(sender, target, playerName, spec, parsed);
            return;
        }

        // Global form: /config set <key...> <value...>
        Optional<SafeConfigService.KeyMatch> globalMatch =
                SafeConfigService.longestMatch(args, 1, true)
                        .filter(match -> match.endIndex() < args.length - 1);

        if (globalMatch.isPresent()) {
            if (!hasPermission(sender, "sandbox.admin")) {
                return;
            }
            SafeConfigService.KeyMatch match = globalMatch.get();
            SafeConfigService.KeySpec spec = match.spec();
            String rawValue = String.join(" ", Arrays.copyOfRange(
                    args, match.endIndex() + 1, args.length
            ));

            Object parsed;
            try {
                parsed = SafeConfigService.parse(spec, rawValue);
            } catch (IllegalArgumentException e) {
                sendError(sender, MessageService.formatCommon(
                        "config-invalid-value",
                        "Invalid value for %s: %s",
                        spec.key(),
                        e.getMessage()
                ));
                return;
            }

            plugin.getConfig().set(spec.key(), parsed);
            plugin.saveConfig();
            plugin.getLogger().info("[Config] " + sender.getName()
                    + " set " + spec.key() + " = " + SafeConfigService.display(parsed));
            sendSuccess(sender, MessageService.formatCommon(
                    "config-global-set",
                    "Updated %s.",
                    spec.key()
            ));
            return;
        }

        sendError(sender, MessageService.common(
                "config-key-not-allowed",
                "That key is not editable through this command. Use /config list to see the supported keys."
        ));
    }

    private void writePlayerValue(CommandSender sender, OfflinePlayer target,
                                  String playerName, SafeConfigService.KeySpec spec,
                                  Object parsed) {
        if (spec.type() == SafeConfigService.Type.COINS) {
            BalanceManager balances = plugin.getBalanceManager();
            if (balances == null || !balances.isReady()) {
                sendError(sender, MessageService.common(
                        "balance-not-ready",
                        "The balance service is not ready yet. Try again in a moment."
                ));
                return;
            }
            balances.setCoins(target.getUniqueId(), (Integer) parsed);
            logPlayerChange(sender, target, spec, parsed);
            sendSuccess(sender, MessageService.formatCommon(
                    "config-player-set",
                    "Set %s = %s for %s.",
                    spec.key(),
                    SafeConfigService.display(parsed),
                    playerName
            ));
            return;
        }

        // Load data off the main thread, then mutate on the main thread so
        // PlayerDataListener.saveIfOffline can safely query Bukkit.
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            playerDataListener.get(target.getUniqueId(), spec.key(), null);
            Bukkit.getScheduler().runTask(plugin, () -> {
                playerDataListener.set(target.getUniqueId(), spec.key(), parsed);
                logPlayerChange(sender, target, spec, parsed);
                sendSuccess(sender, MessageService.formatCommon(
                        "config-player-set",
                        "Set %s = %s for %s.",
                        spec.key(),
                        SafeConfigService.display(parsed),
                        playerName
                ));
            });
        });
    }

    private void logPlayerChange(CommandSender sender, OfflinePlayer target,
                                 SafeConfigService.KeySpec spec, Object parsed) {
        plugin.getLogger().info("[Config] " + sender.getName()
                + " set " + spec.key() + " = " + SafeConfigService.display(parsed)
                + " for " + target.getName() + " (" + target.getUniqueId() + ")");
    }

    // ------------------------------------------------------------------
    // list / keys
    // ------------------------------------------------------------------

    private void handleList(CommandSender sender, String label, String[] args) {
        if (args.length == 1 || args[1].equalsIgnoreCase("keys")) {
            handleKeys(sender);
            return;
        }

        if (args[1].equalsIgnoreCase("global")) {
            if (!hasPermission(sender, "sandbox.admin")) {
                return;
            }
            sendInformation(sender, MessageService.common(
                    "config-list-global",
                    "Safe global config keys:"
            ));
            for (SafeConfigService.KeySpec spec : SafeConfigService.globalSpecs()) {
                Object value = plugin.getConfig().get(spec.key());
                sendInformation(sender, spec.key() + " = " + SafeConfigService.display(value));
            }
            return;
        }

        if (!hasPermission(sender, "sandbox.staff")) {
            return;
        }

        String playerName = args[1];
        OfflinePlayer target = resolveOfflineTarget(sender, playerName);
        if (target == null) {
            sendError(sender, MessageService.formatCommon(
                    "player-not-found",
                    "Player not found: %s",
                    playerName
            ));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<String> lines = new ArrayList<>();
            for (SafeConfigService.KeySpec spec : SafeConfigService.playerSpecs()) {
                if (!spec.readable()) {
                    continue;
                }
                Object value = spec.type() == SafeConfigService.Type.COINS
                        ? plugin.getBalanceManager().getCoins(target.getUniqueId())
                        : playerDataListener.get(target.getUniqueId(), spec.key(), null);
                lines.add(spec.key() + " = " + SafeConfigService.display(value));
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                sendInformation(sender, MessageService.formatCommon(
                        "config-list-player",
                        "Player data for %s:",
                        playerName
                ));
                for (String line : lines) {
                    send(sender, "&7" + line);
                }
            });
        });
    }

    private void handleKeys(CommandSender sender) {
        if (!hasPermission(sender, "sandbox.staff")) {
            return;
        }
        sendInformation(sender, MessageService.common(
                "config-keys-player",
                "Safe player data keys:"
        ));
        for (SafeConfigService.KeySpec spec : SafeConfigService.playerSpecs()) {
            send(sender, "&7" + SafeConfigService.describe(spec));
        }
        if (sender.hasPermission("sandbox.admin")) {
            sendInformation(sender, MessageService.common(
                    "config-keys-global",
                    "Safe global config keys:"
            ));
            for (SafeConfigService.KeySpec spec : SafeConfigService.globalSpecs()) {
                send(sender, "&7" + SafeConfigService.describe(spec));
            }
        }
    }

    // ------------------------------------------------------------------
    // tab completion
    // ------------------------------------------------------------------

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("sandbox.staff")) {
            return Collections.emptyList();
        }

        if (args.length == 0) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filter(List.of("get", "set", "list", "keys"), args[0]);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        boolean admin = sender.hasPermission("sandbox.admin");

        if (sub.equals("keys")) {
            return Collections.emptyList();
        }

        if (sub.equals("list")) {
            if (args.length == 2) {
                List<String> values = new ArrayList<>();
                values.add("global");
                if (admin) {
                    values.add("keys");
                }
                values.addAll(offlinePlayerCompletions(""));
                return filter(values, args[1]);
            }
            return Collections.emptyList();
        }

        // get/set: key first, then value/player completions.
        Optional<SafeConfigService.KeyMatch> playerMatch =
                SafeConfigService.longestMatch(args, 1, false);
        if (playerMatch.isPresent()) {
            return completePlayerKey(args, playerMatch.get(), sub);
        }

        Optional<SafeConfigService.KeyMatch> globalMatch =
                SafeConfigService.longestMatch(args, 1, true);
        if (admin && globalMatch.isPresent()) {
            return completeGlobalKey(args, globalMatch.get(), sub);
        }

        if (args.length == 2) {
            List<String> keys = new ArrayList<>(SafeConfigService.playerKeyNames());
            if (admin) {
                keys.addAll(SafeConfigService.globalKeyNames());
            }
            return filter(keys, args[1]);
        }

        return Collections.emptyList();
    }

    private List<String> completePlayerKey(String[] args, SafeConfigService.KeyMatch match, String sub) {
        if (sub.equals("get")) {
            return args.length == match.endIndex() + 2
                    ? offlinePlayerCompletions(args[args.length - 1])
                    : Collections.emptyList();
        }

        // set <key> <value...> [player]
        if (args.length == match.endIndex() + 2) {
            return valueCompletions(match.spec(), args[args.length - 1]);
        }
        if (args.length > match.endIndex() + 2) {
            return offlinePlayerCompletions(args[args.length - 1]);
        }
        return Collections.emptyList();
    }

    private List<String> completeGlobalKey(String[] args, SafeConfigService.KeyMatch match, String sub) {
        if (sub.equals("get")) {
            return Collections.emptyList();
        }
        if (args.length == match.endIndex() + 2) {
            return valueCompletions(match.spec(), args[args.length - 1]);
        }
        return Collections.emptyList();
    }

    private List<String> valueCompletions(SafeConfigService.KeySpec spec, String prefix) {
        if (spec.type() == SafeConfigService.Type.BOOLEAN) {
            return filter(List.of("true", "false"), prefix);
        }
        if (spec.type() == SafeConfigService.Type.STRING && !spec.allowed().isEmpty()) {
            return filter(new ArrayList<>(spec.allowed()), prefix);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> values, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(value);
            }
        }
        return out;
    }
}
