package org.thesandbox.core.commands.utility;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.thesandbox.core.TheSandboxCore;
import org.thesandbox.core.commands.BaseCommand;
import org.thesandbox.core.util.colors.Colors;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CageCommand extends BaseCommand implements Listener
{
    private final TheSandboxCore plugin;
    private final File cageFile;

    // Active cages: caged player's UUID -> cage data
    private final Map<UUID, CageData> cages = new HashMap<>();

    // O(1) lookup for block-break protection: block position -> owning cage.
    private final Map<BlockPos, CageData> cageBlockIndex = new HashMap<>();

    // Recoveries that could not run yet because their world was not loaded.
    private final Map<UUID, PendingRestore> pendingRestores = new LinkedHashMap<>();

    // Cage snapshots are built on the main thread and written by a debounced
    // async worker. A generation counter prevents an older write from winning.
    private final AtomicReference<CageSave> pendingCageSave = new AtomicReference<>();
    private final AtomicBoolean cageSaveScheduled = new AtomicBoolean();
    private final Object cageWriteLock = new Object();
    private volatile long cageSaveGeneration;
    private BukkitTask cageSaveTask;

    // Optional WorldGuard integration, resolved once via reflection so the
    // plugin still builds and runs without a WorldGuard dependency.
    private volatile boolean worldGuardLookupDone;
    private volatile Method worldGuardCanBuild;
    private volatile Plugin worldGuardPlugin;

    public CageCommand(TheSandboxCore plugin)
    {
        this.plugin = plugin;
        this.cageFile = new File(plugin.getDataFolder(), "cages.yml");
        Bukkit.getPluginManager().registerEvents(this, plugin);
        loadCageRecords();
    }

    /* ============================== Command ============================== */

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args)
    {
        if (!sender.hasPermission("sandbox.staff"))
        {
            sender.sendMessage(Colors.color("&c&lError &8» &cNo permission."));
            return true;
        }
        if (args.length < 1)
        {
            sender.sendMessage(Colors.color("&c&lUsage &8» &c/" + label + " <player> [skull <username> | block <MATERIAL>] | /" + label + " purge"));
            return true;
        }

        // /cage purge
        if ("purge".equalsIgnoreCase(args[0]))
        {
            if (cages.isEmpty())
            {
                sender.sendMessage(Colors.color("&c&lError &8» &cNo players are currently caged."));
                return true;
            }
            // Uncage everyone safely
            for (UUID id : new HashSet<>(cages.keySet()))
            {
                Player p = Bukkit.getPlayer(id);
                if (p != null && p.isOnline())
                {
                    uncage(p);
                }
                else
                {
                    CageData d = cages.remove(id);
                    if (d != null && restoreAndRemoveFailed(d))
                    {
                        pendingRestores.put(id, toPending(id, d));
                    }
                }
            }
            saveCageRecords();
            Colors.broadcast(Colors.color("&c&lServer &8» &c" + sender.getName() + " has uncaged all players."));
            return true;
        }

        // Normal /cage <player> [skull <u> | block <mat>]
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null)
        {
            // partial match fallback
            List<Player> matches = Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.getName().toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
            if (matches.size() == 1) target = matches.getFirst();
        }
        if (target == null)
        {
            sender.sendMessage(Colors.color("&c&lError &8» &cPlayer not found."));
            return true;
        }

        UUID tid = target.getUniqueId();

        // Toggle OFF if already caged
        if (cages.containsKey(tid))
        {
            uncage(target);
            Colors.broadcast(Colors.color("&c&lServer &8» &c" + sender.getName() + " has uncaged " + target.getName() + "."));
            return true;
        }

        // Parse mode
        CageMode mode = CageMode.GLASS_DEFAULT;
        Material blockMat = Material.GLASS;
        String skullUser = null;

        if (args.length >= 2)
        {
            if (args[1].equalsIgnoreCase("skull"))
            {
                if (args.length < 3)
                {
                    sender.sendMessage(Colors.color("&c&lUsage &8» &c/" + label + " <target> skull <username>"));
                    return true;
                }
                mode = CageMode.SKULL;
                skullUser = args[2]; // keep EXACT user-typed casing for broadcast
            }
            else if (args[1].equalsIgnoreCase("block"))
            {
                if (args.length < 3)
                {
                    sender.sendMessage(Colors.color("&c&lUsage &8» &c/" + label + " <target> block <material>"));
                    return true;
                }
                Material m = resolveBlockMaterial(args[2]);
                if (m == null)
                {
                    sender.sendMessage(Colors.color("&c&lError &8» &cUnknown block: " + args[2]));
                    return true;
                }
                mode = CageMode.BLOCK;
                blockMat = m; // stays ALL CAPS via Material.name()
            }
        }

        int[] bounds = cageBounds(target.getLocation());
        if (overlapsExistingCage(target.getWorld(), bounds))
        {
            sender.sendMessage(Colors.color("&c&lError &8» &cThat area overlaps an active cage."));
            return true;
        }
        if (!canBuildCage(sender, target.getWorld(), bounds))
        {
            sender.sendMessage(Colors.color("&c&lError &8» &cYou cannot build here (region protection)."));
            return true;
        }

        CageData data = buildCage(target, mode, blockMat, skullUser);

        if (data == null) {
            sender.sendMessage(Colors.color("&c&lError &8» &cFailed to place cage (world or block issue)."));
            return true;
        }

        // Broadcast shows skull username exactly as typed; blocks stay ALL CAPS (Material.name()).
        String msg = "&c&lServer &8» &c" + sender.getName() + " has caged " + target.getName() + broadcastSuffix(mode, blockMat, skullUser) + ".";
        Colors.broadcast(Colors.color(msg));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        if (args.length == 1)
        {
            String prefix = args[0].toLowerCase();
            List<String> names = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(prefix))
                    .sorted()
                    .collect(Collectors.toList());
            if ("purge".startsWith(prefix))
            {
                names.add("purge");
            }
            return names;
        }
        if (args.length == 2)
        {
            return Stream.of("skull", "block")
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("block"))
        {
            String p = args[2].toUpperCase(Locale.ROOT);
            return Arrays.stream(Material.values())
                    .filter(Material::isBlock)
                    .map(Enum::name)
                    .filter(n -> n.startsWith(p))
                    .limit(20)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() != plugin) return;
        for (UUID id : new HashSet<>(cages.keySet())) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) uncage(p);
            else {
                CageData d = cages.remove(id);
                if (d != null && restoreAndRemoveFailed(d)) {
                    pendingRestores.put(id, toPending(id, d));
                }
            }
        }
        // Keeps world-not-loaded recoveries for the next start.
        flushCageRecords();
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        flushPendingRestores();
    }

    /* ============================== Events ============================== */

    // Keep the player inside the cage without recursion
    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event)
    {
        CageData d = cages.get(event.getPlayer().getUniqueId());
        if (d == null) return;

        // Ignore rotations to reduce churn
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()
                && Objects.equals(event.getFrom().getWorld(), event.getTo().getWorld()))
        {
            return;
        }

        if (!isInside(event.getTo(), d))
        {
            event.setTo(d.center.clone());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event)
    {
        CageData d = cages.get(event.getPlayer().getUniqueId());
        if (d == null) return;

        if (!isInside(event.getTo(), d))
        {
            event.setTo(d.center.clone()); // prevents recursion
        }
    }

    // Make ALL cage blocks unbreakable while caged (for everyone).
    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event)
    {
        CageData data = cageBlockIndex.get(BlockPos.of(event.getBlock()));
        if (data == null) return;

        event.setCancelled(true);
        String message = Colors.color("&c&lError &8» &cThat block is part of an active cage.");
        event.getPlayer().sendMessage(message);
    }

    // Prevent the caged player from placing blocks anywhere inside their cage
    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event)
    {
        UUID placerId = event.getPlayer().getUniqueId();
        CageData d = cages.get(placerId);
        if (d == null) return;

        // Only block if the placement is within the cage bounds (and same world)
        if (event.getBlockPlaced().getWorld().getUID().equals(d.worldId)
                && isInside(event.getBlockPlaced().getLocation(), d))
        {
            event.setCancelled(true);
            String message = Colors.color("&c&lError &8» &cYou cannot place blocks inside your cage.");

            event.getPlayer().sendMessage(message);
        }
    }

    /* ============================== Core ============================== */

    private enum CageMode { GLASS_DEFAULT, BLOCK, SKULL }

    // Broadcast suffix:
    //  - SKULL: use the EXACT casing the initiator typed (e.g., NoTcH -> " in NoTcH")
    //  - BLOCK: keep ALL CAPS via Material.name()
    private String broadcastSuffix(CageMode mode, Material mat, String skullUser)
    {
        return switch (mode)
        {
            case SKULL -> " in " + (skullUser == null ? "UNKNOWN" : skullUser);
            case BLOCK -> " in " + mat.name();
            default -> "";
        };
    }

    // Accept loose tokens and coerce to plausible blocks (e.g., "diamond" -> DIAMOND_BLOCK)
    private Material resolveBlockMaterial(String token)
    {
        String up = token.toUpperCase(Locale.ROOT);

        Material m = Material.matchMaterial(up, true);
        if (m != null && m.isBlock()) return m;

        List<String> suffixes = Arrays.asList("_BLOCK", "_WOOL", "_CONCRETE", "_GLASS", "_PLANKS", "_TERRACOTTA");
        for (String suf : suffixes)
        {
            m = Material.matchMaterial(up.endsWith(suf) ? up : up + suf, true);
            if (m != null && m.isBlock()) return m;
        }

        m = Material.matchMaterial(up);
        if (m != null && m.isBlock()) return m;

        return null;
    }

    private boolean isInside(Location loc, CageData d)
    {
        if (loc == null || loc.getWorld() == null) return false;
        if (!loc.getWorld().getUID().equals(d.worldId)) return false;
        double x = loc.getX(), y = loc.getY(), z = loc.getZ();
        return x >= d.minX + 0.1 && x <= d.maxX + 0.9
            && y >= d.minY       && y <= d.maxY + 0.9
            && z >= d.minZ + 0.1 && z <= d.maxZ + 0.9;
    }

    private CageData buildCage(Player target, CageMode mode, Material blockMat, String skullUser)
    {
        World w = target.getWorld();
        int[] bounds = cageBounds(target.getLocation());
        int minX = bounds[0];
        int minY = bounds[1];
        int minZ = bounds[2];
        int maxX = bounds[3];
        int maxY = bounds[4];
        int maxZ = bounds[5];

        List<OriginalBlock> originals = new ArrayList<>();
        Set<BlockPos> cageBlocks = new HashSet<>();
        List<Block> wallBlocks = new ArrayList<>();

        // Capture every original block *before* touching the world so a crash
        // during the build can still be reverted on the next startup.
        for (int x = minX; x <= maxX; x++)
        {
            for (int y = minY; y <= maxY; y++)
            {
                for (int z = minZ; z <= maxZ; z++)
                {
                    boolean isWall = (x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ);
                    if (!isWall) continue;
                    if (y < w.getMinHeight() || y >= w.getMaxHeight()) continue;

                    Block b = w.getBlockAt(x, y, z);
                    originals.add(new OriginalBlock(b.getLocation(), b.getState()));
                    cageBlocks.add(BlockPos.of(b));
                    wallBlocks.add(b);
                }
            }
        }

        if (wallBlocks.isEmpty()) return null;

        // Center player inside the cage
        Location center = new Location(w, (minX + maxX) / 2.0 + 0.5, minY + 1.0, (minZ + maxZ) / 2.0 + 0.5);

        CageData data = new CageData(
                originals, cageBlocks, w.getUID(),
                minX, minY, minZ, maxX, maxY, maxZ,
                center, mode, blockMat, skullUser, target.getName()
        );
        cages.put(target.getUniqueId(), data);
        for (BlockPos pos : cageBlocks)
        {
            cageBlockIndex.put(pos, data);
        }
        // Persist first: a crash mid-build then leaves a record that reverts to
        // the original terrain on the next start.
        saveCageRecords();

        // Force Survival while caged
        target.setGameMode(GameMode.SURVIVAL);

        for (Block b : wallBlocks)
        {
            if (mode == CageMode.SKULL)
            {
                b.setType(Material.PLAYER_HEAD, false);
            }
            else
            {
                b.setType(blockMat, false);
            }
        }

        target.teleport(center);

        // If skull mode, fetch textures via SkinsRestorer (offline/cracked friendly)
        if (mode == CageMode.SKULL && skullUser != null && !skullUser.isBlank())
        {
            fetchAndApplySkullProfileAsync(target.getUniqueId(), skullUser);
        }

        return data;
    }

    /* ============================== Skull profile via SkinsRestorer ============================== */

    @SuppressWarnings("UnstableApiUsage") // ResolvableProfile is the only API that can carry the cached texture property.
    private void fetchAndApplySkullProfileAsync(UUID cagedPlayerId, String username)
    {
        if (!Bukkit.getPluginManager().isPluginEnabled("SkinsRestorer"))
        {
            plugin.getLogger().warning("[/cage skull] SkinsRestorer is not enabled; cannot texture skull cage.");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try
            {
                SkinsRestorer sr = SkinsRestorerProvider.get();

                // Get the skin identifier SkinsRestorer would apply for this player/name
                Optional<SkinIdentifier> idOpt =
                        sr.getPlayerStorage().getSkinIdForPlayer(cagedPlayerId, username, false);
                if (idOpt.isEmpty()) return;

                Optional<SkinProperty> propOpt = sr.getSkinStorage().getSkinDataByIdentifier(idOpt.get());
                if (propOpt.isEmpty()) return;

                SkinProperty prop = propOpt.get();
                String value = prop.getValue();
                String signature = prop.getSignature();

                // Apply on the main thread
                Bukkit.getScheduler().runTask(plugin, () -> {
                    CageData d = cages.get(cagedPlayerId);
                    if (d == null) return;
                    World w = Bukkit.getWorld(d.worldId);
                    if (w == null) return;

                    PlayerProfile prof = Bukkit.createProfile(UUID.randomUUID(), username);
                    if (!signature.isBlank())
                    {
                        prof.setProperty(new ProfileProperty("textures", value, signature));
                    }
                    else
                    {
                        prof.setProperty(new ProfileProperty("textures", value));
                    }
                    d.profile = prof;

                    for (int x = d.minX; x <= d.maxX; x++)
                    {
                        for (int y = d.minY; y <= d.maxY; y++)
                        {
                            for (int z = d.minZ; z <= d.maxZ; z++)
                            {
                                boolean isWall = (x == d.minX || x == d.maxX || y == d.minY || y == d.maxY || z == d.minZ || z == d.maxZ);
                                if (!isWall) continue;
                                Block b = w.getBlockAt(x, y, z);
                                if (b.getType() == Material.PLAYER_HEAD)
                                {
                                    BlockState state = b.getState();
                                    if (state instanceof Skull skull)
                                    {
                                        skull.setProfile(io.papermc.paper.datacomponent.item.ResolvableProfile.resolvableProfile(prof));
                                        skull.update(true, false);
                                    }
                                }
                            }
                        }
                    }
                });
            }
            catch (Throwable t)
            {
                plugin.getLogger().warning("[/cage skull] SR lookup failed for '" + username + "': " + t.getMessage());
            }
        });
    }

    /* ============================== Apply / Unapply ============================== */

    private void uncage(Player target)
    {
        UUID id = target.getUniqueId();
        CageData d = cages.remove(id);
        if (d == null) return;
        if (restoreAndRemoveFailed(d))
        {
            pendingRestores.put(id, toPending(id, d));
        }
        saveCageRecords();
    }

    /**
     * Puts every captured block back. Returns {@code true} when the cage's
     * world is not loaded yet so the caller can persist a deferred restore.
     */
    private boolean restoreAndRemoveFailed(CageData d)
    {
        unindexCage(d);

        World w = Bukkit.getWorld(d.worldId);
        if (w == null)
        {
            plugin.getLogger().warning("[/cage] World " + d.worldId + " is not loaded; deferring cage restoration.");
            return true;
        }

        // Restore in reverse build order without mutating the stored list.
        for (int i = d.originals.size() - 1; i >= 0; i--)
        {
            OriginalBlock ob = d.originals.get(i);
            try
            {
                // A full BlockState snapshot also restores container contents,
                // sign text, and skull profiles.
                if (ob.state() != null && ob.state().getWorld() == w)
                {
                    ob.state().update(true, false);
                }
                else
                {
                    w.getBlockAt(ob.location()).setBlockData(ob.blockData(), false);
                }
            }
            catch (RuntimeException ex)
            {
                plugin.getLogger().warning("[/cage] Could not restore block at "
                        + ob.location().getBlockX() + "," + ob.location().getBlockY()
                        + "," + ob.location().getBlockZ() + ": " + ex.getMessage());
            }
        }
        return false;
    }

    /* ============================== Persistence ============================== */

    /** Records all active cages and deferred restores so a crash cannot leave terrain caged forever. */
    private void saveCageRecords()
    {
        pendingCageSave.set(new CageSave(snapshotCageRecords(), ++cageSaveGeneration));
        scheduleCageSave();
    }

    /**
     * Builds the cage YAML on the caller (main) thread. Returns {@code null} when
     * there is nothing to persist, which means the old file should be removed.
     */
    private YamlConfiguration snapshotCageRecords()
    {
        if (cages.isEmpty() && pendingRestores.isEmpty()) return null;

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().setHeader(List.of(
                "Crash-recovery records for /cage. Do not edit by hand.",
                "They are replayed (terrain restored) on the next startup."
        ));

        for (Map.Entry<UUID, CageData> e : cages.entrySet())
        {
            CageData d = e.getValue();
            writeCageRecord(yaml, "cages." + e.getKey(), d.playerName,
                    d.worldId, worldName(d.worldId), savedBlocks(d));
        }
        for (Map.Entry<UUID, PendingRestore> e : pendingRestores.entrySet())
        {
            PendingRestore p = e.getValue();
            writeCageRecord(yaml, "pending." + e.getKey(), p.playerName(),
                    p.worldId(), p.worldName(), p.blocks());
        }
        return yaml;
    }

    private void scheduleCageSave()
    {
        if (!cageSaveScheduled.compareAndSet(false, true)) return;
        try
        {
            cageSaveTask = Bukkit.getScheduler().runTaskLaterAsynchronously(
                    plugin, this::drainCageSave, 20L);
        }
        catch (RuntimeException ex)
        {
            cageSaveScheduled.set(false);
            plugin.getLogger().warning("[/cage] Could not schedule save: " + ex.getMessage());
        }
    }

    private void drainCageSave()
    {
        cageSaveTask = null;
        try
        {
            synchronized (cageWriteLock)
            {
                CageSave save = pendingCageSave.getAndSet(null);
                if (save != null && save.generation() == cageSaveGeneration)
                {
                    writeCageSave(save.yaml());
                }
            }
        }
        finally
        {
            cageSaveScheduled.set(false);
            if (pendingCageSave.get() != null) scheduleCageSave();
        }
    }

    /** Flushes the newest snapshot synchronously; used when the plugin disables. */
    private void flushCageRecords()
    {
        BukkitTask task = cageSaveTask;
        cageSaveTask = null;
        if (task != null) task.cancel();
        cageSaveScheduled.set(false);
        pendingCageSave.set(null);
        synchronized (cageWriteLock)
        {
            ++cageSaveGeneration;
            writeCageSave(snapshotCageRecords());
        }
    }

    private void writeCageSave(YamlConfiguration yaml)
    {
        File tmp = null;
        try
        {
            File parent = cageFile.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs())
            {
                plugin.getLogger().warning("[/cage] Could not create data folder " + parent + ".");
                return;
            }

            if (yaml == null)
            {
                Files.deleteIfExists(cageFile.toPath());
                return;
            }

            tmp = File.createTempFile("cages", ".tmp", parent);
            yaml.save(tmp);
            try
            {
                Files.move(tmp.toPath(), cageFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            }
            catch (AtomicMoveNotSupportedException unsupported)
            {
                Files.move(tmp.toPath(), cageFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException ex)
        {
            plugin.getLogger().warning("[/cage] Could not save " + cageFile.getName() + ": " + ex.getMessage());
        }
        finally
        {
            if (tmp != null && tmp.exists() && !tmp.delete())
            {
                plugin.getLogger().warning("[/cage] Could not delete temporary file " + tmp);
            }
        }
    }

    private record CageSave(YamlConfiguration yaml, long generation) {
    }

    private void writeCageRecord(YamlConfiguration yaml, String path, String playerName,
                                 UUID worldId, String worldName, List<SavedBlock> blocks)
    {
        ConfigurationSection section = yaml.createSection(path);
        section.set("player", playerName == null ? "" : playerName);
        section.set("world", worldId.toString());
        section.set("world-name", worldName == null ? "" : worldName);

        ConfigurationSection blockSection = section.createSection("blocks");
        for (SavedBlock block : blocks)
        {
            blockSection.set(block.x() + "," + block.y() + "," + block.z(), block.data());
        }
    }

    /** Reads recovery records written before an unclean shutdown. */
    private void loadCageRecords()
    {
        if (!cageFile.isFile()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(cageFile);
        int before = pendingRestores.size();
        readCageRecords(yaml.getConfigurationSection("cages"));
        readCageRecords(yaml.getConfigurationSection("pending"));

        int found = pendingRestores.size() - before;
        if (found > 0)
        {
            plugin.getLogger().info("[/cage] Found " + found
                    + " cage area(s) to restore from a previous unclean shutdown.");
        }
        flushPendingRestores();
    }

    private void readCageRecords(ConfigurationSection section)
    {
        if (section == null) return;

        for (String key : section.getKeys(false))
        {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) continue;

            UUID playerId;
            try
            {
                playerId = UUID.fromString(key);
            }
            catch (IllegalArgumentException ex)
            {
                continue;
            }

            UUID worldId;
            try
            {
                worldId = UUID.fromString(entry.getString("world", ""));
            }
            catch (IllegalArgumentException ex)
            {
                plugin.getLogger().warning("[/cage] Skipping recovery record with an invalid world: " + key);
                continue;
            }

            List<SavedBlock> blocks = readSavedBlocks(entry.getConfigurationSection("blocks"));
            if (blocks.isEmpty()) continue;

            pendingRestores.put(playerId, new PendingRestore(
                    playerId,
                    stringOrEmpty(entry, "player"),
                    worldId,
                    stringOrEmpty(entry, "world-name"),
                    blocks
            ));
        }
    }

    private static String stringOrEmpty(ConfigurationSection section, String key) {
        String value = section.getString(key);
        return value == null ? "" : value;
    }

    private List<SavedBlock> readSavedBlocks(ConfigurationSection section)
    {
        if (section == null) return List.of();

        List<SavedBlock> blocks = new ArrayList<>();
        for (String key : section.getKeys(false))
        {
            String[] parts = key.split(",");
            if (parts.length != 3) continue;
            try
            {
                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int z = Integer.parseInt(parts[2].trim());
                blocks.add(new SavedBlock(x, y, z, section.getString(key, "minecraft:air")));
            }
            catch (NumberFormatException ignored)
            {
                // Ignore malformed coordinates instead of failing the whole restore.
            }
        }
        return blocks;
    }

    /** Restores deferred areas whose worlds are now loaded. */
    private void flushPendingRestores()
    {
        if (pendingRestores.isEmpty()) return;

        int restored = 0;
        Iterator<Map.Entry<UUID, PendingRestore>> iterator = pendingRestores.entrySet().iterator();
        while (iterator.hasNext())
        {
            PendingRestore pending = iterator.next().getValue();
            if (restoreBlocks(pending))
            {
                iterator.remove();
                restored++;
                plugin.getLogger().info("[/cage] Restored cage terrain for "
                        + (pending.playerName().isBlank() ? pending.playerId().toString() : pending.playerName())
                        + " in " + (pending.worldName().isBlank() ? pending.worldId().toString() : pending.worldName()));
            }
        }
        if (restored > 0) saveCageRecords();
    }

    private boolean restoreBlocks(PendingRestore pending)
    {
        World world = Bukkit.getWorld(pending.worldId());
        if (world == null && !pending.worldName().isBlank())
        {
            // The world UID changes if a world is regenerated; fall back to its name.
            world = Bukkit.getWorld(pending.worldName());
        }
        if (world == null) return false;

        for (SavedBlock saved : pending.blocks())
        {
            try
            {
                world.getBlockAt(saved.x(), saved.y(), saved.z())
                        .setBlockData(Bukkit.createBlockData(saved.data()), false);
            }
            catch (RuntimeException ex)
            {
                plugin.getLogger().warning("[/cage] Skipping invalid block data '" + saved.data()
                        + "' at " + saved.x() + "," + saved.y() + "," + saved.z());
            }
        }
        return true;
    }

    private PendingRestore toPending(UUID playerId, CageData data)
    {
        return new PendingRestore(playerId, data.playerName, data.worldId,
                worldName(data.worldId), savedBlocks(data));
    }

    private void unindexCage(CageData data)
    {
        for (BlockPos pos : data.blockSet)
        {
            cageBlockIndex.remove(pos, data);
        }
    }

    private List<SavedBlock> savedBlocks(CageData data)
    {
        List<SavedBlock> blocks = new ArrayList<>(data.originals.size());
        for (OriginalBlock original : data.originals)
        {
            Location loc = original.location();
            blocks.add(new SavedBlock(
                    loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                    original.blockData().getAsString()
            ));
        }
        return blocks;
    }

    private String worldName(UUID worldId)
    {
        World world = Bukkit.getWorld(worldId);
        return world == null ? "" : world.getName();
    }

    /* ============================== Region safety ============================== */

    private static int[] cageBounds(Location location)
    {
        int minX = location.getBlockX() - 2;
        int minY = location.getBlockY() - 1; // floor one block below the player
        int minZ = location.getBlockZ() - 2;
        return new int[]{minX, minY, minZ, minX + 4, minY + 4, minZ + 4};
    }

    private boolean overlapsExistingCage(World world, int[] bounds)
    {
        UUID worldId = world.getUID();
        for (CageData d : cages.values())
        {
            if (!d.worldId.equals(worldId)) continue;
            if (bounds[0] <= d.maxX && bounds[3] >= d.minX
                    && bounds[1] <= d.maxY && bounds[4] >= d.minY
                    && bounds[2] <= d.maxZ && bounds[5] >= d.minZ)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Optional WorldGuard check. The plugin is not a compile dependency, so the
     * legacy {@code WorldGuardPlugin#canBuild(Player, Block)} method is resolved
     * reflectively. If the API shape is unknown the check fails open rather than
     * breaking the command.
     */
    private boolean canBuildCage(CommandSender sender, World world, int[] bounds)
    {
        if (!(sender instanceof Player player)) return true;

        Method canBuild = worldGuardCanBuild();
        Plugin worldGuard = worldGuardPlugin;
        if (canBuild == null || worldGuard == null) return true;

        for (int x = bounds[0]; x <= bounds[3]; x++)
        {
            for (int y = bounds[1]; y <= bounds[4]; y++)
            {
                if (y < world.getMinHeight() || y >= world.getMaxHeight()) continue;

                for (int z = bounds[2]; z <= bounds[5]; z++)
                {
                    boolean wall = x == bounds[0] || x == bounds[3]
                            || y == bounds[1] || y == bounds[4]
                            || z == bounds[2] || z == bounds[5];
                    if (!wall) continue;

                    try
                    {
                        Object result = canBuild.invoke(worldGuard, player, world.getBlockAt(x, y, z));
                        if (result instanceof Boolean allowed && !allowed) return false;
                    }
                    catch (ReflectiveOperationException | RuntimeException ex)
                    {
                        plugin.getLogger().warning("[/cage] WorldGuard build check failed: " + ex.getMessage());
                        return true;
                    }
                }
            }
        }
        return true;
    }

    private Method worldGuardCanBuild()
    {
        if (worldGuardLookupDone) return worldGuardCanBuild;

        synchronized (this)
        {
            if (worldGuardLookupDone) return worldGuardCanBuild;
            worldGuardLookupDone = true;

            Plugin wg = Bukkit.getPluginManager().getPlugin("WorldGuard");
            if (wg == null || !wg.isEnabled()) return null;

            try
            {
                worldGuardCanBuild = wg.getClass().getMethod("canBuild", Player.class, Block.class);
                worldGuardPlugin = wg;
            }
            catch (NoSuchMethodException ex)
            {
                plugin.getLogger().info("[/cage] WorldGuard build check is unavailable; region protection is not enforced for /cage.");
            }
            return worldGuardCanBuild;
        }
    }

    /* ============================== Data & utils ============================== */

    private record SavedBlock(int x, int y, int z, String data) {
    }

    private record PendingRestore(UUID playerId, String playerName, UUID worldId, String worldName,
                                  List<SavedBlock> blocks) {
    }

    private record OriginalBlock(Location location, BlockData blockData, BlockState state) {
            private OriginalBlock(Location location, BlockState state) {
                this(location.clone(), state.getBlockData().clone(), state);
            }
        }

    // Compact immutable block position key (world + xyz)
        private record BlockPos(UUID world, int x, int y, int z) {
        static BlockPos of(Block b) {
                return new BlockPos(b.getWorld().getUID(), b.getX(), b.getY(), b.getZ());
            }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof BlockPos(UUID world1, int x1, int y1, int z1))) return false;
                return x == x1 && y == y1 && z == z1 && Objects.equals(world, world1);
            }
    }

    private static class CageData
    {
        final List<OriginalBlock> originals;
        final Set<BlockPos> blockSet;
        final UUID worldId;
        final int minX, minY, minZ, maxX, maxY, maxZ;
        final Location center;

        // For rebuilds
        final CageMode mode;
        final Material blockMat;
        final String skullUser;
        final String playerName;
        PlayerProfile profile; // may be null until textures fetched

        CageData(List<OriginalBlock> originals, Set<BlockPos> blockSet, UUID worldId,
                 int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                 Location center, CageMode mode, Material blockMat, String skullUser, String playerName)
        {
            this.originals = originals;
            this.blockSet = blockSet;
            this.worldId = worldId;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            this.center = center.clone();
            this.mode = mode;
            this.blockMat = blockMat;
            this.skullUser = skullUser;
            this.playerName = playerName;
        }
    }
}
