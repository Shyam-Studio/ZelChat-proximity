package com.shyamstudio.rai.zelChatProximity.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.shyamstudio.rai.zelChatProximity.ZelChatProximity;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DataManager {

  private final ZelChatProximity plugin;
  private final File dataFile;
  private final File tempFile;
  private final Gson gson;
  private final ConcurrentHashMap<UUID, PlayerData> playerDataMap;
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private volatile boolean pendingSave = false;

  public DataManager(ZelChatProximity plugin) {
    this.plugin = plugin;
    this.dataFile = new File(plugin.getDataFolder(), "playerdata.json");
    this.tempFile = new File(plugin.getDataFolder(), "playerdata.json.tmp");
    this.gson = new GsonBuilder().setPrettyPrinting().create();
    this.playerDataMap = new ConcurrentHashMap<>();
  }

  public CompletableFuture<Void> loadDataAsync() {
    return CompletableFuture.runAsync(() -> {
      if (!plugin.getDataFolder().exists()) {
        plugin.getDataFolder().mkdirs();
      }

      if (!dataFile.exists()) {
        saveDataSync();
        return;
      }

      lock.writeLock().lock();
      try (FileReader reader = new FileReader(dataFile)) {
        Type type = new TypeToken<ConcurrentHashMap<UUID, PlayerData>>(){}.getType();
        ConcurrentHashMap<UUID, PlayerData> loaded = gson.fromJson(reader, type);
        if (loaded != null) {
          playerDataMap.clear();
          playerDataMap.putAll(loaded);
        }
      } catch (IOException e) {
        plugin.getLogger().warning("Failed to load player data: " + e.getMessage());
      } finally {
        lock.writeLock().unlock();
      }
    });
  }

  public CompletableFuture<Void> saveDataAsync() {
    if (pendingSave) {
      return CompletableFuture.completedFuture(null);
    }

    pendingSave = true;
    return CompletableFuture.runAsync(() -> {
      try {
        saveDataSync();
      } finally {
        pendingSave = false;
      }
    });
  }

  private void saveDataSync() {
    lock.readLock().lock();
    try {
      ConcurrentHashMap<UUID, PlayerData> dataCopy = new ConcurrentHashMap<>(playerDataMap);

      try (FileWriter writer = new FileWriter(tempFile)) {
        gson.toJson(dataCopy, writer);
        writer.flush();
      }

      Files.move(tempFile.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

    } catch (IOException e) {
      plugin.getLogger().warning("Failed to save player data: " + e.getMessage());
    } finally {
      lock.readLock().unlock();
    }
  }

  public PlayerData getPlayerData(UUID playerId) {
    return playerDataMap.computeIfAbsent(playerId, id -> new PlayerData(id, false));
  }

  public void setPlayerData(UUID playerId, PlayerData data) {
    playerDataMap.put(playerId, data);
  }

  public CompletableFuture<Void> togglePlayerModeAsync(Player player) {
    return CompletableFuture.runAsync(() -> {
      PlayerData data = getPlayerData(player.getUniqueId());
      data.toggleMode();
      setPlayerData(player.getUniqueId(), data);

      plugin.getFoliaLib().getScheduler().runNextTick((task) -> {
        String message = data.isLocalMode() ?
            plugin.getConfigManager().getLocalModeMessage() :
            plugin.getConfigManager().getGlobalModeMessage();

        player.sendMessage(message.replace("&", "§"));

        if (plugin.getConfigManager().isDebugEnabled()) {
          plugin.getLogger().info("[DEBUG] Player " + player.getName() + " toggled to " +
              (data.isLocalMode() ? "LOCAL" : "GLOBAL") + " mode");
        }
      });
    });
  }

  public void markDirty() {

  }
}