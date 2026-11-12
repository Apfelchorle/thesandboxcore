package org.thesandbox.core;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.java.JavaPlugin;
import org.thesandbox.core.commands.staff.UpdateLocalCommand;
import org.thesandbox.core.data.DataManager;
import org.thesandbox.core.data.PlayerDataListener;
import org.thesandbox.core.data.database.BalanceSqlStore;
import org.thesandbox.core.data.database.Database;
import org.thesandbox.core.data.database.LegacySqlStore;
import org.thesandbox.core.services.chat.ManageChatService;
import org.thesandbox.core.services.chat.ShushService;
import org.thesandbox.core.services.chat.StaffChatManager;
import org.thesandbox.core.services.discord.DiscordBridge;
import org.thesandbox.core.services.discord.media.AnimatedMapService;
import org.thesandbox.core.services.display.RankScoreboardManager;
import org.thesandbox.core.services.economy.BalanceManager;
import org.thesandbox.core.services.entities.RideableDragonEntity;
import org.thesandbox.core.services.guilds.GuildManager;
import org.thesandbox.core.services.items.FloatBoatItem;
import org.thesandbox.core.services.items.util.Item;
import org.thesandbox.core.services.items.util.ItemKeys;
import org.thesandbox.core.services.listeners.*;
import org.thesandbox.core.services.login.LoginMessages;
import org.thesandbox.core.services.login.LoginService;
import org.thesandbox.core.services.moderation.CommandBlockManager;
import org.thesandbox.core.services.moderation.CommandSpyManager;
import org.thesandbox.core.services.moderation.PotionSpyService;
import org.thesandbox.core.services.tags.TagService;
import org.thesandbox.core.services.update.UpdateTarget;
import org.thesandbox.core.services.world.AutoClearService;
import org.thesandbox.core.services.world.AutoTpService;
import org.thesandbox.core.services.world.EntityWiper;
import org.thesandbox.core.util.FunctionsHolder;
import org.thesandbox.core.util.Utils;
import org.thesandbox.core.util.config.PluginConfigManager;
import org.thesandbox.core.util.registry.CommandAutoRegistrar;
import org.thesandbox.core.util.registry.ItemAutoRegistrar;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TheSandboxCore extends JavaPlugin {

    private static TheSandboxCore instance;

    public static TheSandboxCore getInstance() {
        return instance;
    }

    // Core
    private DataManager dataManager;
    private PlayerDataListener dataListener;
    private LoginMessages loginMessages;
    private EntityWiper entityWiper;

    // SQL
    private Database database;
    private LegacySqlStore legacySqlStore;
    private BalanceSqlStore balanceSqlStore;

    // Services & Managers
    private CommandBlockManager commandBlockManager;
    private RankScoreboardManager rankScoreboardManager;
    private StaffChatManager staffChatManager;
    private CommandSpyManager commandSpyManager;
    private DiscordBridge discord;
    private LoginService loginService;
    private TagService tagService;
    private GuildManager guildManager;
    private LiteBansWarningListener liteBansWarningListener;
    private UpdateTarget updateTarget;
    private UpdateLocalCommand updateLocalCommand;
    private GenericListener genericListener;
    private ChatFilterEngine chatFilterEngine;
    private PotionSpyService potionSpyService;
    private ShushService shushService;
    private List<Item> registeredItems = new ArrayList<>();
    private PluginConfigManager pluginConfigManager;
    private BalanceManager balanceManager;
    private FunctionsHolder functionsHolder;
    private AutoClearService autoClearService;
    private AutoTpService autoTpService;
    private ManageChatService manageChatService;
    private ItemKeys itemKeys;
    private AnimatedMapService animatedMapService;
    private RideableDragonEntity rideableDragonEntity;

    // ==================== Getters ====================

    public RideableDragonEntity getRideableDragonEntity() {
        return rideableDragonEntity;
    }

    public BalanceManager getBalanceManager() {
        return balanceManager;
    }

    public DiscordBridge getDiscord() {
        return this.discord;
    }

    public boolean isDiscordChatBridgeEnabled() {
        return getConfig().getBoolean("discord.chat-bridge.enabled", true);
    }

    public PotionSpyService getPotionSpyService() {
        return potionSpyService;
    }

    public List<Item> getRegisteredItems() {
        return this.registeredItems;
    }

    public File getPluginFile() {
        return getFile();
    }

    public PluginConfigManager getPluginConfigManager() {
        return pluginConfigManager;
    }

    public ShushService getShushService() {
        return shushService;
    }

    public GuildManager getGuildManager() {
        return guildManager;
    }

    public LoginService getLoginService() {
        return loginService;
    }

    public CommandBlockManager getCommandBlockManager() {
        return commandBlockManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public LoginMessages getLoginMessages() {
        return loginMessages;
    }

    public TagService getTagService() {
        return tagService;
    }

    public UpdateLocalCommand getUpdateLocalCommand() {
        return updateLocalCommand;
    }

    public GenericListener getGenericListener() {
        return genericListener;
    }

    public ChatFilterEngine getChatFilterEngine() {
        return chatFilterEngine;
    }

    public RankScoreboardManager getRankScoreboardManager() {
        return rankScoreboardManager;
    }

    public StaffChatManager getStaffChatManager() {
        return staffChatManager;
    }

    public PlayerDataListener getDataListener() {
        return dataListener;
    }

    public CommandSpyManager getCommandSpyManager() {
        return commandSpyManager;
    }

    public EntityWiper getEntityWiper() {
        return entityWiper;
    }

    public FunctionsHolder getFunctionsHolder() {
        return functionsHolder;
    }

    // ==================== Lifecycle Hooks ====================

    @Override
    public void onLoad() {
        instance = this;
        Utils.sendASCII("Loading TheSandboxCore ...");
        getLogger().info("loading.. :)");

        try {
            PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
            PacketEvents.getAPI().load();
        } catch (Throwable t) {
            Utils.dump(t, " ): PacketEvents Failed to load!");
        }
        getLogger().info("loaded! ;)");
    }

    @Override
    public void onEnable() {
        startup();
        if (isEnabled()) {
            getLogger().info("TheSandboxCore enabled!");
        }
    }

    @Override
    public void onDisable() {
        cleanup();
        getLogger().info("TheSandboxCore disabled!");
    }

    // ==================== Startup Orchestration ====================

    private void startup() {
        Utils.sendASCII("Starting TheSandboxCore ...");
        getLogger().info("starting up :)");

        Utils.setPlugin(this);
        saveDefaultConfig();
        org.thesandbox.core.commands.MessageService.load(this);

        try {
            initPacketEvents();
            initDatabase();
            initServices();
            if (!verifyInstances()) {
                return;
            }
            registerListeners();
            registerCommands();

            rankScoreboardManager.setupRankScoreboardTeams();
            rankScoreboardManager.refreshAllRankScoreboardTeams();
        } catch (Throwable t) {
            // Don't leave a half-initialised plugin running (listeners without services, etc.).
            Utils.dump(t, "TheSandboxCore failed to start - disabling");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("Discord Chat Bridge : " + DiscordChatBridgeStatus());
        getLogger().info("startup complete! ;)");
    }

    private void initPacketEvents() {
        try {
            PacketEvents.getAPI().init();
            getLogger().info("PacketEvents initialized successfully");
        } catch (Throwable t) {
            Utils.dump(t, "PacketEvents failed to initialize");
        }
    }

    private void initDatabase() {
        // One pool for every SQL-backed feature: balances, guilds, tags, spy/staff chat.
        this.database = new Database(this);
        this.database.setup();

        this.legacySqlStore = new LegacySqlStore(this, database);
        this.legacySqlStore.setup();

        this.balanceSqlStore = new BalanceSqlStore(this, database);
        this.balanceSqlStore.setup();
    }

    private void initServices() {
        this.functionsHolder = new FunctionsHolder();
        this.pluginConfigManager = new PluginConfigManager(this);

        this.dataManager = new DataManager(this);
        this.dataListener = new PlayerDataListener(this, dataManager);
        this.loginMessages = new LoginMessages(dataListener, this);

        this.balanceManager = new BalanceManager(this, balanceSqlStore.getDelegatingDataSource());
        this.balanceManager.init();

        this.itemKeys = new ItemKeys(this);
        this.rideableDragonEntity = new RideableDragonEntity(this);
        this.registeredItems = ItemAutoRegistrar.registerAll(
            this,
            itemKeys,
            dataListener,
            loginMessages,
            pluginConfigManager,
            rideableDragonEntity
        );

        this.loginService = new LoginService(this);
        this.loginService.init();

        this.rankScoreboardManager = new RankScoreboardManager(loginService);
        this.commandBlockManager = new CommandBlockManager();
        this.commandSpyManager = new CommandSpyManager(this, () ->
            legacySqlStore.getDataSource()
        );

        this.manageChatService = new ManageChatService(this);
        this.guildManager = new GuildManager(this, database.getDelegatingDataSource());
        this.chatFilterEngine = new ChatFilterEngine(pluginConfigManager, this);

        this.tagService = new TagService(this, database.getDelegatingDataSource());
        this.tagService.init();

        // Discord must be initialized before staffChatManager references it
        this.discord = new DiscordBridge(
            this,
            this.chatFilterEngine,
            this.dataListener
        );
        this.discord.start();

        // Animated map service for GIF maps
        this.animatedMapService = new AnimatedMapService(this);
        this.discord.setAnimatedMapService(this.animatedMapService);

        // Services that depend on discord or shushService
        this.potionSpyService = new PotionSpyService(
            this,
            legacySqlStore.getDelegatingDataSource()
        );
        this.shushService = new ShushService(
            this,
            legacySqlStore.getDelegatingDataSource()
        );
        this.genericListener = new GenericListener(pluginConfigManager);

        // Now safe to initialize staffChatManager (discord and shushService are ready)
        this.staffChatManager = new StaffChatManager(
                this,
                () -> legacySqlStore.getDataSource(),
                () -> shushService,
                () -> discord
        );

        this.updateLocalCommand = new UpdateLocalCommand(this, discord);
        this.autoClearService = new AutoClearService();
        this.autoTpService = new AutoTpService(this);

        // Was declared but never created, so automatic wiping / item despawn never ran.
        this.entityWiper = new EntityWiper(this);
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();

        pm.registerEvents(dataListener, this);
        pm.registerEvents(loginMessages, this);
        pm.registerEvents(entityWiper, this);
        entityWiper.startFromConfig();
        pm.registerEvents(
            new ChatMentionFormatListener(this, chatFilterEngine),
            this
        );
        pm.registerEvents(new ManageChatListener(manageChatService), this);
        pm.registerEvents(new AutoTpListener(this, autoTpService), this);
        new org.thesandbox.core.services.tags.TagCleanupListener(
            this,
            tagService,
            guildManager
        ).register();

        pm.registerEvents(new AutoClearListener(autoClearService), this);
        pm.registerEvents(potionSpyService, this);
        pm.registerEvents(new ShushListener(this), this);

        this.liteBansWarningListener = new LiteBansWarningListener(this);
        this.liteBansWarningListener.register();

        pm.registerEvents(genericListener, this);
        pm.registerEvents(
            new JoinLeaveListener(
                this,
                dataListener,
                loginMessages,
                rankScoreboardManager,
                shushService
            ),
            this
        );
        pm.registerEvents(
            new CommandPreListener(
                this,
                commandBlockManager,
                commandSpyManager,
                () -> shushService,
                () -> discord
            ),
            this
        );
        pm.registerEvents(
            new StaffChatListener(
                this,
                staffChatManager,
                commandSpyManager,
                legacySqlStore
            ),
            this
        );
    }

    private void registerCommands() {
        List<Object> commandServices = new ArrayList<>(
            List.of(
                tagService,
                loginService,
                autoClearService,
                autoTpService,
                manageChatService,
                guildManager,
                potionSpyService,
                this.shushService,
                this.discord,
                dataListener,
                    balanceManager,
                itemKeys,
                loginMessages,
                pluginConfigManager,
                updateLocalCommand,
                entityWiper,
                genericListener,
                commandBlockManager,
                rankScoreboardManager,
                staffChatManager,
                commandSpyManager,
                rideableDragonEntity
            )
        );

        commandServices.addAll(this.registeredItems);
        CommandAutoRegistrar.registerAll(this, commandServices.toArray());
    }

    /**
     * Checks that the critical services exist. Runs right after initServices() (it used to run last,
     * and used Map.of which throws NPE on a null value - so the "is it null" check could never report anything).
     */
    private boolean verifyInstances() {
        Map<String, Object> criticalServices = new LinkedHashMap<>();
        criticalServices.put("PluginConfigManager", pluginConfigManager);
        criticalServices.put("DataManager", dataManager);
        criticalServices.put("PlayerDataListener", dataListener);
        criticalServices.put("BalanceManager", balanceManager);
        criticalServices.put("Database", database);
        criticalServices.put("LegacySqlStore", legacySqlStore);
        criticalServices.put("BalanceSqlStore", balanceSqlStore);
        criticalServices.put("LoginService", loginService);
        criticalServices.put("FunctionsHolder", functionsHolder);
        criticalServices.put("EntityWiper", entityWiper);

        for (Map.Entry<String, Object> entry : criticalServices.entrySet()) {
            if (entry.getValue() == null) {
                getLogger().severe(
                    "CRITICAL ERROR: " + entry.getKey() + " is NULL after startup!"
                );
                getServer().getPluginManager().disablePlugin(this);
                return false;
            }
        }
        return true;
    }

    // ==================== Teardown / Cleanup ====================

    private void cleanup() {
        getLogger().info("[cleaning up :)]");

        // Persist data FIRST: stopping Discord can block for a long time and must not risk losing saves.
        safely("save player data", () -> {
            if (dataListener != null) dataListener.saveAll();
        });
        safely("save balances", () -> {
            if (balanceManager != null) balanceManager.flush();
        });
        safely("save guilds", () -> {
            if (guildManager != null) {
                guildManager.flush();
                guildManager.shutdown();
            }
        });

        safely("stop entity wiper", () -> {
            if (entityWiper != null) entityWiper.stop();
        });
        safely("unregister LiteBans listener", () -> {
            if (liteBansWarningListener != null) liteBansWarningListener.unregister();
        });
        safely("stop discord", () -> {
            if (discord != null) discord.stop();
        });
        safely("stop animated map service", () -> {
            if (animatedMapService != null) animatedMapService.close();
        });
        safely("login service shutdown", () -> {
            if (loginService != null) loginService.shutdown();
        });

        safely("clean up items", () ->
            registeredItems
                .stream()
                .filter(item -> item instanceof FloatBoatItem)
                .map(item -> (FloatBoatItem) item)
                .findFirst()
                .ifPresent(FloatBoatItem::cleanup)
        );

        safely("clean up rideable dragon entity", () -> {
            if (rideableDragonEntity != null) rideableDragonEntity.cleanup();
        });

        safely("terminate PacketEvents", () -> {
            if (PacketEvents.getAPI() != null) PacketEvents.getAPI().terminate();
        });

        // Close database connections last
        safely("close balances database", () -> {
            if (balanceSqlStore != null) balanceSqlStore.close();
        });
        safely("close database", () -> {
            if (legacySqlStore != null) legacySqlStore.close();
        });
        safely("close shared database pool", () -> {
            if (database != null) database.close();
        });

        getLogger().info("clean up complete! have a nice day! ;)");
    }

    /** One failing step must not skip the rest of the shutdown. */
    private void safely(String what, Runnable step) {
        try {
            step.run();
        } catch (Throwable t) {
            getLogger().warning("Shutdown step '" + what + "' failed: " + t);
        }
    }

    // ==================== Discord / Config Helpers ====================

    public void reloadAndReconnect() {
        reloadConfig();
        org.thesandbox.core.commands.MessageService.reload();

        // Persist pending async writes before the old pool is closed.
        if (balanceManager != null) balanceManager.flush();
        if (guildManager != null) guildManager.flush();

        database.setup(); // swaps the shared pool; services hold the delegating DataSource
        legacySqlStore.setup();
        balanceSqlStore.setup();
        if (entityWiper != null) {
            entityWiper.startFromConfig();
        }
        if (discord != null) {
            discord.stop();
            discord.start();
        }
    }

    public boolean DiscordChatBridgeStatus() {
        return isDiscordChatBridgeEnabled();
    }

    public boolean setDiscordChatBridgeEnabled(boolean enabled) {
        boolean current = isDiscordChatBridgeEnabled();
        if (current == enabled) return false;
        getConfig().set("discord.chat-bridge.enabled", enabled);
        saveConfig();
        if (discord != null) {
            discord.applyChatBridgePermissionState();
        }
        return true;
    }
}
