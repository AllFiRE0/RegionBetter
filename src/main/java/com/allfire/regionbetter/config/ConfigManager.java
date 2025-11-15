package com.allfire.regionbetter.config;

import com.allfire.regionbetter.RegionBetter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class ConfigManager {

    private final RegionBetter plugin;
    private FileConfiguration config;
    private FileConfiguration language;
    private File configFile;
    private File languageFile;

    public ConfigManager(RegionBetter plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        // Создаем папку плагина если не существует
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        // Загружаем основной конфиг
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        // Загружаем языковой файл
        loadLanguageFile();

        plugin.getLogger().info("Конфигурация загружена!");
    }

    public void reloadConfig() {
        if (configFile != null && configFile.exists()) {
            config = YamlConfiguration.loadConfiguration(configFile);
        }

        // Перезагружаем языковой файл
        loadLanguageFile();

        plugin.getLogger().info("Конфигурация перезагружена!");
    }

    private void loadLanguageFile() {
        languageFile = new File(plugin.getDataFolder(), "language.yml");
        if (!languageFile.exists()) {
            try (InputStream in = plugin.getResource("language.yml")) {
                if (in != null) {
                    Files.copy(in, languageFile.toPath());
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Не удалось создать языковой файл: " + e.getMessage());
            }
        }
        language = YamlConfiguration.loadConfiguration(languageFile);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getLanguageConfig() {
        return language;
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить конфигурацию: " + e.getMessage());
        }
    }
}