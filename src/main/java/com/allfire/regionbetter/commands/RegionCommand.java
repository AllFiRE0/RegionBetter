package com.allfire.regionbetter.commands;

import com.allfire.regionbetter.RegionBetter;
import com.allfire.regionbetter.managers.SelectionManager.SelectionData;
import com.allfire.regionbetter.utils.CommandTrigger;
import com.allfire.regionbetter.utils.ColorUtils;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RegionCommand implements CommandExecutor, TabCompleter {

    private final RegionBetter plugin;
    private final CommandTrigger commandTrigger;

    public RegionCommand(RegionBetter plugin) {
        this.plugin = plugin;
        this.commandTrigger = new CommandTrigger(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            String message = plugin.getConfigManager().getLanguageConfig().getString("ErrorMessages.PlayerOnlyCommand", "&cЭта команда доступна только игрокам!");
            ColorUtils.sendColoredMessage((Player) sender, message);
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            handleHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                handleCreate(player, args);
                break;
            case "delete":
                handleDelete(player, args);
                break;
            case "addowner":
                handleAddOwner(player, args);
                break;
            case "addmember":
                handleAddMember(player, args);
                break;
            case "removeowner":
                handleRemoveOwner(player, args);
                break;
            case "removemember":
                handleRemoveMember(player, args);
                break;
            case "info":
                handleInfo(player, args);
                break;
            case "help":
                handleHelp(player);
                break;
            case "better":
                handleBetter(player);
                break;
            case "select":
                handleSelect(player, args);
                break;
            case "glow":
                handleGlow(player, args);
                break;
            case "view":
                handleView(player, args);
                break;
            case "reload":
                handleReload(player);
                break;
            case "cancel":
                handleCancel(player);
                break;
            case "undo":
                handleUndo(player);
                break;
            default:
                handleHelp(player);
                break;
        }

        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            commandTrigger.executeCommandsWithDelay(player, "RegionCreate", new String[]{});
            return;
        }

        String regionName = args[1];
        
        // Проверяем, есть ли выделение
        if (!plugin.getSelectionManager().hasSelection(player)) {
            commandTrigger.executeCommandsWithDelay(player, "RegionCreate", new String[]{});
            return;
        }

        // Создаем регион
        boolean success = plugin.getRegionManager().createRegion(
            player,
            regionName,
            plugin.getSelectionManager().getPos1(player),
            plugin.getSelectionManager().getPos2(player)
        );

        if (success) {
            // Очищаем все состояния игрока после успешного создания
            plugin.getSelectionManager().clearPlayerStates(player);
            commandTrigger.executeCommandsWithDelay(player, "RegionCreateSuccess", new String[]{regionName});
        } else {
            commandTrigger.executeCommandsWithDelay(player, "RegionCreateError", new String[]{regionName});
        }
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length < 2) {
            commandTrigger.executeCommandsWithDelay(player, "RegionDelete", new String[]{});
            return;
        }

        String regionName = args[1];
        boolean success = plugin.getRegionManager().deleteRegion(player, regionName);

        if (success) {
            commandTrigger.executeCommandsWithDelay(player, "RegionDeleteSuccess", new String[]{regionName});
        } else {
            commandTrigger.executeCommandsWithDelay(player, "RegionDeleteError", new String[]{regionName});
        }
    }

    private void handleAddOwner(Player player, String[] args) {
        if (args.length < 3) {
            commandTrigger.executeCommandsWithDelay(player, "RegionAddOwner", new String[]{});
            return;
        }

        String regionName = args[1];
        String targetPlayer = args[2];
        boolean success = plugin.getRegionManager().addOwner(player, regionName, targetPlayer);

        if (success) {
            commandTrigger.executeCommandsWithDelay(player, "RegionAddOwnerSuccess", new String[]{regionName, targetPlayer});
        } else {
            commandTrigger.executeCommandsWithDelay(player, "RegionAddOwnerError", new String[]{regionName, targetPlayer});
        }
    }

    private void handleAddMember(Player player, String[] args) {
        if (args.length < 3) {
            commandTrigger.executeCommandsWithDelay(player, "RegionAddMember", new String[]{});
            return;
        }

        String regionName = args[1];
        String targetPlayer = args[2];
        boolean success = plugin.getRegionManager().addMember(player, regionName, targetPlayer);

        if (success) {
            commandTrigger.executeCommandsWithDelay(player, "RegionAddMemberSuccess", new String[]{regionName, targetPlayer});
        } else {
            commandTrigger.executeCommandsWithDelay(player, "RegionAddMemberError", new String[]{regionName, targetPlayer});
        }
    }

    private void handleRemoveOwner(Player player, String[] args) {
        if (args.length < 3) {
            commandTrigger.executeCommandsWithDelay(player, "RegionRemoveOwner", new String[]{});
            return;
        }

        String regionName = args[1];
        String targetPlayer = args[2];
        boolean success = plugin.getRegionManager().removeOwner(player, regionName, targetPlayer);

        if (success) {
            commandTrigger.executeCommandsWithDelay(player, "RegionRemoveOwnerSuccess", new String[]{regionName, targetPlayer});
        } else {
            commandTrigger.executeCommandsWithDelay(player, "RegionRemoveOwnerError", new String[]{regionName, targetPlayer});
        }
    }

    private void handleRemoveMember(Player player, String[] args) {
        if (args.length < 3) {
            commandTrigger.executeCommandsWithDelay(player, "RegionRemoveMember", new String[]{});
            return;
        }

        String regionName = args[1];
        String targetPlayer = args[2];
        boolean success = plugin.getRegionManager().removeMember(player, regionName, targetPlayer);

        if (success) {
            commandTrigger.executeCommandsWithDelay(player, "RegionRemoveMemberSuccess", new String[]{regionName, targetPlayer});
        } else {
            commandTrigger.executeCommandsWithDelay(player, "RegionRemoveMemberError", new String[]{regionName, targetPlayer});
        }
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            commandTrigger.executeCommandsWithDelay(player, "RegionInfo", new String[]{});
            return;
        }

        String regionName = args[1];
        var regionInfo = plugin.getRegionManager().getRegionDetailedInfo(player, regionName);

        if (regionInfo != null) {
            // Подготавливаем аргументы для триггеров
            String[] triggerArgs = {
                regionName,
                regionInfo.getOwners(),
                regionInfo.getMembers(),
                regionInfo.getFlags(),
                regionInfo.getPos1(),
                regionInfo.getPos2()
            };
            
            if (regionInfo.isOwner()) {
                commandTrigger.executeCommandsWithDelay(player, "RegionInfoOwner", triggerArgs);
            } else if (regionInfo.isMember()) {
                commandTrigger.executeCommandsWithDelay(player, "RegionInfoMember", triggerArgs);
            } else {
                commandTrigger.executeCommandsWithDelay(player, "RegionInfoPublic", triggerArgs);
            }
        } else {
            commandTrigger.executeCommandsWithDelay(player, "RegionInfoNotFound", new String[]{regionName});
        }
    }

    private void handleHelp(Player player) {
        commandTrigger.executeCommandsWithDelay(player, "RegionHelp", new String[]{});
    }

    private void handleBetter(Player player) {
        // Если у игрока уже есть обе точки, запускаем создание региона
        if (plugin.getSelectionManager().hasSelection(player)) {
            commandTrigger.executeCommandsWithDelay(player, "RegionBetterComplete", new String[]{});
            // Добавляем игрока в ожидание названия региона
            SelectionData selectionData = new SelectionData(player, "create");
            selectionData.setPos1(plugin.getSelectionManager().getPos1(player));
            selectionData.setPos2(plugin.getSelectionManager().getPos2(player));
            plugin.getSelectionManager().setWaitingForName(player, selectionData);
            return;
        }

        // Проверяем, есть ли уже активное выделение (только для типа "create")
        if (plugin.getSelectionManager().hasActiveSelection(player)) {
            SelectionData activeSelection = plugin.getSelectionManager().getActiveSelection(player);
            if (activeSelection != null && "create".equals(activeSelection.getType())) {
                commandTrigger.executeCommandsWithDelay(player, "RegionBetterAlreadyActive", new String[]{});
                return;
            }
        }

        // Запускаем процесс выделения
        plugin.getSelectionManager().startSelection(player, "create");
        commandTrigger.executeCommandsWithDelay(player, "RegionBetter", new String[]{});
    }

    private void handleSelect(Player player, String[] args) {
        if (args.length < 2) {
            commandTrigger.executeCommandsWithDelay(player, "RegionSelect", new String[]{});
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "1":
                // Запускаем процесс выбора первой точки (как в /region better)
                plugin.getSelectionManager().startSelection(player, "select1");
                commandTrigger.executeCommandsWithDelay(player, "RegionSelect1", new String[]{});
                break;

            case "2":
                // Запускаем процесс выбора второй точки (как в /region better)
                plugin.getSelectionManager().startSelection(player, "select2");
                commandTrigger.executeCommandsWithDelay(player, "RegionSelect2", new String[]{});
                break;

            case "clear":
                plugin.getSelectionManager().clearSelection(player);
                commandTrigger.executeCommandsWithDelay(player, "RegionSelectClear", new String[]{});
                break;

            case "move":
                if (args.length < 3) {
                    commandTrigger.executeCommandsWithDelay(player, "RegionSelectMove", new String[]{});
                    return;
                }
                try {
                    int blocks = Integer.parseInt(args[2]);
                    
                    // Получаем 3D направление взгляда игрока
                    Vector direction = player.getLocation().getDirection();
                    
                    double deltaX = direction.getX() * blocks;
                    double deltaY = direction.getY() * blocks;
                    double deltaZ = direction.getZ() * blocks;
                    
                    plugin.getSelectionManager().moveSelection(player, (int) deltaX, (int) deltaY, (int) deltaZ);
                    String blockMove = (blocks >= 0 ? "+" : "") + String.valueOf(blocks);
                    commandTrigger.executeCommandsWithDelay(player, "RegionSelectMoveSuccess", new String[]{blockMove});
                } catch (NumberFormatException e) {
                    commandTrigger.executeCommandsWithDelay(player, "RegionSelectMoveError", new String[]{});
                }
                break;

            case "size":
                if (args.length < 4) {
                    commandTrigger.executeCommandsWithDelay(player, "RegionSelectSize", new String[]{});
                    return;
                }
                try {
                    String direction = args[2].toLowerCase();
                    int blocks = Integer.parseInt(args[3]);
                    
                    // Проверяем валидность направления
                    if (!direction.equals("up") && !direction.equals("down") && !direction.equals("face")) {
                        commandTrigger.executeCommandsWithDelay(player, "RegionSelectSizeError", new String[]{});
                        return;
                    }
                    
                    plugin.getSelectionManager().resizeSelection(player, direction, blocks);
                    String blockSize = (blocks >= 0 ? "+" : "") + String.valueOf(blocks);
                    commandTrigger.executeCommandsWithDelay(player, "RegionSelectSizeSuccess", new String[]{direction, blockSize});
                } catch (NumberFormatException e) {
                    commandTrigger.executeCommandsWithDelay(player, "RegionSelectSizeError", new String[]{});
                }
                break;

            case "glow":
                commandTrigger.executeCommandsWithDelay(player, "RegionSelectGlow", new String[]{});
                break;

            default:
                commandTrigger.executeCommandsWithDelay(player, "RegionSelect", new String[]{});
                break;
        }
    }

    private void handleGlow(Player player, String[] args) {
        if (args.length < 2) {
            commandTrigger.executeCommandsWithDelay(player, "RegionGlow", new String[]{});
            return;
        }

        String regionName = args[1];
        commandTrigger.executeCommandsWithDelay(player, "RegionGlow", new String[]{regionName});
    }

    private void handleView(Player player, String[] args) {
        // Команда /region view работает как переключатель прав regionbetter.region.view
        if (player.hasPermission("regionbetter.region.view")) {
            // У игрока есть право - забираем его
            player.addAttachment(plugin, "regionbetter.region.view", false);
            commandTrigger.executeCommandsWithDelay(player, "RegionViewDisabled", new String[]{});
        } else {
            // У игрока нет права - выдаем его
            player.addAttachment(plugin, "regionbetter.region.view", true);
            commandTrigger.executeCommandsWithDelay(player, "RegionViewEnabled", new String[]{});
        }
    }

    private void handleReload(Player player) {
        if (!player.hasPermission("regionbetter.admin")) {
            commandTrigger.executeCommandsWithDelay(player, "RegionReloadNoPermission", new String[]{});
            return;
        }

        plugin.getConfigManager().reloadConfig();
        commandTrigger.executeCommandsWithDelay(player, "RegionReload", new String[]{});
    }

    private void handleCancel(Player player) {
        plugin.getSelectionManager().clearPlayerStates(player);
        commandTrigger.executeCommandsWithDelay(player, "RegionCancel", new String[]{});
    }

    private void handleUndo(Player player) {
        commandTrigger.executeCommandsWithDelay(player, "RegionUndo", new String[]{});
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList(
                "create", "delete", "addowner", "addmember", "removeowner", "removemember",
                "info", "help", "better", "select", "glow", "view", "reload", "cancel", "undo"
            );
            
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "delete":
                case "addowner":
                case "addmember":
                case "removeowner":
                case "removemember":
                case "info":
                    // Добавляем все регионы для команды info
                    completions.addAll(getAllRegionsForInfo((Player) sender));
                    break;
                case "glow":
                    // Добавляем все регионы для команды glow (кроме исключений)
                    completions.addAll(getAllRegionsForGlow((Player) sender));
                    break;
                case "select":
                    List<String> selectOptions = Arrays.asList("1", "2", "clear", "move", "size", "glow");
                    for (String option : selectOptions) {
                        if (option.toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(option);
                        }
                    }
                    break;
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("select") && args[1].equals("size")) {
                List<String> directions = Arrays.asList("up", "down", "face");
                for (String direction : directions) {
                    if (direction.toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(direction);
                    }
                }
            }
        }

        return completions;
    }
    
    /**
     * Получает все регионы для команды glow (кроме исключений и __global__)
     */
    private List<String> getAllRegionsForGlow(Player player) {
        List<String> regions = new ArrayList<>();
        
        try {
            // Получаем все регионы в мире игрока
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regionManager = container.get(BukkitAdapter.adapt(player.getWorld()));
            
            if (regionManager != null) {
                // Получаем список исключений из конфигурации
                List<String> excludedRegions = plugin.getConfigManager().getConfig().getStringList("RegionTabGlow");
                excludedRegions.add("__global__"); // Всегда исключаем глобальный регион
                
                for (ProtectedRegion region : regionManager.getRegions().values()) {
                    String regionName = region.getId();
                    
                    // Исключаем __global__ и регионы из списка исключений
                    if (!excludedRegions.contains(regionName)) {
                        regions.add(regionName);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при получении списка регионов для glow: " + e.getMessage());
        }
        
        return regions;
    }
    
    /**
     * Получает все регионы для команды info (все регионы в мире, кроме исключений)
     */
    private List<String> getAllRegionsForInfo(Player player) {
        List<String> regions = new ArrayList<>();
        
        try {
            // Получаем все регионы в мире игрока
            var worldRegions = plugin.getRegionManager().getAllRegionsInWorld(player.getWorld());
            regions.addAll(worldRegions);
            
            // Исключаем __global__ регион
            regions.remove("__global__");
            
            // Исключаем регионы из конфигурации RegionTabInfo
            List<String> excludedRegions = plugin.getConfigManager().getConfig().getStringList("RegionTabInfo");
            for (String excludedRegion : excludedRegions) {
                regions.remove(excludedRegion);
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при получении списка регионов для info: " + e.getMessage());
        }
        
        return regions;
    }
}