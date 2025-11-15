package com.allfire.regionbetter.managers;

import com.allfire.regionbetter.RegionBetter;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RegionBetterManager {
    
    private final RegionBetter plugin;
    
    public RegionBetterManager(RegionBetter plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Создает новый регион
     */
    public boolean createRegion(Player player, String regionName, Location pos1, Location pos2) {
        try {
            RegionManager regionManager = WorldGuard.getInstance().getPlatform()
                .getRegionContainer().get(BukkitAdapter.adapt(player.getWorld()));
            
            if (regionManager == null) {
                return false;
            }
            
            // Проверка на существование региона
            if (regionManager.hasRegion(regionName)) {
                return false; // Регион уже существует
            }
            
            // ========== ПРОВЕРКИ БЕЗОПАСНОСТИ ==========
            
            // 1. Проверка максимального количества блоков
            int selectionBlocks = calculateSelectionBlocks(pos1, pos2);
            if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                plugin.getLogger().info("Проверка блоков: " + selectionBlocks + " блоков в выделении");
            }
            if (!checkMaxBlocks(player, selectionBlocks)) {
                plugin.getLogger().warning("Превышен лимит блоков для игрока " + player.getName());
                return false; // Превышен лимит блоков
            }
            
            // 2. Проверка максимального количества регионов
            if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                plugin.getLogger().info("Проверка лимита регионов для игрока " + player.getName());
            }
            if (!checkMaxRegions(player)) {
                plugin.getLogger().warning("Превышен лимит регионов для игрока " + player.getName());
                return false; // Превышен лимит регионов
            }
            
            // 3. Проверка пересечения с другими регионами (перенесена в PlayerListener)

            BlockVector3 min = BlockVector3.at(
                Math.min(pos1.getBlockX(), pos2.getBlockX()),
                Math.min(pos1.getBlockY(), pos2.getBlockY()),
                Math.min(pos1.getBlockZ(), pos2.getBlockZ())
            );
            BlockVector3 max = BlockVector3.at(
                Math.max(pos1.getBlockX(), pos2.getBlockX()),
                Math.max(pos1.getBlockY(), pos2.getBlockY()),
                Math.max(pos1.getBlockZ(), pos2.getBlockZ())
            );

            ProtectedCuboidRegion region = new ProtectedCuboidRegion(regionName, min, max);
            
            // Set priority from config (как в eRegions)
            int priority = plugin.getConfigManager().getConfig().getInt("RegionSettints.Default-priority", 0);
            region.setPriority(priority);
            
            // Set owner (как в eRegions)
            region.getOwners().addPlayer(player.getUniqueId());
            
            // Add region to manager FIRST (как в eRegions)
            regionManager.addRegion(region);
            if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                plugin.getLogger().info("Регион добавлен в менеджер: " + regionName);
            }
            
            // Apply default flags from config (как в eRegions)
            List<String> defaultFlags = plugin.getConfigManager().getConfig().getStringList("RegionSettints.Default-flags");
            if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                plugin.getLogger().info("Применяем " + defaultFlags.size() + " флагов по умолчанию");
            }
            for (String flagString : defaultFlags) {
                String[] parts = flagString.split("=");
                if (parts.length == 2) {
                    String flagName = parts[0].trim();
                    String flagValue = parts[1].trim();
                    
                    // Add the flag to region
                    addRegionFlag(player.getWorld(), regionName, flagName, flagValue);
                }
            }
            
            // Автоматически устанавливаем флаг regionbetter-view если включено в конфиге
            if (plugin.getConfigManager().getConfig().getBoolean("RegionSettints.Instal-flags", false)) {
                try {
                    var flag = plugin.getRegionBetterViewFlag();
                    if (flag != null) {
                        region.setFlag(flag, com.sk89q.worldguard.protection.flags.StateFlag.State.ALLOW);
                        if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                            plugin.getLogger().info("Флаг regionbetter-view установлен");
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Не удалось установить флаг regionbetter-view: " + e.getMessage());
                }
            }
            
            // Set creator flag (как в eRegions)
            setRegionCreator(region, player.getName());
            if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                plugin.getLogger().info("Creator flag установлен");
            }
            
            // Save changes to WorldGuard (как в eRegions)
            regionManager.save();
            if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                plugin.getLogger().info("Изменения сохранены в WorldGuard");
            }
            
            if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                plugin.getLogger().info("Регион " + regionName + " успешно создан игроком " + player.getName() + " в мире " + player.getWorld().getName());
            }
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при создании региона: " + e.getMessage());
            e.printStackTrace(); // Добавляем полный стек трейс
            return false;
        }
    }
    
    /**
     * Удаляет регион
     */
    public boolean deleteRegion(Player player, String regionName) {
        try {
            RegionManager regionManager = WorldGuard.getInstance().getPlatform()
                .getRegionContainer().get(BukkitAdapter.adapt(player.getWorld()));
            
            if (regionManager == null) {
                return false;
            }
            
            ProtectedRegion region = regionManager.getRegion(regionName);
            if (region == null) {
                return false;
            }
            
            // Проверяем, является ли игрок владельцем
            if (!region.getOwners().contains(player.getUniqueId())) {
                return false;
            }

            regionManager.removeRegion(regionName);
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при удалении региона: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Добавляет владельца в регион
     */
    public boolean addOwner(Player player, String regionName, String targetPlayerName) {
        try {
            RegionManager regionManager = WorldGuard.getInstance().getPlatform()
                .getRegionContainer().get(BukkitAdapter.adapt(player.getWorld()));
            
            if (regionManager == null) {
                return false;
            }
            
            ProtectedRegion region = regionManager.getRegion(regionName);
            if (region == null) {
                return false;
            }
            
            // Проверяем, является ли игрок владельцем
            if (!region.getOwners().contains(player.getUniqueId())) {
                    return false;
                }

            // Получаем UUID целевого игрока
            UUID targetUUID = Bukkit.getOfflinePlayer(targetPlayerName).getUniqueId();
                region.getOwners().addPlayer(targetUUID);
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при добавлении владельца: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Добавляет участника в регион
     */
    public boolean addMember(Player player, String regionName, String targetPlayerName) {
        try {
            RegionManager regionManager = WorldGuard.getInstance().getPlatform()
                .getRegionContainer().get(BukkitAdapter.adapt(player.getWorld()));
            
            if (regionManager == null) {
                return false;
            }
            
            ProtectedRegion region = regionManager.getRegion(regionName);
            if (region == null) {
                return false;
            }
            
            // Проверяем, является ли игрок владельцем
            if (!region.getOwners().contains(player.getUniqueId())) {
                    return false;
                }

            // Получаем UUID целевого игрока
            UUID targetUUID = Bukkit.getOfflinePlayer(targetPlayerName).getUniqueId();
                region.getMembers().addPlayer(targetUUID);
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при добавлении участника: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Удаляет владельца из региона
     */
    public boolean removeOwner(Player player, String regionName, String targetPlayerName) {
        try {
            RegionManager regionManager = WorldGuard.getInstance().getPlatform()
                .getRegionContainer().get(BukkitAdapter.adapt(player.getWorld()));
            
            if (regionManager == null) {
                return false;
            }
            
            ProtectedRegion region = regionManager.getRegion(regionName);
            if (region == null) {
                return false;
            }
            
            // Проверяем, является ли игрок владельцем
            if (!region.getOwners().contains(player.getUniqueId())) {
                    return false;
                }

            // Получаем UUID целевого игрока
            UUID targetUUID = Bukkit.getOfflinePlayer(targetPlayerName).getUniqueId();
                region.getOwners().removePlayer(targetUUID);
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при удалении владельца: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Удаляет участника из региона
     */
    public boolean removeMember(Player player, String regionName, String targetPlayerName) {
        try {
            RegionManager regionManager = WorldGuard.getInstance().getPlatform()
                .getRegionContainer().get(BukkitAdapter.adapt(player.getWorld()));
            
            if (regionManager == null) {
                return false;
            }
            
            ProtectedRegion region = regionManager.getRegion(regionName);
            if (region == null) {
                return false;
            }
            
            // Проверяем, является ли игрок владельцем
            if (!region.getOwners().contains(player.getUniqueId())) {
                    return false;
                }

            // Получаем UUID целевого игрока
            UUID targetUUID = Bukkit.getOfflinePlayer(targetPlayerName).getUniqueId();
                region.getMembers().removePlayer(targetUUID);
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при удалении участника: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Получает информацию о регионе
     */
    public RegionInfo getRegionInfo(Player player, String regionName) {
        try {
            RegionManager regionManager = WorldGuard.getInstance().getPlatform()
                .getRegionContainer().get(BukkitAdapter.adapt(player.getWorld()));

            if (regionManager == null) {
                return null;
            }

            ProtectedRegion region = regionManager.getRegion(regionName);
            if (region == null) {
                return null;
            }

            return new RegionInfo(region, player);

        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при получении информации о регионе: " + e.getMessage());
            return null;
        }
    }

    /**
     * Проверяет существование региона
     */
    public boolean regionExists(String regionName) {
        try {
            // Получаем RegionManager для первого мира на сервере
            RegionManager regionManager = WorldGuard.getInstance().getPlatform()
                .getRegionContainer().get(BukkitAdapter.adapt(plugin.getServer().getWorlds().get(0)));

            return regionManager != null && regionManager.hasRegion(regionName);
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при проверке существования региона: " + e.getMessage());
            return false;
        }
    }

    /**
     * Получает регионы игрока
     */
    public List<String> getPlayerRegions(Player player, boolean owned, boolean member) {
        List<String> regions = new ArrayList<>();
        
        try {
            RegionManager regionManager = WorldGuard.getInstance().getPlatform()
                .getRegionContainer().get(BukkitAdapter.adapt(player.getWorld()));
            
            if (regionManager == null) {
                return regions;
            }
            
            for (ProtectedRegion region : regionManager.getRegions().values()) {
                if (owned && region.getOwners().contains(player.getUniqueId())) {
                        regions.add(region.getId());
                } else if (member && region.getMembers().contains(player.getUniqueId())) {
                        regions.add(region.getId());
                    }
                }

        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при получении регионов игрока: " + e.getMessage());
        }
        
        return regions;
    }
    
    /**
     * Класс для хранения информации о регионе
     */
    public static class RegionInfo {
        private final ProtectedRegion region;
        private final Player player;

        public RegionInfo(ProtectedRegion region, Player player) {
            this.region = region;
            this.player = player;
        }

        public String getName() {
                    return region.getId();
                }

        public List<String> getOwners() {
            List<String> owners = new ArrayList<>();
            for (UUID uuid : region.getOwners().getUniqueIds()) {
                owners.add(Bukkit.getOfflinePlayer(uuid).getName());
            }
            return owners;
        }

        public List<String> getMembers() {
            List<String> members = new ArrayList<>();
            for (UUID uuid : region.getMembers().getUniqueIds()) {
                members.add(Bukkit.getOfflinePlayer(uuid).getName());
            }
            return members;
        }

        public List<String> getFlags() {
            List<String> flags = new ArrayList<>();
            for (com.sk89q.worldguard.protection.flags.Flag<?> flag : region.getFlags().keySet()) {
                flags.add(flag.getName() + "=" + region.getFlag(flag));
            }
            return flags;
        }

        public int getVolume() {
            // Вычисляем объем вручную, так как getVolume() не существует в WorldGuard API
            try {
                // Получаем минимальные и максимальные координаты
                com.sk89q.worldedit.math.BlockVector3 min = region.getMinimumPoint();
                com.sk89q.worldedit.math.BlockVector3 max = region.getMaximumPoint();
                
                // Вычисляем объем: (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1)
                int width = max.x() - min.x() + 1;
                int height = max.y() - min.y() + 1;
                int length = max.z() - min.z() + 1;
                
                return width * height * length;
        } catch (Exception e) {
                // Если что-то пошло не так, возвращаем 0
                return 0;
            }
        }

        public boolean isOwner() {
            return region.getOwners().contains(player.getUniqueId());
        }

        public boolean isMember() {
            return region.getMembers().contains(player.getUniqueId());
        }
    }
    
    /**
     * Вычисляет количество блоков в выделении
     */
    private int calculateSelectionBlocks(Location pos1, Location pos2) {
        int width = Math.abs(pos1.getBlockX() - pos2.getBlockX()) + 1;
        int height = Math.abs(pos1.getBlockY() - pos2.getBlockY()) + 1;
        int length = Math.abs(pos1.getBlockZ() - pos2.getBlockZ()) + 1;
        return width * height * length;
    }
    
    /**
     * Проверяет максимальное количество блоков
     */
    private boolean checkMaxBlocks(Player player, int selectionBlocks) {
        // Получаем максимальное количество блоков из прав LuckPerms
        int maxBlocks = getMaxBlocksFromPermissions(player);
        
        if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
            plugin.getLogger().info("Проверка блоков: " + selectionBlocks + " блоков в выделении");
            plugin.getLogger().info("Максимальное количество блоков для игрока " + player.getName() + ": " + maxBlocks);
            
            // Отладочная информация о правах игрока
            plugin.getLogger().info("Права игрока " + player.getName() + " на блоки:");
            for (int i = 10; i <= 10000; i += 1000) {
                if (player.hasPermission("regionbetter.selection." + i)) {
                    plugin.getLogger().info("  - regionbetter.selection." + i + ": ЕСТЬ");
                }
            }
        }
        
        if (maxBlocks > 0 && selectionBlocks > maxBlocks) {
            plugin.getLogger().warning("Превышен лимит блоков: " + selectionBlocks + " > " + maxBlocks);
            plugin.getLogger().warning("Превышен лимит блоков для игрока " + player.getName());
            return false;
        }
        
        return true;
    }
    
    /**
     * Проверяет максимальное количество регионов
     */
    private boolean checkMaxRegions(Player player) {
        // Получаем максимальное количество регионов из прав LuckPerms
        int maxRegions = getMaxRegionsFromPermissions(player);
        
        if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
            plugin.getLogger().info("Максимальное количество регионов для игрока " + player.getName() + ": " + maxRegions);
        }
        
        if (maxRegions > 0) {
            int currentRegions = getPlayerRegions(player, true, true).size();
            if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                plugin.getLogger().info("Текущее количество регионов игрока " + player.getName() + ": " + currentRegions);
            }
            
            if (currentRegions >= maxRegions) {
                plugin.getLogger().warning("Превышен лимит регионов: " + currentRegions + " >= " + maxRegions);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Проверяет пересечение с другими регионами
     */
    private boolean checkRegionOverlap(RegionManager regionManager, Location pos1, Location pos2) {
        // Создаем временный регион для проверки пересечений
        com.sk89q.worldedit.math.BlockVector3 min = com.sk89q.worldedit.math.BlockVector3.at(
            Math.min(pos1.getBlockX(), pos2.getBlockX()),
            Math.min(pos1.getBlockY(), pos2.getBlockY()),
            Math.min(pos1.getBlockZ(), pos2.getBlockZ())
        );
        com.sk89q.worldedit.math.BlockVector3 max = com.sk89q.worldedit.math.BlockVector3.at(
            Math.max(pos1.getBlockX(), pos2.getBlockX()),
            Math.max(pos1.getBlockY(), pos2.getBlockY()),
            Math.max(pos1.getBlockZ(), pos2.getBlockZ())
        );
        
        ProtectedCuboidRegion tempRegion = new ProtectedCuboidRegion("temp", min, max);
        
        // Проверяем пересечение с каждым существующим регионом
        for (ProtectedRegion existingRegion : regionManager.getRegions().values()) {
            if (regionsOverlap(tempRegion, existingRegion)) {
                return false; // Найдено пересечение
            }
        }
        
        return true; // Пересечений нет
    }
    
    /**
     * Получает список регионов, которые пересекаются с выделенной областью
     * 
     * @param world Мир
     * @param pos1 Первая позиция
     * @param pos2 Вторая позиция
     * @return Список названий пересекающихся регионов
     */
    public List<String> getOverlappingRegions(World world, Location pos1, Location pos2) {
        List<String> overlappingRegions = new ArrayList<>();
        
        try {
            if (!isWorldGuardAvailable()) {
                return overlappingRegions;
            }
            
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regions = container.get(BukkitAdapter.adapt(world));
            
            if (regions == null) {
                return overlappingRegions;
            }
            
            // Convert Bukkit locations to WorldEdit BlockVector3
            BlockVector3 min = BlockVector3.at(
                Math.min(pos1.getBlockX(), pos2.getBlockX()),
                Math.min(pos1.getBlockY(), pos2.getBlockY()),
                Math.min(pos1.getBlockZ(), pos2.getBlockZ())
            );
            
            BlockVector3 max = BlockVector3.at(
                Math.max(pos1.getBlockX(), pos2.getBlockX()),
                Math.max(pos1.getBlockY(), pos2.getBlockY()),
                Math.max(pos1.getBlockZ(), pos2.getBlockZ())
            );
            
            // Create temporary region for comparison
            ProtectedCuboidRegion tempRegion = new ProtectedCuboidRegion("temp", min, max);
            
            // Check all existing regions for overlap
            for (ProtectedRegion existingRegion : regions.getRegions().values()) {
                // Check if regions overlap by comparing their bounds
                if (regionsOverlap(tempRegion, existingRegion)) {
                    overlappingRegions.add(existingRegion.getId());
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при проверке пересечения регионов: " + e.getMessage());
        }
        
        return overlappingRegions;
    }

    /**
     * Проверяет пересечение двух регионов (как в eRegions)
     */
    private boolean regionsOverlap(ProtectedCuboidRegion region1, ProtectedRegion region2) {
        try {
            // Get bounds of both regions
            BlockVector3 min1 = region1.getMinimumPoint();
            BlockVector3 max1 = region1.getMaximumPoint();
            
            BlockVector3 min2 = region2.getMinimumPoint();
            BlockVector3 max2 = region2.getMaximumPoint();
            
            // Check if regions overlap in 3D space
            // Two regions overlap if they overlap in all three dimensions (X, Y, Z)
            boolean xOverlap = (min1.x() <= max2.x()) && (max1.x() >= min2.x());
            boolean yOverlap = (min1.y() <= max2.y()) && (max1.y() >= min2.y());
            boolean zOverlap = (min1.z() <= max2.z()) && (max1.z() >= min2.z());
            
            return xOverlap && yOverlap && zOverlap;
            
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при проверке пересечения регионов: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Получает максимальное количество блоков из прав LuckPerms
     */
    private int getMaxBlocksFromPermissions(Player player) {
        int maxBlocks = 0;
        
        // Проверяем права от больших значений к меньшим
        // regionbetter.selection.999999999999 - максимальное значение
        for (int i = 999999999; i >= 10; i -= 1000) {
            if (player.hasPermission("regionbetter.selection." + i)) {
                maxBlocks = i;
                break;
            }
        }
        
        // Если не найдено, проверяем стандартные значения
        if (maxBlocks == 0) {
            for (int i = 1000; i >= 10; i -= 10) {
                if (player.hasPermission("regionbetter.selection." + i)) {
                    maxBlocks = i;
                    break;
                }
            }
        }
        
        return maxBlocks;
    }
    
    /**
     * Получает максимальное количество регионов из прав LuckPerms
     */
    private int getMaxRegionsFromPermissions(Player player) {
        int maxRegions = 0;
        
        // Проверяем права от больших значений к меньшим
        // regionbetter.region.9999999 - максимальное значение
        for (int i = 9999999; i >= 1000; i -= 1000) {
            if (player.hasPermission("regionbetter.region." + i)) {
                maxRegions = i;
                break;
            }
        }
        
        // Если не найдено, проверяем стандартные значения
        if (maxRegions == 0) {
            for (int i = 100; i >= 1; i--) {
                if (player.hasPermission("regionbetter.region." + i)) {
                    maxRegions = i;
                    break;
                }
            }
        }
        
        return maxRegions;
    }
    
    /**
     * Add flag to region (как в eRegions)
     */
    public boolean addRegionFlag(World world, String regionName, String flagName, String flagValue) {
        try {
            if (!isWorldGuardAvailable()) {
                return false;
            }

            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regions = container.get(BukkitAdapter.adapt(world));

            if (regions == null) {
                return false;
            }

            ProtectedRegion region = regions.getRegion(regionName);
            if (region == null) {
                return false;
            }

            // Find and set the flag
            var flagRegistry = WorldGuard.getInstance().getFlagRegistry();
            var flag = flagRegistry.get(flagName);
            if (flag != null) {
                // Handle different flag types
                if (flag instanceof com.sk89q.worldguard.protection.flags.StateFlag) {
                    var stateFlag = (com.sk89q.worldguard.protection.flags.StateFlag) flag;
                    if (flagValue.equalsIgnoreCase("allow") || flagValue.equalsIgnoreCase("true")) {
                        region.setFlag(stateFlag, com.sk89q.worldguard.protection.flags.StateFlag.State.ALLOW);
                    } else {
                        region.setFlag(stateFlag, com.sk89q.worldguard.protection.flags.StateFlag.State.DENY);
                    }
                } else if (flag instanceof com.sk89q.worldguard.protection.flags.StringFlag) {
                    region.setFlag((com.sk89q.worldguard.protection.flags.StringFlag) flag, flagValue);
                } else if (flag instanceof com.sk89q.worldguard.protection.flags.IntegerFlag) {
                    try {
                        int intValue = Integer.parseInt(flagValue);
                        region.setFlag((com.sk89q.worldguard.protection.flags.IntegerFlag) flag, intValue);
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("Invalid integer value for flag " + flagName + ": " + flagValue);
                        return false;
                    }
                }
                regions.save();
                return true;
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при добавлении флага в регион: " + regionName + ", флаг: " + flagName + ", значение: " + flagValue + ", ошибка: " + e.getMessage());
            return false;
        }
        return false;
    }
    
    /**
     * Получает все регионы в мире (для табуляции и других целей)
     */
    public List<String> getAllRegionsInWorld(World world) {
        List<String> regions = new ArrayList<>();
        
        try {
            // Прямой доступ к WorldGuard API
            com.sk89q.worldguard.WorldGuard worldGuard = com.sk89q.worldguard.WorldGuard.getInstance();
            com.sk89q.worldguard.protection.regions.RegionContainer container = worldGuard.getPlatform().getRegionContainer();
            com.sk89q.worldguard.protection.managers.RegionManager regionManager = container.get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world));
            
            if (regionManager != null) {
                // Получаем все регионы в мире
                for (String regionName : regionManager.getRegions().keySet()) {
                    regions.add(regionName);
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при получении списка регионов в мире: " + e.getMessage());
        }
        
        return regions;
    }
    
    /**
     * Получает детальную информацию о регионе для команды info
     */
    public RegionDetailedInfo getRegionDetailedInfo(Player player, String regionName) {
        try {
            // Прямой доступ к WorldGuard API
            com.sk89q.worldguard.WorldGuard worldGuard = com.sk89q.worldguard.WorldGuard.getInstance();
            com.sk89q.worldguard.protection.regions.RegionContainer container = worldGuard.getPlatform().getRegionContainer();
            com.sk89q.worldguard.protection.managers.RegionManager regionManager = container.get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(player.getWorld()));
            
            if (regionManager == null) return null;
            
            com.sk89q.worldguard.protection.regions.ProtectedRegion region = regionManager.getRegion(regionName);
            if (region == null) return null;
            
            return new RegionDetailedInfo(region, player);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при получении детальной информации о регионе: " + e.getMessage());
        return null;
    }
    }
    
    /**
     * Класс для хранения детальной информации о регионе
     */
    public static class RegionDetailedInfo {
        private final com.sk89q.worldguard.protection.regions.ProtectedRegion region;
        private final Player player;
        
        public RegionDetailedInfo(com.sk89q.worldguard.protection.regions.ProtectedRegion region, Player player) {
            this.region = region;
            this.player = player;
        }
        
        public boolean isOwner() {
            return region.getOwners().contains(player.getUniqueId());
        }
        
        public boolean isMember() {
            return region.getMembers().contains(player.getUniqueId());
        }
        
        public String getOwners() {
            java.util.Set<String> ownerNames = new java.util.HashSet<>();
            for (java.util.UUID uuid : region.getOwners().getUniqueIds()) {
                String name = Bukkit.getOfflinePlayer(uuid).getName();
                if (name != null) ownerNames.add(name);
            }
            return String.join(", ", ownerNames);
        }
        
        public String getMembers() {
            java.util.Set<String> memberNames = new java.util.HashSet<>();
            for (java.util.UUID uuid : region.getMembers().getUniqueIds()) {
                String name = Bukkit.getOfflinePlayer(uuid).getName();
                if (name != null) memberNames.add(name);
            }
            return String.join(", ", memberNames);
        }
        
        public String getFlags() {
            java.util.List<String> flagList = new java.util.ArrayList<>();
            var flagRegistry = com.sk89q.worldguard.WorldGuard.getInstance().getFlagRegistry();
            
            for (var flag : flagRegistry.getAll()) {
                if (flag instanceof com.sk89q.worldguard.protection.flags.StateFlag) {
                    var stateFlag = (com.sk89q.worldguard.protection.flags.StateFlag) flag;
                    var state = region.getFlag(stateFlag);
                    if (state != null) {
                        String color = state.equals(com.sk89q.worldguard.protection.flags.StateFlag.State.ALLOW) ? "&a" : "&c";
                        flagList.add(color + flag.getName() + "=" + state.toString().toLowerCase());
                    }
                }
            }
            
            return String.join("&7, ", flagList);
        }
        
        public String getPos1() {
            var min = region.getMinimumPoint();
            return min.x() + ", " + min.y() + ", " + min.z();
        }
        
        public String getPos2() {
            var max = region.getMaximumPoint();
            return max.x() + ", " + max.y() + ", " + max.z();
        }
    }
    
    /**
     * Set region creator (как в eRegions)
     */
    public void setRegionCreator(ProtectedRegion region, String creatorName) {
        try {
            // Try to set creator flag if available
            var flagRegistry = WorldGuard.getInstance().getFlagRegistry();
            var creatorFlag = flagRegistry.get("regionbetter-creator");
            if (creatorFlag instanceof com.sk89q.worldguard.protection.flags.StringFlag) {
                region.setFlag((com.sk89q.worldguard.protection.flags.StringFlag) creatorFlag, creatorName);
            }
        } catch (Exception e) {
            // Ignore if creator flag is not available
        }
    }
    
    /**
     * Check if WorldGuard is available
     * 
     * @return True if available
     */
    public boolean isWorldGuardAvailable() {
        try {
            return plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null;
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при проверке доступности WorldGuard: " + e.getMessage());
            return false;
        }
    }
}