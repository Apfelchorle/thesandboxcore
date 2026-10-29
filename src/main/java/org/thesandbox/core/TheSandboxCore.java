package org.thesandbox.core;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.java.JavaPlugin;
import org.thesandbox.core.commands.Admin.UpdateLocalCommand;
import org.thesandbox.core.commands.CommandCenter;
import org.thesandbox.core.commands.CommandManager;
import org.thesandbox.core.fun.LoginMessages;
import org.thesandbox.core.fun.Utils;
import org.thesandbox.core.fun.items.FloatBoatItem;
import org.thesandbox.core.fun.items.itemUTILS.Item;
import org.thesandbox.core.fun.items.itemUTILS.ItemKeys;
import org.thesandbox.core.guilds.GuildManager;
import org.thesandbox.core.listeners.*;
import org.thesandbox.core.login.LoginService;
import org.thesandbox.core.managers.*;
import org.thesandbox.core.services.*;
import org.thesandbox.core.tags.TagService;
import org.thesandbox.core.util.*;
import org.thesandbox.core.util.Internals.FunctionsHolder;

import java.io.File;
import java.util.ArrayList;
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
    private LegacySqlStore legacySqlStore;

    // Services & Managers
    private CommandBlockManager commandBlockManager;
    private RankScoreboardManager rankScoreboardManager;
    private StaffChatManager staffChatManager;
    private CommandSpyManager commandSpyManager;
    private DiscordBridge discord;
    private CommandManager commandManager;
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
    private CommandCenter commandCenter;
    private AutoClearService autoClearService;
    private AutoTpService autoTpService;
    private ManageChatService manageChatService;
    private ItemKeys itemKeys;

    // ==================== Getters ====================

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
        getLogger().info("TheSandboxCore enabled!");
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

        initPacketEvents();
        initDatabase();
        initServices();
        registerListeners();
        registerCommands();
        verifyInstances();

        rankScoreboardManager.setupRankScoreboardTeams();
        rankScoreboardManager.refreshAllRankScoreboardTeams();

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
        this.legacySqlStore = new LegacySqlStore(this);
        this.legacySqlStore.setup();
    }

    private void initServices() {
        this.functionsHolder = new FunctionsHolder();
        this.pluginConfigManager = new PluginConfigManager(this);

        this.dataManager = new DataManager(this);
        this.dataListener = new PlayerDataListener(this, dataManager);
        this.loginMessages = new LoginMessages(dataListener, this);

        this.balanceManager = new BalanceManager();
        this.balanceManager.init();

        this.itemKeys = new ItemKeys(this);
        this.registeredItems = ItemAutoRegistrar.registerAll(
            this,
            itemKeys,
            dataListener,
            loginMessages,
            pluginConfigManager
        );

        this.loginService = new LoginService(this);
        this.loginService.init();

        this.rankScoreboardManager = new RankScoreboardManager(loginService);
        this.commandBlockManager = new CommandBlockManager();
        this.commandSpyManager = new CommandSpyManager(this, () ->
            legacySqlStore.getDataSource()
        );

        this.manageChatService = new ManageChatService(this);
        this.guildManager = new GuildManager(this);
        this.chatFilterEngine = new ChatFilterEngine(pluginConfigManager, this);

        this.tagService = new TagService(this);
        this.tagService.init();

        // Discord must be initialized before staffChatManager references it
        this.discord = new DiscordBridge(
            this,
            this.chatFilterEngine,
            this.dataListener
        );
        this.discord.start();

        // Services that depend on discord or shushService
        this.potionSpyService = new PotionSpyService(
            this,
            legacySqlStore.getDataSource()
        );
        this.shushService = new ShushService(
            this,
            legacySqlStore.getDataSource()
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
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();

        pm.registerEvents(dataListener, this);
        pm.registerEvents(loginMessages, this);
        pm.registerEvents(
            new ChatMentionFormatListener(this, chatFilterEngine),
            this
        );
        pm.registerEvents(new ManageChatListener(manageChatService), this);
        pm.registerEvents(new AutoTpListener(this, autoTpService), this);
        new org.thesandbox.core.tags.TagCleanupListener(
            this,
            tagService,
            guildManager
        ).register();

        pm.registerEvents(new AutoClearListener(autoClearService), this);
        pm.registerEvents(potionSpyService, this);
        pm.registerEvents(new ShushListener(this), this);

        this.liteBansWarningListener = new LiteBansWarningListener(this);
        this.liteBansWarningListener.register();

        pm.registerEvents(new ChatFilterListener(chatFilterEngine), this);
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
                genericListener,
                commandBlockManager,
                rankScoreboardManager,
                staffChatManager,
                commandSpyManager
            )
        );

        commandServices.addAll(this.registeredItems);
        CommandAutoRegistrar.registerAll(this, commandServices.toArray());
    }

    private void verifyInstances() {
        Map<String, Object> criticalServices = Map.of(
                "PluginConfigManager",
                this.pluginConfigManager,
                "DataManager",
                this.dataManager,
                "PlayerDataListener",
                this.dataListener,
                "LegacySqlStore",
                this.legacySqlStore,
                "LoginService",
                this.loginService,
                "FunctionsHolder",
                this.functionsHolder
        );

        for (Map.Entry<String, Object> entry : criticalServices.entrySet()) {
            if (entry.getValue() == null) {
                getLogger().severe(
                    "CRITICAL ERROR: " +
                        entry.getKey() +
                        " is NULL after startup!"
                );
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }
    }

    // ==================== Teardown / Cleanup ====================

    private void cleanup() {
        getLogger().info("[cleaning up :)]");

        // 1. Stop tasks & integrations
        if (entityWiper != null) entityWiper.stop();
        if (
            liteBansWarningListener != null
        ) liteBansWarningListener.unregister();
        if (discord != null) discord.stop();

        // 2. Save active data to disk / database
        if (dataListener != null) dataListener.saveAll();
        if (guildManager != null) {
            guildManager.save();
            guildManager.shutdown();
        }
        if (loginService != null) loginService.shutdown();

        // 3. Clean up items
        registeredItems
            .stream()
            .filter(item -> item instanceof FloatBoatItem)
            .map(item -> (FloatBoatItem) item)
            .findFirst()
            .ifPresent(FloatBoatItem::cleanup);

        // 4. Terminate PacketEvents
        try {
            PacketEvents.getAPI().terminate();
        } catch (Throwable t) {
            getLogger().warning(
                "Error terminating PacketEvents: " + t.getMessage()
            );
        }

        // 5. Close database connections last
        if (legacySqlStore != null) legacySqlStore.close();

        getLogger().info("clean up complete! have a nice day! ;)");
    }

    // ==================== Discord / Config Helpers ====================

    public void reloadAndReconnect() {
        reloadConfig();
        legacySqlStore.setup();
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
