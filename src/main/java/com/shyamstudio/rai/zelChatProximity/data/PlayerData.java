package com.shyamstudio.rai.zelChatProximity.data;

import java.util.UUID;

public class PlayerData {

  private UUID playerId;
  private boolean localMode;

  public PlayerData(UUID playerId, boolean localMode) {
    this.playerId = playerId;
    this.localMode = localMode;
  }

  public boolean isLocalMode() {
    return localMode;
  }

  public void toggleMode() {
    this.localMode = !this.localMode;
  }
}