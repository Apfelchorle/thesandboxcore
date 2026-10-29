package org.thesandbox.core.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.fun.Utils;
import org.thesandbox.core.util.BalanceRepository;
import org.thesandbox.core.util.PlayerDataKeys;
import org.thesandbox.core.util.PlayerDataListener;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntUnaryOperator;

public class BalanceManager {

    /** A single row in the coin leaderboard. */
    public record CoinBalance(UUID uuid, int coins) {}

    private final TheSandboxCore plugin;
    private final BalanceRepository repository;

    /** Hot path cache; the database is the source of truth. */
    private final Map<UUID, Integer> cache = new ConcurrentHashMap<>();

    /** UUIDs with a cache value that has not been confirmed written to SQL yet. */
    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();

    /** UUIDs whose writes are currently being drained, so updates stay ordered. */
    private final Set<UUID> saving = ConcurrentHashMap.newKeySet();

    public BalanceManager(TheSandboxCore plugin, DataSource dataSource) {
        this.plugin = plugin;
        this.repository = new BalanceRepository(dataSource);
    }

    private PlayerDataListener playerData() {
        return plugin.getDataListener();
    }

    public void init() {
        try {
            repository.ensureSchema();

            // First SQL deployment: import the old YAML balances before the cache is warmed.
            if (repository.count() == 0) {
                migrateYamlBalances();
            } else {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, this::migrateYamlBalances);
            }

            cache.putAll(repository.loadAll());
        } catch (SQLException e) {
            plugin.getLogger().severe("[Balances] Could not initialize SQL storage: " + e.getMessage());
        }

        Utils.log("Initialized BalanceManager (SQL)");
    }

    /** Copies coins from the legacy player_data/*.yml files into SQL, never overwriting existing rows. */
    private void migrateYamlBalances() {
        int imported = 0;
        for (UUID uuid : playerData().knownPlayerIds()) {
            int coins = playerData().getCoins(uuid);
            if (coins <= 0) continue;

            try {
                if (repository.importIfMissing(uuid, coins)) imported++;
            } catch (SQLException e) {
                plugin.getLogger().warning("[Balances] YAML migration failed for " + uuid + ": " + e.getMessage());
            }
        }

        if (imported > 0) {
            plugin.getLogger().info("[Balances] Imported " + imported + " balances from player_data into SQL.");
        }
    }



    public void TakeCoins(CommandSender sender, Player target, int amount, String[] args) {
        Integer parsed = parsePositiveAmount(sender, amount, args);
        if (parsed == null) {
            return;
        }
        if (!requireOnlineTarget(sender, target)) {
            return;
        }
        if (isSamePlayer(sender, target)) {
            sender.sendMessage(Component.text("[Error]: You cannot take coins from yourself.", NamedTextColor.RED));
            return;
        }

        int taken = Math.min(parsed, getCoins(target.getUniqueId()));
        if (taken <= 0) {
            sender.sendMessage(Component.text(target.getName() + " has no coins to take.", NamedTextColor.RED));
            return;
        }

        removeCoins(target.getUniqueId(), taken);
        Bukkit.broadcast(Component.text(
                sender.getName() + " Has Taken " + taken + " Coins From " + target.getName(),
                NamedTextColor.GOLD
        ));
        TransactionMessage(target, "You've Been Beanzz'd by hehe", NamedTextColor.DARK_PURPLE);
        if (sender instanceof Player staff) {
            addCoins(staff.getUniqueId(), taken);
            TransactionMessage(staff, "You've Taken " + taken + " Coins From " + target.getName(), NamedTextColor.DARK_GREEN);
        }
    }

    public void TransferCoins(Player source, Player receive, int amount, String[] args) {
        Integer parsed = parsePositiveAmount(source, amount, args);
        if (parsed == null) {
            return;
        }
        if (!requireOnlineTarget(source, receive)) {
            return;
        }
        if (source.getUniqueId().equals(receive.getUniqueId())) {
            source.sendMessage(Component.text("[Error]: You cannot send coins to yourself.", NamedTextColor.RED));
            return;
        }
        if (getCoins(source.getUniqueId()) < parsed) {
            source.sendMessage(Component.text("You don't have enough coins!", NamedTextColor.RED));
            Utils.Feedback(source, "Error: Transaction Cancelled", NamedTextColor.RED, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        removeCoins(source.getUniqueId(), parsed);
        addCoins(receive.getUniqueId(), parsed);
        Bukkit.broadcast(Component.text(source.getName() + " has given " + receive.getName() + " " + parsed + " coins!", NamedTextColor.GOLD));
        TransactionMessage(receive, "You've Received " + parsed + " Coins From " + source.getName() + "!", NamedTextColor.DARK_GREEN);
        TransactionMessage(source, "You've Sent " + parsed + " Coins To " + receive.getName(), NamedTextColor.GOLD);
        Utils.SendMessage(source, receive.getName() + " has " + getCoins(receive.getUniqueId()) + " Coins!", NamedTextColor.GREEN);
        Utils.SendMessage(source, "Current Balance: " + getCoins(source.getUniqueId()) + " Coins!", NamedTextColor.YELLOW);
    }

    public void AddCoins(CommandSender sender, String target, int amount, String[] args) {
        Integer parsed = parsePositiveAmount(sender, amount, args);
        if (parsed == null) {
            return;
        }
        Player targetPlayer = org.thesandbox.core.util.Internals.FunctionsHolder.resolveTarget(sender, target);
        if (!requireOnlineTarget(sender, targetPlayer)) {
            return;
        }

        addCoins(targetPlayer.getUniqueId(), parsed);
        Bukkit.broadcast(Component.text(
                sender.getName() + " has given " + targetPlayer.getName() + " " + parsed + " coins!",
                NamedTextColor.GOLD
        ));
        TransactionMessage(targetPlayer, "You've Recieved " + parsed + " Coins From " + sender.getName(), NamedTextColor.GREEN);
        sender.sendMessage(Component.text(
                targetPlayer.getName() + " has " + getCoins(targetPlayer.getUniqueId()) + " Coins!",
                NamedTextColor.GREEN
        ));
    }

    private Integer parsePositiveAmount(CommandSender sender, int amount, String[] args) {
        int value = amount;
        if (args != null && args.length >= 3) {
            try {
                value = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("[Error]: " + args[2] + " is not a number.", NamedTextColor.RED));
                return null;
            }
        }
        if (value <= 0) {
            sender.sendMessage(Component.text("Dude.", NamedTextColor.DARK_RED, TextDecoration.ITALIC, TextDecoration.BOLD));
            return null;
        }
        return value;
    }

    private boolean requireOnlineTarget(CommandSender sender, Player target) {
        if (target == null || !target.isOnline()) {
            sender.sendMessage(Component.text("Player is offline!", NamedTextColor.DARK_GRAY));
            return false;
        }
        return true;
    }

    private boolean isSamePlayer(CommandSender sender, Player target) {
        return sender instanceof Player player && player.getUniqueId().equals(target.getUniqueId());
    }

    private void TransactionMessage(Player player, String message, NamedTextColor color) {
        Utils.Feedback(player, message, color, Sound.ENTITY_ARROW_HIT_PLAYER);
    }

    public int getCoins(UUID uuid) {
        Integer cached = cache.get(uuid);
        if (cached != null) return cached;

        Integer stored = queryCoins(uuid);
        if (stored == null) {
            // Database outage: read the legacy YAML value without caching it so the
            // next call retries SQL once the database comes back.
            return playerData().get(uuid, PlayerDataKeys.COINS, 0);
        }

        cache.put(uuid, stored);
        return stored;
    }

    public void setCoins(UUID uuid, int coins) {
        cache.put(uuid, Math.max(0, coins));
        persist(uuid);
    }

    public void addCoins(UUID uuid, int amount) {
        updateCoins(uuid, current -> (int) Math.clamp((long) current + amount, 0, Integer.MAX_VALUE));
    }

    public void removeCoins(UUID uuid, int amount) {
        updateCoins(uuid, current -> (int) Math.max(0L, (long) current - amount));
    }

    private void updateCoins(UUID uuid, IntUnaryOperator update) {
        // compute() keeps read/modify/write atomic without recursively touching the cache.
        cache.compute(uuid, (id, current) -> {
            Integer base = current != null ? current : queryCoins(id);
            if (base == null) base = playerData().get(id, PlayerDataKeys.COINS, 0);
            return update.applyAsInt(base);
        });
        persist(uuid);
    }

    /** Returns null when SQL is unavailable, so callers can fall back to YAML. */
    private Integer queryCoins(UUID uuid) {
        try {
            Integer stored = repository.getCoins(uuid);
            return stored == null ? 0 : Math.max(0, stored);
        } catch (SQLException e) {
            plugin.getLogger().warning("[Balances] Could not load " + uuid + ": " + e.getMessage());
            return null;
        }
    }

    private void persist(UUID uuid) {
        dirty.add(uuid);
        if (!saving.add(uuid)) return; // an existing save loop will pick up the new value

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = true;
            try {
                while (true) {
                    Integer value = cache.get(uuid);
                    if (value == null) break;

                    repository.upsert(uuid, value);

                    // A newer value arrived while this write was in flight; write that one too.
                    if (Objects.equals(cache.get(uuid), value)) {
                        dirty.remove(uuid);
                        break;
                    }
                }
            } catch (SQLException e) {
                success = false;
                plugin.getLogger().warning("[Balances] Could not save " + uuid + ": " + e.getMessage());
            } finally {
                saving.remove(uuid);
                // Only retry automatically after a successful drain; on failure the next
                // coin change will try again instead of hammering a dead database.
                if (success && dirty.contains(uuid)) {
                    persist(uuid);
                }
            }
        });
    }

    /** Synchronously writes any pending coin changes. Called during plugin shutdown. */
    public void flush() {
        for (UUID uuid : Set.copyOf(dirty)) {
            Integer value = cache.get(uuid);
            if (value == null) continue;

            try {
                repository.upsert(uuid, value);
                if (Objects.equals(cache.get(uuid), value)) {
                    dirty.remove(uuid);
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("[Balances] Could not flush " + uuid + ": " + e.getMessage());
            }
        }
    }

    /**
     * Highest coin balances, largest first. Reads SQL, so call this off the main thread.
     * Values still waiting to be written are overlaid from the cache.
     */
    public List<CoinBalance> getTopCoins(int limit) {
        int wanted = Math.max(1, limit);
        Map<UUID, Integer> balances = new HashMap<>();
        try {
            // Ask for a few extra rows so pending (dirty) balances can move into the top.
            balances.putAll(repository.top(wanted + dirty.size()));
        } catch (SQLException e) {
            plugin.getLogger().warning("[Balances] Could not load leaderboard: " + e.getMessage());
            // Database outage: fall back to the legacy YAML values instead of showing an empty top.
            for (UUID uuid : playerData().knownPlayerIds()) {
                balances.put(uuid, playerData().getCoins(uuid));
            }
        }

        // Overlay every pending balance, including ones the SQL top-N query did not return.
        for (UUID uuid : dirty) {
            Integer value = cache.get(uuid);
            if (value != null) balances.put(uuid, value);
        }

        return balances.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> new CoinBalance(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(CoinBalance::coins).reversed())
                .limit(wanted)
                .toList();
    }
}
