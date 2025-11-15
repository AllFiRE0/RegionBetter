package com.allfire.regionbetter.utils;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    /**
     * Отправляет цветное сообщение игроку
     */
    public static void sendColoredMessage(Player player, String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }

        // Отправляем как legacy сообщение
        String processedMessage = processColors(message);
        player.sendMessage(processedMessage);
    }

    /**
     * Обрабатывает цвета в сообщении (Legacy)
     */
    public static String processColors(String message) {
        if (message == null) {
            return "";
        }

        // Обрабатываем HEX цвета &#RRGGBB (упрощенная версия)
        Matcher hexMatcher = HEX_PATTERN.matcher(message);
        while (hexMatcher.find()) {
            String hexColor = hexMatcher.group(1);
            // Пока что просто убираем HEX цвета, так как Bukkit не поддерживает их напрямую
            message = message.replace(hexMatcher.group(0), "");
        }

        // Используем стандартные цвета Bukkit
        return ChatColor.translateAlternateColorCodes('&', message);
    }

}