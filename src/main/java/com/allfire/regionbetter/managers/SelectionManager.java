package com.allfire.regionbetter.managers;

import com.allfire.regionbetter.RegionBetter;
import com.allfire.regionbetter.utils.WorldEditUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SelectionManager {

    private final RegionBetter plugin;
    private WorldEditUtils worldEditUtils;
    private final Map<UUID, Location> pos1Map = new HashMap<>();
    private final Map<UUID, Location> pos2Map = new HashMap<>();

    // Система состояний для пошагового создания регионов
    private final Map<Player, SelectionData> activeSelections = new HashMap<>();
    private final Map<Player, SelectionData> waitingForName = new HashMap<>();

    public SelectionManager(RegionBetter plugin) {
        this.plugin = plugin;
        this.worldEditUtils = null; // Will be set later
    }
    
    /**
     * Set WorldEditUtils after initialization
     */
    public void setWorldEditUtils(WorldEditUtils worldEditUtils) {
        this.worldEditUtils = worldEditUtils;
    }

    /**
     * Класс для отслеживания процесса создания региона
     */
    public static class SelectionData {
        private final Player player;
        private final String type;
        private final long startTime;
        private Location pos1;
        private Location pos2;
        private boolean completed;

        public SelectionData(Player player, String type) {
            this.player = player;
            this.type = type;
            this.startTime = System.currentTimeMillis();
            this.completed = false;
        }

        // Геттеры и сеттеры
        public Player getPlayer() { return player; }
        public String getType() { return type; }
        public long getStartTime() { return startTime; }
        public Location getPos1() { return pos1; }
        public Location getPos2() { return pos2; }
        public boolean isCompleted() { return completed; }

        public void setPos1(Location pos1) { this.pos1 = pos1; }
        public void setPos2(Location pos2) { this.pos2 = pos2; }
        public void setCompleted(boolean completed) { this.completed = completed; }
    }

    /**
     * Устанавливает первую точку выделения
     */
    public void setPos1(Player player, Location location) {
        pos1Map.put(player.getUniqueId(), location);
        
        // Также устанавливаем в WorldEdit для совместимости с SelectionVisualizer
        if (worldEditUtils != null) {
            worldEditUtils.setSelection(player, location, location);
        }
    }

    /**
     * Устанавливает вторую точку выделения
     */
    public void setPos2(Player player, Location location) {
        pos2Map.put(player.getUniqueId(), location);
        
        // Также устанавливаем в WorldEdit для совместимости с SelectionVisualizer
        if (worldEditUtils != null) {
            Location pos1 = pos1Map.get(player.getUniqueId());
            if (pos1 != null) {
                worldEditUtils.setSelection(player, pos1, location);
            }
        }
    }

    /**
     * Получает первую точку выделения
     */
    public Location getPos1(Player player) {
        return pos1Map.get(player.getUniqueId());
    }

    /**
     * Получает вторую точку выделения
     */
    public Location getPos2(Player player) {
        return pos2Map.get(player.getUniqueId());
    }

    /**
     * Очищает выделение игрока
     */
    public void clearSelection(Player player) {
        pos1Map.remove(player.getUniqueId());
        pos2Map.remove(player.getUniqueId());
        
        // Очищаем состояния
        activeSelections.remove(player);
        waitingForName.remove(player);
        
        // Отключаем WorldEdit выделение
        if (worldEditUtils != null) {
            worldEditUtils.disableSelection(player);
        }
    }
    
    /**
     * Начать процесс выделения для игрока
     */
    public void startSelection(Player player, String type) {
        // Убираем предыдущее активное выделение, если оно есть
        if (hasActiveSelection(player)) {
            activeSelections.remove(player);
        }
        
        // Создаем данные выделения
        SelectionData selectionData = new SelectionData(player, type);
        activeSelections.put(player, selectionData);
        
        // Включаем WorldEdit выделение
        if (worldEditUtils != null) {
            worldEditUtils.enableSelection(player);
        }
    }
    
    /**
     * Завершить процесс выделения
     */
    public SelectionData completeSelection(Player player) {
        SelectionData selectionData = activeSelections.get(player);
        if (selectionData == null) {
            return null;
        }
        
        // Для команд select1/select2 завершаем сразу после установки одной точки
        if ("select1".equals(selectionData.getType()) || "select2".equals(selectionData.getType())) {
            selectionData.setCompleted(true);
            activeSelections.remove(player); // Убираем из активных выделений
            return selectionData;
        }
        
        // Для команды create требуем обе точки
        if ("create".equals(selectionData.getType()) && 
            selectionData.getPos1() != null && selectionData.getPos2() != null) {
            selectionData.setCompleted(true);
            waitingForName.put(player, selectionData);
            
            // Вызываем триггер для завершения выделения
            plugin.getCommandTrigger().executeCommandsWithDelay(player, "RegionBetterComplete", new String[]{});
            
            return selectionData;
        }
        
        return null;
    }
    
    /**
     * Проверить, есть ли активное выделение
     */
    public boolean hasActiveSelection(Player player) {
        return activeSelections.containsKey(player);
    }
    
    /**
     * Получить данные активного выделения
     */
    public SelectionData getActiveSelection(Player player) {
        return activeSelections.get(player);
    }
    
    /**
     * Проверить, ждет ли игрок ввода названия
     */
    public boolean isWaitingForName(Player player) {
        return waitingForName.containsKey(player);
    }
    
    /**
     * Получить данные ожидающего игрока
     */
    public SelectionData getWaitingForName(Player player) {
        return waitingForName.get(player);
    }
    
    /**
     * Удалить игрока из ожидания
     */
    public void removeWaitingForName(Player player) {
        waitingForName.remove(player);
    }
    
    /**
     * Установить игрока в ожидание названия
     */
    public void setWaitingForName(Player player, SelectionData selectionData) {
        waitingForName.put(player, selectionData);
    }
    
    /**
     * Очистить все состояния игрока
     */
    public void clearPlayerStates(Player player) {
        activeSelections.remove(player);
        waitingForName.remove(player);
        pos1Map.remove(player.getUniqueId());
        pos2Map.remove(player.getUniqueId());
    }

    /**
     * Проверяет, есть ли у игрока выделение
     */
    public boolean hasSelection(Player player) {
        return pos1Map.containsKey(player.getUniqueId()) && pos2Map.containsKey(player.getUniqueId());
    }

    /**
     * Получает количество блоков в выделении
     */
    public int getSelectionBlocks(Player player) {
        Location pos1 = getPos1(player);
        Location pos2 = getPos2(player);
        
        if (pos1 == null || pos2 == null) {
            return 0;
        }

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    /**
     * Перемещает выделение
     */
    public void moveSelection(Player player, int x, int y, int z) {
        Location pos1 = getPos1(player);
        Location pos2 = getPos2(player);
        
        if (pos1 != null && pos2 != null) {
            Location newPos1 = pos1.clone().add(x, y, z);
            Location newPos2 = pos2.clone().add(x, y, z);
            setPos1(player, newPos1);
            setPos2(player, newPos2);
        }
    }

    /**
     * Изменяет размер выделения
     */
    public void resizeSelection(Player player, String direction, int blocks) {
        Location pos1 = getPos1(player);
        Location pos2 = getPos2(player);
        
        if (pos1 == null || pos2 == null) {
            return;
        }

        Location newPos1 = pos1.clone();
        Location newPos2 = pos2.clone();

        switch (direction.toLowerCase()) {
            case "up":
                if (blocks > 0) {
                    // Положительное значение - расширяем вверх
                    newPos2.setY(Math.max(pos1.getY(), pos2.getY()) + blocks);
                } else {
                    // Отрицательное значение - сжимаем сверху
                    newPos2.setY(Math.max(pos1.getY(), pos2.getY()) + blocks);
                }
                break;
                
            case "down":
                if (blocks > 0) {
                    // Положительное значение - расширяем вниз
                    newPos1.setY(Math.min(pos1.getY(), pos2.getY()) - blocks);
                } else {
                    // Отрицательное значение - сжимаем снизу
                    newPos1.setY(Math.min(pos1.getY(), pos2.getY()) - blocks);
                }
                break;
                
            case "face":
                // Изменяем размер по 3D направлению взгляда
                org.bukkit.util.Vector faceDirection = player.getLocation().getDirection();
                
                double deltaX = faceDirection.getX() * blocks;
                double deltaY = faceDirection.getY() * blocks;
                double deltaZ = faceDirection.getZ() * blocks;
                
                if (blocks > 0) {
                    // Расширяем в направлении взгляда
                    if (deltaX > 0) {
                        newPos2.setX(Math.max(pos1.getX(), pos2.getX()) + deltaX);
                    } else if (deltaX < 0) {
                        newPos1.setX(Math.min(pos1.getX(), pos2.getX()) + deltaX);
                    }
                    
                    if (deltaY > 0) {
                        newPos2.setY(Math.max(pos1.getY(), pos2.getY()) + deltaY);
                    } else if (deltaY < 0) {
                        newPos1.setY(Math.min(pos1.getY(), pos2.getY()) + deltaY);
                    }
                    
                    if (deltaZ > 0) {
                        newPos2.setZ(Math.max(pos1.getZ(), pos2.getZ()) + deltaZ);
                    } else if (deltaZ < 0) {
                        newPos1.setZ(Math.min(pos1.getZ(), pos2.getZ()) + deltaZ);
                    }
                } else {
                    // Сжимаем в направлении взгляда
                    if (deltaX > 0) {
                        newPos1.setX(Math.min(pos1.getX(), pos2.getX()) + deltaX);
                    } else if (deltaX < 0) {
                        newPos2.setX(Math.max(pos1.getX(), pos2.getX()) + deltaX);
                    }
                    
                    if (deltaY > 0) {
                        newPos1.setY(Math.min(pos1.getY(), pos2.getY()) + deltaY);
                    } else if (deltaY < 0) {
                        newPos2.setY(Math.max(pos1.getY(), pos2.getY()) + deltaY);
                    }
                    
                    if (deltaZ > 0) {
                        newPos1.setZ(Math.min(pos1.getZ(), pos2.getZ()) + deltaZ);
                    } else if (deltaZ < 0) {
                        newPos2.setZ(Math.max(pos1.getZ(), pos2.getZ()) + deltaZ);
                    }
                }
                break;
        }

        setPos1(player, newPos1);
        setPos2(player, newPos2);
    }

    /**
     * Получает строковое представление первой точки
     */
    public String getPos1String(Player player) {
        Location pos1 = getPos1(player);
        if (pos1 == null) {
            return plugin.getConfigManager().getLanguageConfig().getString("InfoMessages.PositionNotSet", "&7Не установлена");
        }
        return pos1.getBlockX() + ", " + pos1.getBlockY() + ", " + pos1.getBlockZ();
    }

    /**
     * Получает строковое представление второй точки
     */
    public String getPos2String(Player player) {
        Location pos2 = getPos2(player);
        if (pos2 == null) {
            return plugin.getConfigManager().getLanguageConfig().getString("InfoMessages.PositionNotSet", "&7Не установлена");
        }
        return pos2.getBlockX() + ", " + pos2.getBlockY() + ", " + pos2.getBlockZ();
    }

    // ========== СИСТЕМА СОСТОЯНИЙ ДЛЯ ПОШАГОВОГО СОЗДАНИЯ ==========


    /**
     * Обрабатывает клик для выбора точки в активном выделении
     */
    public void handleSelectionClick(Player player, Location clickedLocation) {
        if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
            plugin.getLogger().info("[DEBUG] handleSelectionClick вызван для игрока " + player.getName() + " в " + clickedLocation);
        }
        
        if (!hasActiveSelection(player)) {
            if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                plugin.getLogger().info("[DEBUG] У игрока " + player.getName() + " нет активного выделения");
            }
            return;
        }

        SelectionData selectionData = getActiveSelection(player);
        if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
            plugin.getLogger().info("[DEBUG] Тип выделения для игрока " + player.getName() + ": " + selectionData.getType());
        }
        
        if (selectionData.isCompleted()) {
            if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                plugin.getLogger().info("[DEBUG] Выделение для игрока " + player.getName() + " уже завершено");
            }
            return;
        }

        if ("select2".equals(selectionData.getType())) {
            // Для select2 устанавливаем вторую точку
            selectionData.setPos2(clickedLocation);
            setPos2(player, clickedLocation); // Обновляем также старые pos1/pos2 карты
            if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                plugin.getLogger().info("[DEBUG] Выполняем RegionSelect2Complete для игрока " + player.getName());
            }
            plugin.getCommandTrigger().executeCommandsWithDelay(player, "RegionSelect2Complete", new String[]{});
            // Выполняем CMI команду для второй точки
            if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                plugin.getLogger().info("[DEBUG] Выполняем RegionSelect2CMI для игрока " + player.getName());
            }
            plugin.getCommandTrigger().executeCommandsWithDelay(player, "RegionSelect2CMI", new String[]{});
            completeSelection(player);
        } else if (selectionData.getPos1() == null) {
            // Устанавливаем первую точку
            selectionData.setPos1(clickedLocation);
            setPos1(player, clickedLocation); // Обновляем также старые pos1/pos2 карты
            
            // Вызываем триггер для первой точки
            if ("create".equals(selectionData.getType())) {
                if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                    plugin.getLogger().info("[DEBUG] Выполняем RegionSelect1Complete (create) для игрока " + player.getName());
                }
                plugin.getCommandTrigger().executeCommandsWithDelay(player, "RegionSelect1Complete", new String[]{});
            } else if ("select1".equals(selectionData.getType())) {
                if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                    plugin.getLogger().info("[DEBUG] Выполняем RegionSelect1Complete (select1) для игрока " + player.getName());
                }
                plugin.getCommandTrigger().executeCommandsWithDelay(player, "RegionSelect1Complete", new String[]{});
                // Выполняем CMI команду для первой точки
                if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                    plugin.getLogger().info("[DEBUG] Выполняем RegionSelect1CMI для игрока " + player.getName());
                }
                plugin.getCommandTrigger().executeCommandsWithDelay(player, "RegionSelect1CMI", new String[]{});
            }
        } else {
            // Устанавливаем вторую точку и завершаем выделение
            selectionData.setPos2(clickedLocation);
            setPos2(player, clickedLocation); // Обновляем также старые pos1/pos2 карты
            
            // Вызываем триггер для второй точки
            if ("create".equals(selectionData.getType())) {
                if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                    plugin.getLogger().info("[DEBUG] Выполняем RegionSelect2Complete (create) для игрока " + player.getName());
                }
                plugin.getCommandTrigger().executeCommandsWithDelay(player, "RegionSelect2Complete", new String[]{});
            }
            
            completeSelection(player);
        }
    }
}