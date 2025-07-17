package com.shyamstudio.rai.zelChatProximity.gui;

import com.shyamstudio.rai.zelChatProximity.ZelChatProximity;
import com.shyamstudio.rai.zelChatProximity.config.ConfigManager;
import com.shyamstudio.rai.zelChatProximity.data.DataManager;
import com.shyamstudio.rai.zelChatProximity.data.PlayerData;
import com.shyamstudio.rai.zelChatProximity.sound.SoundManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GuiManager implements Listener {

    private final ZelChatProximity plugin;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final SoundManager soundManager;
    private final Map<String, ItemStack> itemCache;

    public GuiManager(ZelChatProximity plugin, ConfigManager configManager, DataManager dataManager, SoundManager soundManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.dataManager = dataManager;
        this.soundManager = soundManager;
        this.itemCache = new ConcurrentHashMap<>();
        initializeItemCacheSync();
    }

    private void initializeItemCacheSync() {
        try {
            List<Integer> fillerSlots = new ArrayList<>();
            for (int i = 0; i <= 26; i++) {
                if (i != 10 && i != 13 && i != 16) {
                    fillerSlots.add(i);
                }
            }

            ItemStack filler = GuiUtils.createItem(
                    Material.BLACK_STAINED_GLASS_PANE,
                    configManager.getConfig().getString("gui.decoration.filler.display-name", "&f"),
                    configManager.getConfig().getStringList("gui.decoration.filler.lore")
            );
            itemCache.put("filler", filler);

            Material closeMaterial = Material.valueOf(configManager.getConfig().getString("gui.close.material", "RED_WOOL"));
            String closeDisplayName = configManager.getConfig().getString("gui.close.display-name", "&c&lCLOSE");
            List<String> closeLore = configManager.getConfig().getStringList("gui.close.lore");
            ItemStack closeItem = GuiUtils.createItem(closeMaterial, closeDisplayName, closeLore);
            itemCache.put("close", closeItem);

            Material infoMaterial = Material.valueOf(configManager.getConfig().getString("gui.info.material", "LECTERN"));
            itemCache.put("info_material", new ItemStack(infoMaterial));

            Material localMaterial = Material.valueOf(configManager.getConfig().getString("gui.confirm.local-material", "GREEN_WOOL"));
            Material globalMaterial = Material.valueOf(configManager.getConfig().getString("gui.confirm.global-material", "BLUE_WOOL"));
            itemCache.put("local_material", new ItemStack(localMaterial));
            itemCache.put("global_material", new ItemStack(globalMaterial));

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to initialize item cache: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void openChatGui(Player player) {
        PlayerData playerData = dataManager.getOrLoadPlayerData(player.getUniqueId());
        final var chatGui = new ChatGui(itemCache, configManager, playerData, player.getName());
        player.openInventory(chatGui.getInventory());
        soundManager.playGuiOpenSound(player);
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {

        if (!(event.getInventory().getHolder(false) instanceof ChatGui)) return;

        Player player = (Player) event.getWhoClicked();
        event.setCancelled(true);

        int slot = event.getSlot();

        if (slot == 16) {
            player.getScheduler().runDelayed(plugin, scheduledTask -> {
                player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                dataManager.togglePlayerModeAsync(player).whenComplete((result, error) -> {
                    if (error != null) {
                        plugin.getLogger().warning("Failed to set player mode for " + error);
                    }

                });
            }, null, 1L);


        } else if (slot == 10) {
            player.getScheduler().runDelayed(plugin, scheduledTask -> {
                player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                soundManager.playGuiCloseSound(player);
                String message = configManager.getConfig().getString("gui.messages.cancelled", "&8[&6Chat&8] &7Menu closed.")
                        .replace("&", "§");
                player.sendMessage(message);
            }, null, 1L);
        }
    }

}