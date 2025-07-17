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
    this.foliaLib = new FoliaLib(this);

    CompletableFuture.runAsync(() -> {
      try {
        this.zelChatAPI = ZelChatAPI.get();
      } catch (Exception e) {
        getLogger().severe("ZelChat API not found! Make sure ZelChat is installed and loaded.");
        foliaLib.getScheduler().runNextTick((task) ->
            getServer().getPluginManager().disablePlugin(this));
        return;
      }

      foliaLib.getScheduler().runNextTick((task) -> initializeComponents());
    });
  }

  private void initializeComponents() {
    this.configManager = new ConfigManager(this);
    this.dataManager = new DataManager(this);
    this.soundManager = new SoundManager(this);

    CompletableFuture.allOf(
        configManager.loadConfigAsync(),
        dataManager.loadDataAsync(),
        soundManager.loadSoundConfigAsync()
    ).thenRun(() -> {
      foliaLib.getScheduler().runNextTick((task) -> {
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
      });
    }).exceptionally(throwable -> {
      getLogger().severe("Failed to load configurations: " + throwable.getMessage());
      throwable.printStackTrace();
      foliaLib.getScheduler().runNextTick((task) ->
          getServer().getPluginManager().disablePlugin(this));
      return null;
    });
  }

  private void startPeriodicSave() {
    foliaLib.getScheduler().runTimer((task) -> {
      dataManager.saveDataAsync().exceptionally(throwable -> {
        getLogger().warning("Failed to save data periodically: " + throwable.getMessage());
        return null;
      });
    }, 20L * 30, 20L * 30);
  }

  @Override
  public void onDisable() {
    CompletableFuture<Void> shutdownFuture = CompletableFuture.runAsync(() -> {
      if (proximityModule != null) {
        proximityModule.unload();
        if (zelChatAPI != null) {
          zelChatAPI.getModuleManager().unregister(this, proximityModule);
        }
      }
    });

    if (dataManager != null) {
      shutdownFuture = shutdownFuture.thenCompose(v -> dataManager.saveDataAsync());
    }

    try {
      shutdownFuture.get(5, TimeUnit.SECONDS);
    } catch (Exception e) {
      getLogger().warning("Failed to complete shutdown tasks: " + e.getMessage());
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