package com.allfire.regionbetter.utils;

import com.allfire.regionbetter.RegionBetter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CommandTrigger {

    private final RegionBetter plugin;
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    public CommandTrigger(RegionBetter plugin) {
        this.plugin = plugin;
    }

    /**
     * Выполняет команды с задержкой
     */
    public void executeCommandsWithDelay(Player player, String triggerName, String[] placeholders) {
        List<String> commands = plugin.getConfigManager().getConfig().getStringList(triggerName + ".Cmds");
        int cooldown = plugin.getConfigManager().getConfig().getInt(triggerName + ".Cooldown", 0);

        // Проверка кулдауна
        String cooldownKey = player.getUniqueId().toString() + ":" + triggerName;
        if (cooldowns.containsKey(cooldownKey)) {
            long lastExecution = cooldowns.get(cooldownKey);
            if (System.currentTimeMillis() - lastExecution < cooldown * 50) { // 50ms = 1 тик
                return;
            }
        }

        cooldowns.put(cooldownKey, System.currentTimeMillis());

        // Выполняем команды
        for (String command : commands) {
            if (command == null || command.trim().isEmpty()) {
                continue;
            }

            String processedCommand = processPlaceholders(command, placeholders, player);
            
            if (processedCommand.startsWith("asPlayer!")) {
                String playerCommand = processedCommand.substring(9).trim();
                if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                    plugin.getLogger().info("[DEBUG] asPlayer! команда: " + playerCommand + " для игрока " + player.getName());
                }
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.performCommand(playerCommand);
                });
            } else if (processedCommand.startsWith("asPlayerForce!")) {
                String playerCommand = processedCommand.substring(14).trim();
                if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                    plugin.getLogger().info("[DEBUG] asPlayerForce! команда: " + playerCommand + " для игрока " + player.getName());
                }
                Bukkit.getScheduler().runTask(plugin, () -> {
                    // Принудительное выполнение команды от имени игрока
                    // Временно выдаем все права игроку для обхода любых ограничений
                    org.bukkit.permissions.PermissionAttachment attachment = player.addAttachment(plugin);
                    
                    // Выдаем базовые права для обхода ограничений (безопасно)
                    attachment.setPermission("command", true);
                    
                    // Извлекаем команду из строки
                    String[] parts = playerCommand.split(" ", 2);
                    if (parts.length >= 1) {
                        String cmdName = parts[0];
                        
                        // Добавляем права на конкретную команду
                        attachment.setPermission(cmdName, true);
                        
                        // Для популярных плагинов добавляем их права
                        if (cmdName.equals("svis")) {
                            attachment.setPermission("svis.use", true);
                            attachment.setPermission("svis.admin", true);
                        } else if (cmdName.equals("worldedit") || cmdName.equals("we")) {
                            attachment.setPermission("worldedit.use", true);
                        } else if (cmdName.equals("cmi")) {
                            attachment.setPermission("cmi.command.point", true);
                            attachment.setPermission("cmi.command.point.trial_spawner_detection", true);
                            attachment.setPermission("cmi.command.point.trial_spawner_detection_ominous", true);
                        } else if (cmdName.equals("give")) {
                            attachment.setPermission("minecraft.command.give", true);
                        } else if (cmdName.equals("tp") || cmdName.equals("teleport")) {
                            attachment.setPermission("minecraft.command.teleport", true);
                        }
                        
                        // Выполняем команду
                        player.performCommand(playerCommand);
                        
                        // Удаляем права через 2 тика (больше времени для выполнения)
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            player.removeAttachment(attachment);
                        }, 2L);
                    }
                });
            } else if (processedCommand.startsWith("asConsole!")) {
                String consoleCommand = processedCommand.substring(10).trim();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), consoleCommand);
                });
            } else if (processedCommand.startsWith("msg!")) {
                String message = processedCommand.substring(4).trim();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    // Отправляем сообщение (MiniMessage или Legacy)
                    ColorUtils.sendColoredMessage(player, message);
                });
            } else if (processedCommand.startsWith("delay!")) {
                try {
                    int delay = Integer.parseInt(processedCommand.substring(6).trim());
                    // Задержка уже учтена в цикле
                    try {
                        Thread.sleep(delay * 50); // 50ms = 1 тик
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Неверный формат задержки: " + processedCommand);
                }
            }
        }
    }

    /**
     * Обрабатывает заполнители в команде
     */
    private String processPlaceholders(String command, String[] placeholders, Player player) {
        String result = command;
        
        // Обрабатываем числовые заполнители {0}, {1}, {2} и т.д.
        for (int i = 0; i < placeholders.length; i++) {
            result = result.replace("{" + i + "}", placeholders[i]);
        }
        
        // Обрабатываем именованные заполнители
        result = result.replace("{region_name}", placeholders.length > 0 ? placeholders[0] : "");
        result = result.replace("{last_player}", placeholders.length > 1 ? placeholders[1] : "");
        result = result.replace("{block_move}", placeholders.length > 0 ? placeholders[0] : "");
        result = result.replace("{block_size}", placeholders.length > 0 ? placeholders[0] : "");
        result = result.replace("{direction}", placeholders.length > 0 ? placeholders[0] : "");
        
        // Умная логика для {blocks} - зависит от количества параметров
        if (placeholders.length == 1) {
            // Для команды move (один параметр)
            result = result.replace("{blocks}", placeholders[0]);
        } else if (placeholders.length >= 2) {
            // Для команды size (два параметра: direction, blocks)
            result = result.replace("{blocks}", placeholders[1]);
        } else {
            result = result.replace("{blocks}", "");
        }
        result = result.replace("{owners}", placeholders.length > 1 ? placeholders[1] : "");
        result = result.replace("{members}", placeholders.length > 2 ? placeholders[2] : "");
        result = result.replace("{flags}", placeholders.length > 3 ? placeholders[3] : "");
        result = result.replace("{pos1}", placeholders.length > 4 ? placeholders[4] : "");
        result = result.replace("{pos2}", placeholders.length > 5 ? placeholders[5] : "");
        
        // Обрабатываем Legacy цвета в результате
        result = com.allfire.regionbetter.utils.ColorUtils.processColors(result);
        
        // Кликабельные заполнители теперь обрабатываются через MiniMessage
        // Удаляем старую логику с JSON

        // Обрабатываем PlaceholderAPI заполнители (передаем игрока для правильной обработки)
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null && player != null) {
            result = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, result);
        }

        return result;
    }
    
    // Старая логика с кликабельными заполнителями удалена
    // Теперь используем MiniMessage для создания кликабельных элементов
}