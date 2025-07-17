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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

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
        if (isInvalidCommand(args)) {
            sender.sendMessage(configManager.getConfig().getString("messages.reload-usage", "&8[&6ZelChat-Proximity&8] &cUsage: &f/pzelchat reload|debug").replace("&", "§"));
            return true;
        }

        switch(args[0].toLowerCase()) {
            case "reload": executeReload(sender); break;
            case "debug": changeDebugLevel(sender); break;
            default: break;
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(args.length == 1) return List.of("reload", "debug");
        return List.of();
    }

    private boolean isInvalidCommand(String[] args) {
        return args.length > 0 && !args[0].equalsIgnoreCase("reload") && !args[0].equalsIgnoreCase("debug");
    }

    private void executeReload(final CommandSender sender) {
        CompletableFuture.runAsync(() -> {
            try{
                configManager.loadConfig();
                dataManager.loadData();
                plugin.getSoundManager().loadSoundConfig();
                plugin.getProximityModule().reload();
                sender.sendMessage(configManager.getConfig().getString("messages.reload-success", "&8[&6ZelChat-Proximity&8] &aConfiguration reloaded successfully!").replace("&", "§"));
            }catch(Exception ex){
                plugin.getLogger().severe("An error occurred while reloading the plugin: " + ex);
                sender.sendMessage("§cFailed to reload: " + ex.getMessage());
            }
        });
    }

    private void changeDebugLevel(final CommandSender sender){
        final var level = plugin.getLogger().getLevel();

        if(!level.equals(Level.INFO)){
            sender.sendMessage("§8[§6ZelChat-Proximity§8] §aDebugger has been disabled!");
            plugin.getLogger().setLevel(Level.INFO);
            Arrays.stream(plugin.getLogger().getHandlers()).forEach(handler -> handler.setLevel(Level.INFO));
            return;
        }
        plugin.getLogger().setLevel(Level.ALL);
        Arrays.stream(plugin.getLogger().getHandlers()).forEach(handler -> handler.setLevel(Level.ALL));
        sender.sendMessage("§8[§6ZelChat-Proximity§8] §aDebugger has been enabled! Check the console for more details.");
    }
}