package com.allfire.regionbetter.listeners;

import com.allfire.regionbetter.RegionBetter;
import com.allfire.regionbetter.managers.SelectionManager;
import com.allfire.regionbetter.utils.CommandTrigger;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.List;

public class PlayerListener implements Listener {
    
    private final RegionBetter plugin;
    private final CommandTrigger commandTrigger;
    
    // Caching player states to prevent repeated triggers
    private final java.util.Map<Player, String> playerRegionStates = new java.util.HashMap<>();
    
    // Optimization: caching last check time
    private final java.util.Map<Player, Long> lastCheckTime = new java.util.HashMap<>();
    private final java.util.Map<Player, Long> lastRegionViewExecution = new java.util.HashMap<>();

    public PlayerListener(RegionBetter plugin) {
        this.plugin = plugin;
        this.commandTrigger = new CommandTrigger(plugin);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Check active selection
        if (!plugin.getSelectionManager().hasActiveSelection(player)) {
            return;
        }

        // Check permissions
        if (!player.hasPermission("regionbetter.selection.use")) {
            return;
        }

        // Handle only SHIFT+clicks
        if (!player.isSneaking()) {
            return;
        }

        // Cancel block breaking during active selection
        event.setCancelled(true);

        // Handle point selection
        if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR) {
            handleShiftLeftClick(player, event);
        }
    }
    
    /**
     * Handle SHIFT+LEFT_CLICK selection
     */
    private void handleShiftLeftClick(Player player, PlayerInteractEvent event) {
        try {
            com.allfire.regionbetter.managers.SelectionManager.SelectionData selectionData = plugin.getSelectionManager().getActiveSelection(player);
            if (selectionData == null || selectionData.isCompleted()) {
                return;
            }
            
            Location clickedLocation;
            if (event.getClickedBlock() != null) {
                // If clicked on block, use its position
                clickedLocation = event.getClickedBlock().getLocation();
            } else {
                // If clicked in air, use raycast
                clickedLocation = getTargetBlockLocation(player);
            }
            
            String selectionType = selectionData.getType();
            
            if ("select1".equals(selectionType)) {
                // Set first point
                selectionData.setPos1(clickedLocation);
                plugin.getSelectionManager().setPos1(player, clickedLocation);
                
                // Complete only this selection type (not full selection)
                plugin.getSelectionManager().completeSelection(player);
                
                // Execute trigger for point setup confirmation
                commandTrigger.executeCommandsWithDelay(player, "RegionBetterSelect1", new String[]{});
                
            } else if ("select2".equals(selectionType)) {
                // Set second point
                selectionData.setPos2(clickedLocation);
                plugin.getSelectionManager().setPos2(player, clickedLocation);
                
                // Complete only this selection type (not full selection)
                plugin.getSelectionManager().completeSelection(player);
                
                // Execute trigger for point setup confirmation
                commandTrigger.executeCommandsWithDelay(player, "RegionBetterSelect2", new String[]{});
                
            } else if ("create".equals(selectionType)) {
                // Standard logic for region creation
                if (selectionData.getPos1() == null) {
                    selectionData.setPos1(clickedLocation);
                    plugin.getSelectionManager().setPos1(player, clickedLocation);
                    
                    // Execute trigger
                    commandTrigger.executeCommandsWithDelay(player, "RegionBetterSelect1", new String[]{});
                    
                } else {
                    // Set second point and complete selection
                    selectionData.setPos2(clickedLocation);
                    plugin.getSelectionManager().setPos2(player, clickedLocation);
                    
                    // Complete selection
                    com.allfire.regionbetter.managers.SelectionManager.SelectionData completedSelection = plugin.getSelectionManager().completeSelection(player);
                    
                    if (completedSelection != null) {
                        // Execute triggers with delay
                        commandTrigger.executeCommandsWithDelay(player, "RegionBetterSelect2", new String[]{});
                        commandTrigger.executeCommandsWithDelay(player, "RegionSelectComplete", new String[]{});
                    }
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error processing SHIFT+click for player " + player.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Get the block location that the player is looking at using raycast
     */
    private Location getTargetBlockLocation(Player player) {
        try {
            // Get player eye position and direction
            Location eyeLocation = player.getEyeLocation();
            Vector direction = eyeLocation.getDirection();
            
            // Perform raycast to find block
            RayTraceResult rayTrace = player.getWorld().rayTraceBlocks(eyeLocation, direction, 100.0, 
                org.bukkit.FluidCollisionMode.NEVER, true);
            
            if (rayTrace != null && rayTrace.getHitBlock() != null) {
                Block hitBlock = rayTrace.getHitBlock();
                return hitBlock.getLocation();
            } else {
                // If block not found, use player position
                return player.getLocation();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error determining block for player " + player.getName() + ": " + e.getMessage());
            return player.getLocation();
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Check access permissions (always check if CheckPerms is enabled)
        
        // Check access permissions
        if (plugin.getConfigManager().getConfig().getBoolean("RegionViewSettings.CheckPerms", true)) {
            if (!player.hasPermission("regionbetter.region.view")) {
                return;
            }
        }

        // Check cooldown
        int cooldownTicks = plugin.getConfigManager().getConfig().getInt("RegionViewSettings.Cooldown", 20);
        if (cooldownTicks > 0) {
            long currentTime = System.currentTimeMillis();
            Long lastExecution = lastRegionViewExecution.get(player);
            if (lastExecution != null && (currentTime - lastExecution) < (cooldownTicks * 50)) { // 50ms = 1 tick
                return; // Cooldown still active
            }
        }

        // Check distance to region boundaries and execute commands
        try {
            Location from = event.getFrom();
            Location to = event.getTo();
            
            if (to == null || from == null) return;
            
            // 🚀 OPTIMIZATION: Check world change at the beginning (like in WorldGuard)
            if (!from.getWorld().equals(to.getWorld())) {
                // Clear cache on world change
                playerRegionStates.remove(player);
                lastCheckTime.remove(player);
                lastRegionViewExecution.remove(player);
                return;
            }
            
            // Check if position changed significantly (minimum 1 block)
            if (from.getBlockX() == to.getBlockX() && 
                from.getBlockZ() == to.getBlockZ() &&
                from.getBlockY() == to.getBlockY()) {
                return; // Player hasn't moved significantly
            }
            
            // 🚀 OPTIMIZATION: Permissions already checked above
            
            // 🚀 OPTIMIZATION: Cache check time (minimum 100ms between checks)
            long currentTime = System.currentTimeMillis();
            Long lastTime = lastCheckTime.get(player);
            if (lastTime != null && (currentTime - lastTime) < 100) {
                return; // Checking too frequently, skip
            }
            lastCheckTime.put(player, currentTime);
            
            // 🚀 KEY DIFFERENCE: Removed distance measurement between locations
            // This prevents the error "Cannot measure distance between world and world_nether"
            
            // 🚀 KEY DIFFERENCE: Use WorldGuard API for optimized region search
            com.sk89q.worldguard.WorldGuard worldGuard = com.sk89q.worldguard.WorldGuard.getInstance();
            com.sk89q.worldguard.protection.regions.RegionContainer container = worldGuard.getPlatform().getRegionContainer();
            com.sk89q.worldguard.protection.managers.RegionManager regionManager = container.get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(to.getWorld()));
            
            if (regionManager == null) return;
            
            // 🚀 KEY DIFFERENCE: Use BlockVector3 instead of Location
            // This prevents errors with distance measurement between worlds
            com.sk89q.worldedit.math.BlockVector3 toVector = com.sk89q.worldedit.math.BlockVector3.at(to.getX(), to.getY(), to.getZ());
            com.sk89q.worldguard.protection.ApplicableRegionSet applicableRegions = regionManager.getApplicableRegions(toVector);
            
            // Filter regions with regionbetter-view flag
            String currentRegion = null;
            var flag = plugin.getRegionBetterViewFlag();
            if (flag != null) {
                for (com.sk89q.worldguard.protection.regions.ProtectedRegion region : applicableRegions) {
                    var flagValue = region.getFlag(flag);
                    if (flagValue == com.sk89q.worldguard.protection.flags.StateFlag.State.ALLOW) {
                        currentRegion = region.getId();
                        if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                            plugin.getLogger().info("Player " + player.getName() + " is inside region " + region.getId() + " with regionbetter-view flag");
                        }
                        break; // WorldGuard returns regions in priority order
                    }
                }
            }
            
            // Check deactivation for player's current region
            String previousRegion = playerRegionStates.get(player);
            
            // Check if player state changed (entry/exit logic like in WorldGuard)
            if (!java.util.Objects.equals(currentRegion, previousRegion)) {
                // State changed - update cache and execute commands
                playerRegionStates.put(player, currentRegion);
                
                if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                    plugin.getLogger().info("Player " + player.getName() + " state changed: " + previousRegion + " -> " + currentRegion);
                }
                
                if (currentRegion != null) {
                    // ENTRY: Player entered region with flag - execute entry commands
                    if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                        plugin.getLogger().info("ENTRY: Executing entry commands for region: " + currentRegion);
                    }
                    executeRegionViewCommands(player, currentRegion);
                    lastRegionViewExecution.put(player, System.currentTimeMillis());
                } else if (previousRegion != null) {
                    // EXIT: Player exited region with flag - execute exit commands
                    if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                        plugin.getLogger().info("EXIT: Executing exit commands for region: " + previousRegion);
                    }
                    executeRegionExitCommands(player, previousRegion);
                    lastRegionViewExecution.put(player, System.currentTimeMillis());
                }
            }
            // If state hasn't changed - don't execute commands (like in WorldGuard)
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking region boundaries: " + e.getMessage());
        }
    }
    

    /**
     * Executes commands on region exit
     */
    private void executeRegionExitCommands(Player player, String regionName) {
        try {
            // Get commands from RegionViewSettings.ExitCmds (if any)
            java.util.List<String> commands = plugin.getConfigManager().getConfig().getStringList("RegionViewSettings.ExitCmds");
            
            if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                plugin.getLogger().info("Found exit commands: " + commands.size());
            }
            
            if (commands.isEmpty()) {
                // If no special exit commands, use highlight disable commands
                commands.add("asPlayer! svis we");
                if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                    plugin.getLogger().info("Using highlight disable command: svis we");
                }
            }
            
            // Execute each command
            for (String command : commands) {
                if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                    plugin.getLogger().info("Processing exit command: " + command);
                }
                
                String processedCommand = command.replace("{region_name}", regionName);
                if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                    plugin.getLogger().info("Command after replacing {region_name}: " + processedCommand);
                }
                
                // Process PlaceholderAPI placeholders
                if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                    processedCommand = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, processedCommand);
                    if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                        plugin.getLogger().info("Command after PlaceholderAPI: " + processedCommand);
                    }
                }
                
                if (processedCommand.startsWith("asConsole!")) {
                    String consoleCommand = processedCommand.substring(10).trim();
                    if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                        plugin.getLogger().info("Executing console exit command: " + consoleCommand);
                    }
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), consoleCommand);
                } else if (processedCommand.startsWith("asPlayer!")) {
                    String playerCommand = processedCommand.substring(9).trim();
                    if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                        plugin.getLogger().info("Executing player exit command: " + playerCommand);
                    }
                    player.performCommand(playerCommand);
                } else {
                    plugin.getLogger().warning("Unknown exit command prefix: " + processedCommand);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error executing region exit commands: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Executes region highlighting commands
     */
    private void executeRegionViewCommands(Player player, String regionName) {
        try {
            // Get commands from RegionViewSettings.Cmds
            java.util.List<String> commands = plugin.getConfigManager().getConfig().getStringList("RegionViewSettings.Cmds");
            
            if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                plugin.getLogger().info("Found commands to execute: " + commands.size());
            }
            
            if (commands.isEmpty()) {
                plugin.getLogger().warning("No commands in RegionViewSettings.Cmds!");
                return;
            }
            
            // Execute each command with proper placeholder handling
            for (String command : commands) {
                if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                    plugin.getLogger().info("Processing command: " + command);
                }
                
                String processedCommand = command.replace("{region_name}", regionName);
                if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                    plugin.getLogger().info("Command after replacing {region_name}: " + processedCommand);
                }
                
                // Process PlaceholderAPI placeholders (for %player_name% and others)
                if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                    processedCommand = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, processedCommand);
                    if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                        plugin.getLogger().info("Command after PlaceholderAPI: " + processedCommand);
                    }
                }
                
                if (processedCommand.startsWith("asConsole!")) {
                    String consoleCommand = processedCommand.substring(10).trim();
                    if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                        plugin.getLogger().info("Executing console command: " + consoleCommand);
                    }
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), consoleCommand);
                } else if (processedCommand.startsWith("asPlayer!")) {
                    String playerCommand = processedCommand.substring(9).trim();
                    if (plugin.getConfigManager().getConfig().getBoolean("Config.Debug", false)) {
                        plugin.getLogger().info("Executing player command: " + playerCommand);
                    }
                    player.performCommand(playerCommand);
                } else {
                    plugin.getLogger().warning("Unknown command prefix: " + processedCommand);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error executing region highlighting commands: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Gets all regions in the world
     */
    private java.util.List<String> getAllRegionsInWorld(org.bukkit.World world) {
        java.util.List<String> regions = new java.util.ArrayList<>();
        try {
            // Get all regions in the world via WorldGuard API
            var regionManager = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform()
                .getRegionContainer().get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world));
            
            if (regionManager != null) {
                regionManager.getRegions().forEach((name, region) -> regions.add(name));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting all regions in world: " + e.getMessage());
        }
        return regions;
    }

    /**
     * Handle region name input in chat
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // Check if player is waiting for name input
        if (!plugin.getSelectionManager().isWaitingForName(player)) {
            return;
        }

        String regionName = event.getMessage().trim();
        event.setCancelled(true); // Cancel chat message

        // Support cancellation - check hardcoded words like in eRegions
        if (regionName.equalsIgnoreCase("cancel") || regionName.equalsIgnoreCase("cancel") || 
            regionName.equalsIgnoreCase("no") || regionName.equalsIgnoreCase("no")) {
            plugin.getSelectionManager().clearPlayerStates(player);
            commandTrigger.executeCommandsWithDelay(player, "RegionCreateCancel", new String[]{});
            return;
        }
        
        // Also check cancel commands from config
        List<String> cancelCommands = plugin.getConfigManager().getConfig().getStringList("CancelCommands");
        if (cancelCommands.contains(regionName.toLowerCase())) {
            plugin.getSelectionManager().clearPlayerStates(player);
            commandTrigger.executeCommandsWithDelay(player, "RegionCreateCancel", new String[]{});
            return;
        }

        // Validate name (only A-Za-z0-9_-)
        if (!regionName.matches("^[a-zA-Z0-9_-]+$")) {
            commandTrigger.executeCommandsWithDelay(player, "RegionCreateInvalidName", new String[]{regionName});
            return;
        }

        // Check if region exists
        if (plugin.getRegionManager().regionExists(regionName)) {
            commandTrigger.executeCommandsWithDelay(player, "RegionCreateExists", new String[]{regionName});
            return;
        }

        // Get selection data
        var selectionData = plugin.getSelectionManager().getWaitingForName(player);
        if (selectionData == null || selectionData.getPos1() == null || selectionData.getPos2() == null) {
            plugin.getSelectionManager().clearPlayerStates(player);
            commandTrigger.executeCommandsWithDelay(player, "RegionCreateNoSelection", new String[]{});
            return;
        }

        // Check overlap with other regions (like in eRegions)
        List<String> overlappingRegions = plugin.getRegionManager().getOverlappingRegions(
            player.getWorld(), 
            selectionData.getPos1(), 
            selectionData.getPos2()
        );
        
        if (!overlappingRegions.isEmpty()) {
            // There are overlaps - show error
            String message = plugin.getConfigManager().getLanguageConfig().getString("ErrorMessages.RegionOverlap", 
                "&cSelected area overlaps with existing regions: &e" + String.join(", ", overlappingRegions) + "&c!");
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage(message);
            });
            return;
        }

        // Create region
        boolean success = plugin.getRegionManager().createRegion(
            player,
            regionName,
            selectionData.getPos1(),
            selectionData.getPos2()
        );

        if (success) {
            // Successful creation - clear all player states
            plugin.getSelectionManager().removeWaitingForName(player);
            plugin.getSelectionManager().clearPlayerStates(player);
            commandTrigger.executeCommandsWithDelay(player, "RegionCreateSuccess", new String[]{regionName});
        } else {
            // Creation error - send general error message
            String message = plugin.getConfigManager().getLanguageConfig().getString("ErrorMessages.RegionCreateFailed", 
                "&cError creating region &e" + regionName + "&c!");
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage(message);
            });
        }
    }


    /**
     * Clear states on player quit
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getSelectionManager().clearPlayerStates(player);
        // Clear player state cache
        playerRegionStates.remove(player);
        lastCheckTime.remove(player);
    }
}