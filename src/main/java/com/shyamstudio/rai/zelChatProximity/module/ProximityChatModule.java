package com.shyamstudio.rai.zelChatProximity.module;

import com.shyamstudio.rai.zelChatProximity.ZelChatProximity;
import com.shyamstudio.rai.zelChatProximity.config.ConfigManager;
import com.shyamstudio.rai.zelChatProximity.data.DataManager;
import com.shyamstudio.rai.zelChatProximity.data.PlayerData;
import it.pino.zelchat.api.message.ChatMessage;
import it.pino.zelchat.api.message.channel.ChannelType;
import it.pino.zelchat.api.message.state.MessageState;
import it.pino.zelchat.api.module.ChatModule;
import it.pino.zelchat.api.module.annotation.ChatModuleSettings;
import it.pino.zelchat.api.module.priority.ModulePriority;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

@ChatModuleSettings(pluginOwner = "ZelChat-proximity", priority = ModulePriority.NORMAL)
public class ProximityChatModule implements ChatModule {

    private final ZelChatProximity plugin;
    private final DataManager dataManager;
    private final ConfigManager configManager;
    private final ConcurrentHashMap<UUID, Location> locationCache;
    private final ConcurrentHashMap<UUID, Boolean> permissionCache;
    private final AtomicReference<List<Player>> cachedPlayers;
    private volatile long lastCacheUpdate = 0;
    private static final long CACHE_DURATION = 50L;
    private static final String LOCAL_PERMISSION = "zelchatproximity.local";

    public ProximityChatModule(ZelChatProximity plugin, DataManager dataManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.configManager = configManager;
        this.locationCache = new ConcurrentHashMap<>();
        this.permissionCache = new ConcurrentHashMap<>();
        this.cachedPlayers = new AtomicReference<>(new ArrayList<>());
        startCacheUpdater();
    }

    private void startCacheUpdater() {
        plugin.getFoliaLib().getScheduler().runTimer((task) -> updatePlayerCache(), 20L, 2L);
    }

    private void updatePlayerCache() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCacheUpdate < CACHE_DURATION) {
            return;
        }
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        cachedPlayers.set(players);

        for (Player player : players) {
            UUID uuid = player.getUniqueId();
            locationCache.put(uuid, player.getLocation().clone());
            permissionCache.put(uuid, player.hasPermission(LOCAL_PERMISSION));
        }

        lastCacheUpdate = currentTime;
    }

    @Override
    public void load() {
        plugin.getLogger().info("Proximity chat module loaded");
    }

    @Override
    public void unload() {
        plugin.getLogger().info("Proximity chat module unloaded");
    }

    public void reload() {
        locationCache.clear();
        permissionCache.clear();
        updatePlayerCache();
    }

    private double calculate2DDistanceSquared(Location loc1, Location loc2) {
        double dx = loc1.getX() - loc2.getX();
        double dz = loc1.getZ() - loc2.getZ();
        return dx * dx + dz * dz;
    }

    @Override
    public void handleChatMessage(@NotNull ChatMessage chatMessage) {
        if (chatMessage.getState() != MessageState.PROCESSING && chatMessage.getState() != MessageState.READY) {
            if (configManager.isDebugEnabled()) {
                plugin.logDebugMessage("[DEBUG] Skipping message due to state: " + chatMessage.getState());
            }
            return;
        }

        var channel = chatMessage.getChannel();
        var channelType = channel.getType();

        if (channelType == ChannelType.STAFF || channelType == ChannelType.PRIVATE) {
            if (configManager.isDebugEnabled()) {
                plugin.logDebugMessage("[DEBUG] Skipping proximity filtering for channel type: " + channelType);
            }
            return;
        }

        Player sender = chatMessage.getBukkitPlayer();
        UUID senderUUID = sender.getUniqueId();
        PlayerData senderData = dataManager.getOrLoadPlayerData(senderUUID);
        Location senderLocation = locationCache.getOrDefault(senderUUID, sender.getLocation());
        World senderWorld = senderLocation.getWorld();
        double radius = configManager.getProximityRadius();
        double radiusSquared = radius * radius;

        Boolean senderHasPermission = permissionCache.get(senderUUID);
        if (senderHasPermission == null) {
            senderHasPermission = sender.hasPermission(LOCAL_PERMISSION);
            permissionCache.put(senderUUID, senderHasPermission);
        }

        boolean senderIsLocal = senderData.isLocalMode() && senderHasPermission;

        if (!senderIsLocal) {
            boolean anyLocalPlayers = false;
            List<Player> players = cachedPlayers.get();

            for (Player onlinePlayer : players) {
                UUID playerUUID = onlinePlayer.getUniqueId();
                PlayerData playerData = dataManager.getOrLoadPlayerData(playerUUID);
                Boolean hasPermission = permissionCache.get(playerUUID);
                if (hasPermission == null) {
                    hasPermission = onlinePlayer.hasPermission(LOCAL_PERMISSION);
                    permissionCache.put(playerUUID, hasPermission);
                }

                if (playerData.isLocalMode() && hasPermission) {
                    anyLocalPlayers = true;
                    break;
                }
            }

            if (!anyLocalPlayers) {
                if (configManager.isDebugEnabled()) {
                    plugin.logDebugMessage("[DEBUG] Sender is GLOBAL and no local players online - no filtering needed");
                }
                return;
            }
        }

        if (configManager.isDebugEnabled()) {
            plugin.logDebugMessage("[DEBUG] Applying proximity filtering - Sender: " + sender.getName() +
                    " (mode: " + (senderIsLocal ? "LOCAL" : "GLOBAL") + ")");
        }

        Collection<Audience> filteredViewers = getFilteredViewers(senderIsLocal, senderLocation, senderWorld, radiusSquared);
        if (filteredViewers.isEmpty()) {
            filteredViewers.add(sender);
            if (configManager.isDebugEnabled()) {
                plugin.logDebugMessage("[DEBUG] No players in range, adding sender to viewers");
            }
        }

        if (configManager.isDebugEnabled()) {
            plugin.logDebugMessage("[DEBUG] Setting " + filteredViewers.size() + " filtered viewers for " + sender.getName());
        }

        channel.setViewers(filteredViewers);
        channel.setType(ChannelType.CUSTOM);

        if (configManager.isDebugEnabled()) {
            plugin.logDebugMessage("[DEBUG] Set channel type to CUSTOM for ZelChat recognition");
        }
    }

    private Collection<Audience> getFilteredViewers(boolean senderIsLocal, Location senderLocation, World senderWorld, double radiusSquared) {
        Collection<Audience> filteredViewers = new ArrayList<>();
        List<Player> players = cachedPlayers.get();

        for (Player onlinePlayer : players) {
            UUID playerUUID = onlinePlayer.getUniqueId();
            PlayerData playerData = dataManager.getOrLoadPlayerData(playerUUID);
            Boolean hasPermission = permissionCache.get(playerUUID);
            if (hasPermission == null) {
                hasPermission = onlinePlayer.hasPermission(LOCAL_PERMISSION);
                permissionCache.put(playerUUID, hasPermission);
            }

            boolean playerIsLocal = playerData.isLocalMode() && hasPermission;

            if (!playerIsLocal) {
                if (!senderIsLocal) {
                    filteredViewers.add(onlinePlayer);
                    if (configManager.isDebugEnabled()) {
                        plugin.logDebugMessage("[DEBUG] Added GLOBAL player " + onlinePlayer.getName() + " to viewers (sender is GLOBAL)");
                    }
                } else {
                    if (configManager.isDebugEnabled()) {
                        plugin.logDebugMessage("[DEBUG] Excluded GLOBAL player " + onlinePlayer.getName() + " from viewers (sender is LOCAL)");
                    }
                }
                continue;
            }

            Location playerLocation = locationCache.get(playerUUID);
            if (playerLocation == null) {
                playerLocation = onlinePlayer.getLocation();
                locationCache.put(playerUUID, playerLocation.clone());
            }

            if (!playerLocation.getWorld().equals(senderWorld)) {
                if (configManager.isDebugEnabled()) {
                    plugin.logDebugMessage("[DEBUG] Excluded LOCAL player " + onlinePlayer.getName() + " from viewers (different world)");
                }
                continue;
            }

            double distanceSquared = calculate2DDistanceSquared(senderLocation, playerLocation);
            if (distanceSquared <= radiusSquared) {
                filteredViewers.add(onlinePlayer);
                if (configManager.isDebugEnabled()) {
                    plugin.logDebugMessage("[DEBUG] Added LOCAL player " + onlinePlayer.getName() + " to viewers (2D distance: " + String.format("%.2f", Math.sqrt(distanceSquared)) + ")");
                }
            } else {
                if (configManager.isDebugEnabled()) {
                    plugin.logDebugMessage("[DEBUG] Excluded LOCAL player " + onlinePlayer.getName() + " from viewers (2D distance: " + String.format("%.2f", Math.sqrt(distanceSquared)) + ")");
                }
            }
        }

        return filteredViewers;
    }
}