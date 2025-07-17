package com.shyamstudio.rai.zelChatProximity;

import com.shyamstudio.rai.zelChatProximity.command.ChatCommand;
import com.shyamstudio.rai.zelChatProximity.command.PzelchatCommand;
import com.shyamstudio.rai.zelChatProximity.config.ConfigManager;
import com.shyamstudio.rai.zelChatProximity.data.DataManager;
import com.shyamstudio.rai.zelChatProximity.gui.GuiManager;
import com.shyamstudio.rai.zelChatProximity.listener.PlayerListener;
import com.shyamstudio.rai.zelChatProximity.module.ProximityChatModule;
import com.shyamstudio.rai.zelChatProximity.sound.SoundManager;
import com.tcoded.folialib.FoliaLib;
import it.pino.zelchat.api.ZelChatAPI;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class ZelChatProximity extends JavaPlugin {

    private ZelChatAPI zelChatAPI;
    private FoliaLib foliaLib;
    private ConfigManager configManager;
    private DataManager dataManager;
    private SoundManager soundManager;
    private GuiManager guiManager;
    private ProximityChatModule proximityModule;

    @Override
    public void onEnable() {
        this.getLogger().setLevel(Level.INFO);
        this.foliaLib = new FoliaLib(this);
        try {
            this.zelChatAPI = ZelChatAPI.get();
        } catch (Exception e) {
            getLogger().severe("ZelChat API not found! Make sure ZelChat is installed and loaded.");
            foliaLib.getScheduler().runNextTick((task) ->
                    getServer().getPluginManager().disablePlugin(this));
            return;
        }
        initializeComponents();
    }

    private void initializeComponents() {
        getLogger().info("Initializing ZelChat proximity");
        this.configManager = new ConfigManager(this);
        this.dataManager = new DataManager(this);
        this.soundManager = new SoundManager(this);

        configManager.loadConfig();
        dataManager.loadData();
        soundManager.loadSoundConfig();
        try {
            this.guiManager = new GuiManager(this, configManager, dataManager, soundManager);
            this.proximityModule = new ProximityChatModule(this, dataManager, configManager);

            zelChatAPI.getModuleManager().register(this, proximityModule);
            proximityModule.load();

            getServer().getPluginManager().registerEvents(new PlayerListener(dataManager), this);
            getServer().getPluginManager().registerEvents(guiManager, this);

            getCommand("chat").setExecutor(new ChatCommand(configManager, guiManager));
            getCommand("pzelchat").setExecutor(new PzelchatCommand(this, dataManager, configManager));

            startPeriodicSave();
            getLogger().info("ZelChat Proximity enabled successfully!");

        } catch (Exception e) {
            getLogger().severe("Failed to initialize components: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void startPeriodicSave() {
        foliaLib.getScheduler().runTimerAsync((task) -> dataManager.saveData(), 20L * 30, 20L * 30);
    }

    @Override
    public void onDisable() {
            if (proximityModule != null) {
                proximityModule.unload();
                if (zelChatAPI != null) {
                    zelChatAPI.getModuleManager().unregister(this, proximityModule);
                }
            }

        if (dataManager != null) {
            dataManager.saveData();
        }

        if (foliaLib != null) {
            foliaLib.getScheduler().cancelAllTasks();
        }
    }

    public FoliaLib getFoliaLib() {
        return foliaLib;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }


    public SoundManager getSoundManager() {
        return soundManager;
    }

    public ProximityChatModule getProximityModule() {
        return proximityModule;
    }
}