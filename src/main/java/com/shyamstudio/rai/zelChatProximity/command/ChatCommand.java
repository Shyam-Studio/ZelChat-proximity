package com.shyamstudio.rai.zelChatProximity.command;

import com.shyamstudio.rai.zelChatProximity.config.ConfigManager;
import com.shyamstudio.rai.zelChatProximity.gui.GuiManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class ChatCommand implements CommandExecutor {

  private final ConfigManager configManager;
  private final GuiManager guiManager;
  private static final String USE_PERMISSION = "zelchatproximity.use";

  public ChatCommand(ConfigManager configManager, GuiManager guiManager) {
    this.configManager = configManager;
    this.guiManager = guiManager;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    if (!(sender instanceof Player)) {
      sender.sendMessage(configManager.getConfig().getString("messages.player-only", "&8[&6Chat&8] &cThis command can only be used by players.").replace("&", "§"));
      return true;
    }

    Player player = (Player) sender;
    if (!player.hasPermission(USE_PERMISSION)) {
      player.sendMessage(configManager.getConfig().getString("messages.no-permission-command", "&8[&6Chat&8] &cInsufficient permissions to use this command.").replace("&", "§"));
      return true;
    }

    CompletableFuture.runAsync(() -> {
      guiManager.openChatGui(player);
    });

    return true;
  }
}