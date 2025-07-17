package com.shyamstudio.rai.zelChatProximity.listener;

import com.shyamstudio.rai.zelChatProximity.data.DataManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.concurrent.CompletableFuture;

public class PlayerListener implements Listener {

  private final DataManager dataManager;

  public PlayerListener(DataManager dataManager) {
    this.dataManager = dataManager;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerJoin(PlayerJoinEvent event) {
    CompletableFuture.runAsync(() -> {
      dataManager.getOrLoadPlayerData(event.getPlayer().getUniqueId());
    });
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerQuit(PlayerQuitEvent event) {
    CompletableFuture.runAsync(() -> {
      dataManager.markDirty();
    });
  }
}