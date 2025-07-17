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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class GuiManager implements Listener {

  private final ZelChatProximity plugin;
  private final ConfigManager configManager;
  private final DataManager dataManager;
  private final SoundManager soundManager;
  private final ConcurrentHashMap<String, ItemStack> itemCache;

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

      ItemStack filler = createItem(
          Material.BLACK_STAINED_GLASS_PANE,
          configManager.getConfig().getString("gui.decoration.filler.display-name", "&f"),
          configManager.getConfig().getStringList("gui.decoration.filler.lore")
      );
      itemCache.put("filler", filler);

      Material closeMaterial = Material.valueOf(configManager.getConfig().getString("gui.close.material", "RED_WOOL"));
      String closeDisplayName = configManager.getConfig().getString("gui.close.display-name", "&c&lCLOSE");
      List<String> closeLore = configManager.getConfig().getStringList("gui.close.lore");
      ItemStack closeItem = createItem(closeMaterial, closeDisplayName, closeLore);
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
    CompletableFuture.runAsync(() -> {
      PlayerData playerData = dataManager.getPlayerData(player.getUniqueId());
      boolean isLocal = playerData.isLocalMode();

      plugin.getFoliaLib().getScheduler().runNextTick((task) -> {
        String title = configManager.getConfig().getString("gui.title", "&8Chat Mode Selection")
            .replace("&", "§");

        Inventory gui = Bukkit.createInventory(null, 27, title);

        setupFillerItems(gui);
        setupInfoItem(gui, player, isLocal);
        setupConfirmItem(gui, isLocal);
        setupCloseItem(gui);

        player.openInventory(gui);
        soundManager.playGuiOpenSound(player);
      });
    });
  }

  private void setupFillerItems(Inventory gui) {
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

  private void setupInfoItem(Inventory gui, Player player, boolean isLocal) {
    String currentMode = isLocal ? "Local" : "Global";
    String radius = String.valueOf((int) configManager.getProximityRadius());

    List<String> lore = new ArrayList<>();
    for (String line : configManager.getConfig().getStringList("gui.info.lore")) {
      lore.add(line.replace("&", "§")
          .replace("<current_mode>", currentMode)
          .replace("<radius>", radius)
          .replace("<player>", player.getName()));
    }

    String displayName = configManager.getConfig().getString("gui.info.display-name", "&e&lCHAT INFORMATION")
        .replace("&", "§")
        .replace("<current_mode>", currentMode);

    ItemStack infoMaterial = itemCache.get("info_material");
    ItemStack info = createItem(infoMaterial.getType(), displayName, lore);
    gui.setItem(13, info);
  }

  private void setupConfirmItem(Inventory gui, boolean isLocal) {
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

    ItemStack confirm = createItem(materialStack.getType(), displayName, lore);
    gui.setItem(16, confirm);
  }

  private void setupCloseItem(Inventory gui) {
    ItemStack close = itemCache.get("close");
    gui.setItem(10, close);
  }

  private ItemStack createItem(Material material, String displayName, List<String> lore) {
    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      meta.setDisplayName(displayName.replace("&", "§"));
      List<String> coloredLore = new ArrayList<>();
      for (String line : lore) {
        coloredLore.add(line.replace("&", "§"));
      }
      meta.setLore(coloredLore);
      item.setItemMeta(meta);
    }
    return item;
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player)) return;

    Player player = (Player) event.getWhoClicked();
    String title = configManager.getConfig().getString("gui.title", "&8Chat Mode Selection")
        .replace("&", "§");

    if (!event.getView().getTitle().equals(title)) return;

    event.setCancelled(true);

    int slot = event.getSlot();

    if (slot == 16) {
      CompletableFuture.runAsync(() -> {
        dataManager.togglePlayerModeAsync(player).thenRun(() -> {
          plugin.getFoliaLib().getScheduler().runNextTick((task) -> {
            soundManager.playModeSwitchSound(player);
            player.closeInventory();
          });
        });
      });

    } else if (slot == 10) {
      player.closeInventory();
      soundManager.playGuiCloseSound(player);

      String message = configManager.getConfig().getString("gui.messages.cancelled", "&8[&6Chat&8] &7Menu closed.")
          .replace("&", "§");
      player.sendMessage(message);
    }
  }
}