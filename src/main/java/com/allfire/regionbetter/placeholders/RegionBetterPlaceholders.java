package com.allfire.regionbetter.placeholders;

import com.allfire.regionbetter.RegionBetter;
import com.allfire.regionbetter.managers.RegionBetterManager;
import com.allfire.regionbetter.managers.SelectionManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RegionBetterPlaceholders extends PlaceholderExpansion {

    private final RegionBetter plugin;

    public RegionBetterPlaceholders(RegionBetter plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "regionbetter";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        // Регионы игрока (owned)
        if (params.startsWith("region_owned_v")) {
            String version = params.substring("region_owned_v".length());
            List<String> regions = plugin.getRegionManager().getPlayerRegions(player, true, false);
            return String.join(getFormat("region_owned_v" + version), regions);
        }

        // Регионы игрока (member)
        if (params.startsWith("region_membed_v")) {
            String version = params.substring("region_membed_v".length());
            List<String> regions = plugin.getRegionManager().getPlayerRegions(player, false, true);
            return String.join(getFormat("region_membed_v" + version), regions);
        }

        // Отдельные регионы (owned)
        if (params.startsWith("region_owned_")) {
            String index = params.substring("region_owned_".length());
            List<String> regions = plugin.getRegionManager().getPlayerRegions(player, true, false);
            try {
                int idx = Integer.parseInt(index) - 1;
                if (idx >= 0 && idx < regions.size()) {
                    return regions.get(idx);
                }
            } catch (NumberFormatException e) {
                // Неверный индекс
            }
            return "";
        }

        // Отдельные регионы (member)
        if (params.startsWith("region_membed_")) {
            String index = params.substring("region_membed_".length());
            List<String> regions = plugin.getRegionManager().getPlayerRegions(player, false, true);
            try {
                int idx = Integer.parseInt(index) - 1;
                if (idx >= 0 && idx < regions.size()) {
                    return regions.get(idx);
                }
            } catch (NumberFormatException e) {
                // Неверный индекс
            }
            return "";
        }

        // Название текущего региона
        if (params.equals("region_name")) {
            // TODO: Реализовать получение региона в котором находится игрок
            return "";
        }

        // Количество блоков в регионе
        if (params.equals("region_blocks")) {
            // TODO: Реализовать получение количества блоков в текущем регионе
            return "0";
        }

        // Количество блоков в выделении
        if (params.equals("selection_blocks")) {
            return String.valueOf(plugin.getSelectionManager().getSelectionBlocks(player));
        }

        // Максимальное количество блоков в выделении
        if (params.equals("selection_max")) {
            return getMaxBlocks(player);
        }

        // Количество регионов игрока (owned)
        if (params.equals("region_owned")) {
            return String.valueOf(plugin.getRegionManager().getPlayerRegions(player, true, false).size());
        }

        // Количество регионов игрока (member)
        if (params.equals("region_membed")) {
            return String.valueOf(plugin.getRegionManager().getPlayerRegions(player, false, true).size());
        }

        // Максимальное количество регионов
        if (params.equals("region_max")) {
            return getMaxRegions(player);
        }

        // Дистанция до ближайшего региона (owned)
        if (params.equals("distance_owned")) {
            // TODO: Реализовать расчет дистанции
            return "0";
        }

        // Название ближайшего региона (owned)
        if (params.equals("distance_owned_name")) {
            // TODO: Реализовать получение названия ближайшего региона
            return "";
        }

        // Дистанция до ближайшего региона (member)
        if (params.equals("distance_membed")) {
            // TODO: Реализовать расчет дистанции
            return "0";
        }

        // Название ближайшего региона (member)
        if (params.equals("distance_membed_name")) {
            // TODO: Реализовать получение названия ближайшего региона
            return "";
        }

        // Магазин регионов
        if (params.equals("shop_regions")) {
            // TODO: Реализовать магазин регионов
            return "";
        }

        if (params.equals("shop_cost")) {
            // TODO: Реализовать стоимость региона
            return "0";
        }

        if (params.equals("shop_cost_symbol")) {
            return plugin.getConfigManager().getConfig().getString("RegionShop.Symbol", "$");
        }

        if (params.equals("shop_start")) {
            // TODO: Реализовать время начала продажи
            return "";
        }

        if (params.equals("shop_end")) {
            // TODO: Реализовать время окончания продажи
            return "";
        }

        // Статус подсветки
        if (params.equals("region_view")) {
            return String.valueOf(player.hasPermission("regionbetter.region.view"));
        }

        // Координаты выделения
        if (params.equals("selection_pos1")) {
            return plugin.getSelectionManager().getPos1String(player);
        }

        if (params.equals("selection_pos2")) {
            return plugin.getSelectionManager().getPos2String(player);
        }

        return null;
    }

    private String getFormat(String key) {
        return plugin.getConfigManager().getConfig().getString(key, ",");
    }

    private String getMaxBlocks(Player player) {
        // Получаем максимальное количество блоков из прав LuckPerms
        int maxBlocks = 0;
        for (int i = 1; i <= 1000000; i++) {
            if (player.hasPermission("regionbetter.selection." + i)) {
                maxBlocks = i;
            }
        }
        return String.valueOf(maxBlocks);
    }

    private String getMaxRegions(Player player) {
        // Получаем максимальное количество регионов из прав LuckPerms
        int maxRegions = 0;
        for (int i = 1; i <= 10000; i++) {
            if (player.hasPermission("regionbetter.region." + i)) {
                maxRegions = i;
            }
        }
        return String.valueOf(maxRegions);
    }
}