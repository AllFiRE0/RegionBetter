package com.allfire.regionbetter;

import com.allfire.regionbetter.commands.RegionCommand;
import com.allfire.regionbetter.config.ConfigManager;
import com.allfire.regionbetter.flags.RegionBetterViewFlag;
import com.allfire.regionbetter.listeners.PlayerListener;
import com.allfire.regionbetter.managers.RegionBetterManager;
import com.allfire.regionbetter.managers.SelectionManager;
import com.allfire.regionbetter.placeholders.RegionBetterPlaceholders;
import com.allfire.regionbetter.utils.WorldEditUtils;
import com.allfire.regionbetter.utils.ColorUtils;
import com.sk89q.worldguard.WorldGuard;
import org.bukkit.plugin.java.JavaPlugin;

public class RegionBetter extends JavaPlugin {

    private static RegionBetter instance;
    private ConfigManager configManager;
    private RegionBetterManager regionManager;
    private SelectionManager selectionManager;
    private WorldEditUtils worldEditUtils;
    private RegionBetterViewFlag regionBetterViewFlag;

    @Override
    public void onLoad() {
        // Регистрация флага regionbetter-view в onLoad()
        try {
            regionBetterViewFlag = new RegionBetterViewFlag("regionbetter-view");
            WorldGuard.getInstance().getFlagRegistry().register(regionBetterViewFlag);
            getLogger().info("Флаг regionbetter-view зарегистрирован в onLoad!");
        } catch (Exception e) {
            getLogger().warning("Ошибка при регистрации флага в onLoad: " + e.getMessage());
        }
    }

    @Override
    public void onEnable() {
        instance = this;

        // Инициализация менеджеров
        this.configManager = new ConfigManager(this);
        this.regionManager = new RegionBetterManager(this);
        this.selectionManager = new SelectionManager(this);
        this.worldEditUtils = new WorldEditUtils(this);
        
        // Устанавливаем WorldEditUtils в SelectionManager
        this.selectionManager.setWorldEditUtils(this.worldEditUtils);

        // Загрузка конфигурации
        configManager.loadConfig();

        // Регистрация команд
        getCommand("region").setExecutor(new RegionCommand(this));
        
        // Переопределение команд WorldGuard
        // overrideWorldGuardCommands(); // Отключено - возвращаем команды WorldGuard

        // Регистрация событий
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // Регистрация PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new RegionBetterPlaceholders(this).register();
            getLogger().info("PlaceholderAPI зарегистрирован!");
        } else {
            getLogger().warning("PlaceholderAPI не найден! Заполнители не будут работать.");
        }

        getLogger().info("RegionBetter успешно загружен!");
    }

    @Override
    public void onDisable() {
        getLogger().info("RegionBetter отключен!");
    }

    public static RegionBetter getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public RegionBetterManager getRegionManager() {
        return regionManager;
    }

    public SelectionManager getSelectionManager() {
        return selectionManager;
    }
    
    public WorldEditUtils getWorldEditUtils() {
        return worldEditUtils;
    }

    public RegionBetterViewFlag getRegionBetterViewFlag() {
        return regionBetterViewFlag;
    }
    
    public com.allfire.regionbetter.utils.CommandTrigger getCommandTrigger() {
        return new com.allfire.regionbetter.utils.CommandTrigger(this);
    }
    
    /**
     * ОТКЛЮЧЕНО: Переопределение команд WorldGuard
     * Возвращаем команды WorldGuard для корректной работы флагов в чат-меню
     */
    // ОТКЛЮЧЕНО: Возвращаем команды WorldGuard для корректной работы флагов
    /*
    private void overrideWorldGuardCommands() {
        try {
            // Переопределяем команду /rg через рефлексию
            org.bukkit.command.Command rgCommand = getCommand("region");
            if (rgCommand != null) {
                // Получаем CommandMap через рефлексию
                java.lang.reflect.Field commandMapField = getServer().getClass().getDeclaredField("commandMap");
                commandMapField.setAccessible(true);
                org.bukkit.command.CommandMap commandMap = (org.bukkit.command.CommandMap) commandMapField.get(getServer());
                
                // Создаем алиас для /rg
                commandMap.register("regionbetter", new org.bukkit.command.Command("rg") {
                    @Override
                    public boolean execute(org.bukkit.command.CommandSender sender, String label, String[] args) {
                        return rgCommand.execute(sender, "region", args);
                    }
                    
                    @Override
                    public java.util.List<String> tabComplete(org.bukkit.command.CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
                        return rgCommand.tabComplete(sender, "region", args);
                    }
                });
                getLogger().info("Переопределена команда /rg");
            }
        } catch (Exception e) {
            getLogger().warning("Не удалось переопределить команды WorldGuard: " + e.getMessage());
        }
    }
    */
}
