package com.shyamstudio.rai.zelChatProximity.sound;

import com.shyamstudio.rai.zelChatProximity.ZelChatProximity;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Blocking;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class SoundManager {

  private final ZelChatProximity plugin;
  private final AtomicReference<FileConfiguration> soundConfig;
  private File soundFile;
  private final ConcurrentHashMap<String, SoundData> soundCache;

  private static class SoundData {
    final Sound sound;
    final float volume;
    final float pitch;
    final boolean enabled;

    SoundData(Sound sound, float volume, float pitch, boolean enabled) {
      this.sound = sound;
      this.volume = volume;
      this.pitch = pitch;
      this.enabled = enabled;
    }
  }

  public SoundManager(ZelChatProximity plugin) {
    this.plugin = plugin;
    this.soundConfig = new AtomicReference<>();
    this.soundCache = new ConcurrentHashMap<>();
  }

  @Blocking
  public void loadSoundConfig() {
      soundFile = new File(plugin.getDataFolder(), "sounds.yml");

      if (!soundFile.exists()) {
        try {
          plugin.getDataFolder().mkdirs();
          InputStream inputStream = plugin.getResource("sounds.yml");
          if (inputStream != null) {
            Files.copy(inputStream, soundFile.toPath());
          } else {
            soundFile.createNewFile();
          }
        } catch (IOException e) {
          plugin.getLogger().warning("Failed to create sounds.yml: " + e.getMessage());
        }
      }

      FileConfiguration config = YamlConfiguration.loadConfiguration(soundFile);
      setDefaults(config);
      saveSoundConfig(config);
      soundConfig.set(config);
      updateSoundCache(config);
  }

  private void setDefaults(FileConfiguration config) {
    config.addDefault("sounds.gui-open.sound", "ITEM_SPYGLASS_USE");
    config.addDefault("sounds.gui-open.volume", 1.0);
    config.addDefault("sounds.gui-open.pitch", 1.0);
    config.addDefault("sounds.gui-open.enabled", true);

    config.addDefault("sounds.mode-switch.sound", "ENTITY_ITEM_PICKUP");
    config.addDefault("sounds.mode-switch.volume", 0.8);
    config.addDefault("sounds.mode-switch.pitch", 1.2);
    config.addDefault("sounds.mode-switch.enabled", true);

    config.addDefault("sounds.gui-close.sound", "ENTITY_VILLAGER_NO");
    config.addDefault("sounds.gui-close.volume", 0.6);
    config.addDefault("sounds.gui-close.pitch", 1.0);
    config.addDefault("sounds.gui-close.enabled", true);

    config.options().copyDefaults(true);
  }

  private void saveSoundConfig(FileConfiguration config) {
    try {
      config.save(soundFile);
    } catch (IOException e) {
      plugin.getLogger().warning("Failed to save sounds.yml: " + e.getMessage());
    }
  }

  private void updateSoundCache(FileConfiguration config) {
    soundCache.clear();

    String[] soundKeys = {"gui-open", "mode-switch", "gui-close"};

    for (String key : soundKeys) {
      try {
        String soundName = config.getString("sounds." + key + ".sound");
        float volume = (float) config.getDouble("sounds." + key + ".volume", 1.0);
        float pitch = (float) config.getDouble("sounds." + key + ".pitch", 1.0);
        boolean enabled = config.getBoolean("sounds." + key + ".enabled", true);

        Sound sound = Sound.valueOf(soundName);
        soundCache.put(key, new SoundData(sound, volume, pitch, enabled));
      } catch (IllegalArgumentException e) {
        plugin.getLogger().warning("Invalid sound name for " + key + ": " + e.getMessage());
      }
    }
  }

  public void playGuiOpenSound(Player player) {
    playSound(player, "gui-open");
  }

  public void playModeSwitchSound(Player player) {
    playSound(player, "mode-switch");
  }

  public void playGuiCloseSound(Player player) {
    playSound(player, "gui-close");
  }

  private void playSound(Player player, String soundKey) {
    CompletableFuture.runAsync(() -> {
      SoundData soundData = soundCache.get(soundKey);
      if (soundData == null || !soundData.enabled) {
        return;
      }

      plugin.getFoliaLib().getScheduler().runNextTick((task) -> {
        try {
          player.playSound(player.getLocation(), soundData.sound, soundData.volume, soundData.pitch);
        } catch (Exception e) {
          plugin.getLogger().warning("Failed to play sound " + soundKey + ": " + e.getMessage());
        }
      });
    });
  }
}