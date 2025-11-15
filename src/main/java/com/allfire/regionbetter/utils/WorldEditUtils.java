package com.allfire.regionbetter.utils;

import com.allfire.regionbetter.RegionBetter;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.logging.Level;

/**
 * WorldEdit Utilities
 * 
 * Handles WorldEdit integration and operations
 * Manages selection and visualization
 * 
 * @author AllF1RE
 */
public class WorldEditUtils {
    
    private final RegionBetter plugin;
    
    public WorldEditUtils(RegionBetter plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Enable selection for player
     * 
     * @param player Player to enable selection for
     */
    public void enableSelection(Player player) {
        try {
            if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                plugin.getLogger().info("[DEBUG] WorldEditUtils.enableSelection called for player: " + player.getName());
            }
            
            if (!isWorldEditAvailable()) {
                plugin.getLogger().warning("WorldEdit не доступен!");
                return;
            }
            
            // Clear any existing selection using command
            boolean result = Bukkit.dispatchCommand(player, "//sel");
            if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                plugin.getLogger().info("[DEBUG] WorldEdit selection clear result: " + result);
            }
            
            if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                plugin.getLogger().info("[DEBUG] WorldEdit selection enabled for player: " + player.getName());
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при включении выделения для игрока " + player.getName(), e);
        }
    }
    
    /**
     * Disable selection for player
     * 
     * @param player Player to disable selection for
     */
    public void disableSelection(Player player) {
        try {
            // Clear selection using command
            boolean result = Bukkit.dispatchCommand(player, "//sel");
            if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                plugin.getLogger().info("[DEBUG] WorldEdit selection disabled for player: " + player.getName() + " (result: " + result + ")");
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при отключении выделения для игрока " + player.getName(), e);
        }
    }
    
    /**
     * Set selection for player
     *
     * @param player Player
     * @param pos1 First position
     * @param pos2 Second position
     */
    public void setSelection(Player player, Location pos1, Location pos2) {
        try {
            if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                plugin.getLogger().info("[DEBUG] WorldEditUtils.setSelection called for player: " + player.getName());
                plugin.getLogger().info("[DEBUG] Pos1: " + pos1);
                plugin.getLogger().info("[DEBUG] Pos2: " + pos2);
            }

            if (!isWorldEditAvailable()) {
                plugin.getLogger().warning("WorldEdit не доступен!");
                return;
            }

            // Try using WorldEdit API directly first for better SelectionVisualizer compatibility
            try {
                // Get WorldEdit session
                var sessionManager = WorldEdit.getInstance().getSessionManager();
                var bukkitPlayer = BukkitAdapter.adapt(player);
                var session = sessionManager.get(bukkitPlayer);
                
                // Get world
                var world = BukkitAdapter.adapt(pos1.getWorld());
                
                // Create region selector
                var regionSelector = session.getRegionSelector(world);
                
                // Convert Bukkit locations to WorldEdit BlockVector3
                var wePos1 = BukkitAdapter.asBlockVector(pos1);
                var wePos2 = BukkitAdapter.asBlockVector(pos2);
                
                // Set selection points
                regionSelector.selectPrimary(wePos1, null);
                regionSelector.selectSecondary(wePos2, null);
                
                if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                    plugin.getLogger().info("[DEBUG] WorldEdit API selection set successfully for player: " + player.getName());
                }
                
                // Also try commands as backup
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    String pos1Cmd = "//pos1 " + pos1.getBlockX() + " " + pos1.getBlockY() + " " + pos1.getBlockZ();
                    String pos2Cmd = "//pos2 " + pos2.getBlockX() + " " + pos2.getBlockY() + " " + pos2.getBlockZ();
                    
                    boolean result1 = Bukkit.dispatchCommand(player, pos1Cmd);
                    boolean result2 = Bukkit.dispatchCommand(player, pos2Cmd);
                    
                    if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                        plugin.getLogger().info("[DEBUG] WorldEdit command backup - pos1: " + result1 + ", pos2: " + result2);
                    }
                }, 3L);
                
            } catch (Exception apiException) {
                if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                    plugin.getLogger().warning("[DEBUG] WorldEdit API failed, falling back to commands: " + apiException.getMessage());
                }
                
                // Fallback to commands
                String pos1Cmd = "//pos1 " + pos1.getBlockX() + " " + pos1.getBlockY() + " " + pos1.getBlockZ();
                String pos2Cmd = "//pos2 " + pos2.getBlockX() + " " + pos2.getBlockY() + " " + pos2.getBlockZ();

                if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                    plugin.getLogger().info("[DEBUG] Executing WorldEdit commands: " + pos1Cmd + " and " + pos2Cmd);
                }

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    boolean result1 = Bukkit.dispatchCommand(player, pos1Cmd);
                    if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                        plugin.getLogger().info("[DEBUG] WorldEdit pos1 result: " + result1);
                    }
                }, 1L);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    boolean result2 = Bukkit.dispatchCommand(player, pos2Cmd);
                    if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                        plugin.getLogger().info("[DEBUG] WorldEdit pos2 result: " + result2);
                    }
                }, 2L);
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при установке выделения для игрока " + player.getName(), e);
        }
    }
    
    /**
     * Check if WorldEdit is available
     * 
     * @return True if available
     */
    public boolean isWorldEditAvailable() {
        try {
            return plugin.getServer().getPluginManager().getPlugin("WorldEdit") != null;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при проверке доступности WorldEdit", e);
            return false;
        }
    }
    
    /**
     * Get selection position 1
     * 
     * @param player Player to get selection from
     * @return BlockVector3 of position 1 or null
     */
    public BlockVector3 getSelectionPos1(Player player) {
        try {
            if (!isWorldEditAvailable()) {
                return null;
            }
            
            BukkitPlayer bukkitPlayer = BukkitAdapter.adapt(player);
            Region region = WorldEdit.getInstance().getSessionManager().get(bukkitPlayer).getSelection(bukkitPlayer.getWorld());
            if (region == null) {
                return null;
            }
            
            return region.getMinimumPoint();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get selection position 2
     * 
     * @param player Player to get selection from
     * @return BlockVector3 of position 2 or null
     */
    public BlockVector3 getSelectionPos2(Player player) {
        try {
            if (!isWorldEditAvailable()) {
                return null;
            }
            
            BukkitPlayer bukkitPlayer = BukkitAdapter.adapt(player);
            Region region = WorldEdit.getInstance().getSessionManager().get(bukkitPlayer).getSelection(bukkitPlayer.getWorld());
            if (region == null) {
                return null;
            }
            
            return region.getMaximumPoint();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get selection size
     * 
     * @param player Player to get selection from
     * @return Size in blocks
     */
    public int getSelectionSize(Player player) {
        try {
            if (!isWorldEditAvailable()) {
                return 0;
            }
            
            BukkitPlayer bukkitPlayer = BukkitAdapter.adapt(player);
            Region region = WorldEdit.getInstance().getSessionManager().get(bukkitPlayer).getSelection(bukkitPlayer.getWorld());
            if (region == null) {
                return 0;
            }
            
            return region.getArea();
        } catch (Exception e) {
            return 0;
        }
    }
}




