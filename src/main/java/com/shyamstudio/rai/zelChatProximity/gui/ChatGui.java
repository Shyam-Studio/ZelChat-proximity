package com.shyamstudio.rai.zelChatProximity.gui;

import com.shyamstudio.rai.zelChatProximity.config.ConfigManager;
import com.shyamstudio.rai.zelChatProximity.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatGui implements InventoryHolder {

    @NotNull
    private final Inventory inventory;
    @NotNull
    private final ConfigManager configManager;

    public ChatGui(@NotNull Map<String, ItemStack> itemCache, @NotNull ConfigManager configManager, @NotNull PlayerData playerData, @NotNull String playerName) {
        this.configManager = configManager;
        this.inventory = this.buildInventory(itemCache, playerData, playerName);
    }

    private Inventory buildInventory(@NotNull Map<String, ItemStack> itemCache, @NotNull PlayerData playerData, @NotNull String playerName) {
        boolean isLocal = playerData.isLocalMode();
        String title = configManager.getConfig().getString("gui.title", "&8Chat Mode Selection")
                .replace("&", "§");

        Inventory gui = Bukkit.createInventory(this, 27, title);

        setupFillerItems(gui, itemCache);
        setupInfoItem(gui, playerName, isLocal, itemCache);
        setupConfirmItem(gui, isLocal, itemCache);
        setupCloseItem(gui, itemCache);

        return gui;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    private void setupFillerItems(Inventory gui, Map<String, ItemStack> itemCache) {
        ItemStack filler = itemCache.get("filler");

        List<Integer> fillerSlots = new ArrayList<>();
        for (int i = 0; i <= 26; i++) {
            if (i != 10 && i != 13 && i != 16) {
                fillerSlots.add(i);
            }
        }

        for (int slot : fillerSlots) {
            gui.setItem(slot, filler);
        }
    }

    private void setupInfoItem(Inventory gui, String playerName, boolean isLocal, Map<String, ItemStack> itemCache) {
        String currentMode = isLocal ? "Local" : "Global";
        String radius = String.valueOf((int) configManager.getProximityRadius());

        List<String> lore = new ArrayList<>();
        for (String line : configManager.getConfig().getStringList("gui.info.lore")) {
            lore.add(line.replace("&", "§")
                    .replace("<current_mode>", currentMode)
                    .replace("<radius>", radius)
                    .replace("<player>", playerName));
        }

        String displayName = configManager.getConfig().getString("gui.info.display-name", "&e&lCHAT INFORMATION")
                .replace("&", "§")
                .replace("<current_mode>", currentMode);

        ItemStack infoMaterial = itemCache.get("info_material");
        ItemStack info = GuiUtils.createItem(infoMaterial.getType(), displayName, lore);
        gui.setItem(13, info);
    }

    private void setupConfirmItem(Inventory gui, boolean isLocal, Map<String, ItemStack> itemCache) {
        String targetMode = isLocal ? "Global" : "Local";
        ItemStack materialStack = isLocal ? itemCache.get("global_material") : itemCache.get("local_material");

        List<String> lore = new ArrayList<>();
        String loreSection = isLocal ? "gui.confirm.global-lore" : "gui.confirm.local-lore";
        for (String line : configManager.getConfig().getStringList(loreSection)) {
            lore.add(line.replace("&", "§")
                    .replace("<target_mode>", targetMode)
                    .replace("<radius>", String.valueOf((int) configManager.getProximityRadius())));
        }

        String displayName = configManager.getConfig().getString(
                isLocal ? "gui.confirm.global-display-name" : "gui.confirm.local-display-name",
                "&a&lSwitch to " + targetMode
        ).replace("&", "§").replace("<target_mode>", targetMode);

        ItemStack confirm = GuiUtils.createItem(materialStack.getType(), displayName, lore);
        gui.setItem(16, confirm);
    }

    private void setupCloseItem(Inventory gui, Map<String, ItemStack> itemCache) {
        ItemStack close = itemCache.get("close");
        gui.setItem(10, close);
    }
}
