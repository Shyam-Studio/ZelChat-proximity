package com.shyamstudio.rai.zelChatProximity.command;

import com.shyamstudio.rai.zelChatProximity.ZelChatProximity;
import com.shyamstudio.rai.zelChatProximity.config.ConfigManager;
import com.shyamstudio.rai.zelChatProximity.data.DataManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PzelchatCommand implements CommandExecutor, TabCompleter {

  private final ZelChatProximity plugin;
  private final DataManager dataManager;
  private final ConfigManager configManager;
  private static final String ADMIN_PERMISSION = "zelchatproximity.admin";

  public PzelchatCommand(ZelChatProximity plugin, DataManager dataManager, ConfigManager configManager) {
    this.plugin = plugin;
    this.dataManager = dataManager;
    this.configManager = configManager;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
      sender.sendMessage(configManager.getConfig().getString("messages.reload-usage", "&8[&6ZelChat-Proximity&8] &cUsage: &f/pzelchat reload").replace("&", "§"));
      return true;
    }

    if (!sender.hasPermission(ADMIN_PERMISSION)) {
      sender.sendMessage(configManager.getConfig().getString("messages.no-permission-reload", "&8[&6ZelChat-Proximity&8] &cInsufficient permissions to reload the plugin.").replace("&", "§"));
      return true;
    }

    CompletableFuture.allOf(
        configManager.loadConfigAsync(),
        dataManager.loadDataAsync(),
        plugin.getSoundManager().loadSoundConfigAsync()
    ).thenRun(() -> {
      plugin.getFoliaLib().getScheduler().runNextTick((task) -> {
        plugin.getProximityModule().reload();
        sender.sendMessage(configManager.getConfig().getString("messages.reload-success", "&8[&6ZelChat-Proximity&8] &aConfiguration reloaded successfully!").replace("&", "§"));
      });
    }).exceptionally(throwable -> {
      sender.sendMessage("§cFailed to reload: " + throwable.getMessage());
      return null;
    });

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    List<String> completions = new ArrayList<>();

    if (args.length == 1 && sender.hasPermission(ADMIN_PERMISSION)) {
      if ("reload".startsWith(args[0].toLowerCase())) {
        completions.add("reload");
      }
    }

    return completions;
  }
}