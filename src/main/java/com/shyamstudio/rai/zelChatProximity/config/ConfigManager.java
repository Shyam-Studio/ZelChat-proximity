package com.shyamstudio.rai.zelChatProximity.config;

import com.shyamstudio.rai.zelChatProximity.ZelChatProximity;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Blocking;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class ConfigManager {

    private final ZelChatProximity plugin;
    private final AtomicReference<FileConfiguration> config;
    private volatile double proximityRadius;
    private volatile boolean debugEnabled;
    private volatile String localModeMessage;
    private volatile String globalModeMessage;
    private volatile String noPermissionMessage;

    public ConfigManager(ZelChatProximity plugin) {
        this.plugin = plugin;
        this.config = new AtomicReference<>();
    }

    @Blocking
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration newConfig = plugin.getConfig();
        config.set(newConfig);

        proximityRadius = newConfig.getDouble("proximity-radius", 200.0);
        debugEnabled = newConfig.getBoolean("debug", false);

        localModeMessage = newConfig.getString("messages.local-mode",
                        "&8[&6Chat&8] &7Switched to &a&lLocal &7chat mode &8(&a<radius> blocks&8)")
                .replace("<radius>", String.valueOf((int) proximityRadius));

        globalModeMessage = newConfig.getString("messages.global-mode",
                "&8[&6Chat&8] &7Switched to &9&lGlobal &7chat mode &8(&9Unlimited range&8)");

        noPermissionMessage = newConfig.getString("messages.no-permission",
                "&8[&6Chat&8] &cInsufficient permissions for local chat. &7Switched to global mode.");
    }

    public double getProximityRadius() {
        return proximityRadius;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public String getLocalModeMessage() {
        return localModeMessage;
    }

    public String getGlobalModeMessage() {
        return globalModeMessage;
    }

    public String getNoPermissionMessage() {
        return noPermissionMessage;
    }

    public FileConfiguration getConfig() {
        return config.get();
    }
}