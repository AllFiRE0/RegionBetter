# RegionBetter Plugin

**Version:** 1.0.1  
**Author:** AllF1RE  
**Description:** Enhanced plugin for region management with WorldGuard integration, WorldEdit support and customizable commands.

## Features

### 🎯 Main Features
- **Region management** with WorldGuard integration
- **Limit system** based on LuckPerms permissions
- **Customizable commands** and messages via configuration
- **Automatic flags** when creating regions
- **Region highlighting** on entry/exit
- **Step-by-step creation** of regions with visualization

### 🔧 Integrations
- **WorldGuard** - region management
- **WorldEdit** - selection visualization
- **LuckPerms** - permission and limit system
- **PlaceholderAPI** - placeholders for other plugins
- **CMI** - CMI command support
- **SVIS** - SVIS command support

## Commands

### Main Commands
```
/region better          - Step-by-step region creation
/region select 1        - Select first point
/region select 2        - Select second point
/region cancel          - Cancel creation process
/region info <name>     - Region information
/region reload          - Reload configuration
/region help            - Command help
```

### Aliases
- `/r` - shortcut for `/region`
- `/regions` - alternative alias
- `/territory` - alternative alias

## Permissions

### Main Permissions
```
regionbetter.use                    - Main permission to use the plugin
regionbetter.selection.use          - Permission to select selection points
regionbetter.region.view            - Permission to view region information
regionbetter.admin                  - Administrative permissions
```

### Block Limit Permissions
```
regionbetter.selection.10           - Up to 10 blocks in selection
regionbetter.selection.100          - Up to 100 blocks in selection
regionbetter.selection.1000         - Up to 1000 blocks in selection
regionbetter.selection.5000         - Up to 5000 blocks in selection
regionbetter.selection.10000        - Up to 10000 blocks in selection
```

### Region Limit Permissions
```
regionbetter.regions.1              - Up to 1 region
regionbetter.regions.5              - Up to 5 regions
regionbetter.regions.10             - Up to 10 regions
regionbetter.regions.unlimited      - Unlimited number of regions
```

## Configuration

### Basic Settings
```yaml
Config:
  Debug: false                       # Enable debug messages
  Language: "en-us"                  # Plugin language

RegionLimits:
  MaxBlocks: 10000                   # Maximum number of blocks by default
  MaxRegions: 5                      # Maximum number of regions by default
  CheckPerms: true                   # Check limit permissions
```

### Region Highlighting Settings
```yaml
RegionViewSettings:
  Enabled: true                      # Enable region highlighting
  CheckPerms: true                   # Check view permissions
  MembedRegions: false               # Show only player's regions
  
  # Commands on region entry
  Cmds:
    - "asConsole! cmi actionbarmsg %player_name% -s:4 &6Territory {region_name}"
    - "asPlayer! svis wg {region_name}"
```

### Command Settings

#### Region Creation
```yaml
RegionBetter:
  Usage: "Usage: /region better"
  Cooldown: 60
  Cmds:
    - "msg! &6=== Step-by-step Region Creation ==="
    - "msg! &7Starting region creation process..."
    - "delay! 10"
    - "asPlayer! svis we"

RegionSelect1:
  Usage: "Usage: /region select 1"
  Cooldown: 60
  Cmds:
    - "msg! &7Select point 1 for territory edge using Shift+Left Mouse Button!"
    - "asPlayer! svis we"

RegionSelect2:
  Usage: "Usage: /region select 2"
  Cooldown: 60
  Cmds:
    - "msg! &7Select point 2 for territory edge using Shift+Left Mouse Button!"
    - "asPlayer! svis we"
```

#### CMI Commands
```yaml
RegionSelect1CMI:
  Cooldown: 0
  Cmds:
    - "asPlayerForce! cmi point trial_spawner_detection_ominous 15 -s:1"
    - "delay! 20"
    - "asPlayer! svis we"

RegionSelect2CMI:
  Cooldown: 0
  Cmds:
    - "asPlayerForce! cmi point trial_spawner_detection 15 -s:1"
    - "delay! 20"
    - "asPlayer! svis we"
```

## Command Prefixes

### Available Prefixes
- **`msg!`** - Send message to player
- **`asPlayer!`** - Execute command as player
- **`asPlayerForce!`** - Force execute command as player (bypass permission checks)
- **`asConsole!`** - Execute command as console
- **`delay!`** - Delay before executing next command

### Usage Examples
```yaml
Cmds:
  - "msg! &aWelcome to the region!"
  - "asPlayer! svis wg {region_name}"
  - "asPlayerForce! cmi point trial_spawner_detection 15 -s:1"
  - "asConsole! cmi actionbarmsg %player_name% -s:4 &6Territory {region_name}"
  - "delay! 20"
```

## Placeholders

### PlaceholderAPI Placeholders
```
%regionbetter_player_regions%        - Player's number of regions
%regionbetter_player_blocks%         - Player's maximum number of blocks
%regionbetter_player_max_regions%    - Player's maximum number of regions
%regionbetter_region_blocks%         - Number of blocks in region
%regionbetter_region_area%           - Region area
```

### Internal Placeholders
```
{region_name}                        - Region name
{player_name}                        - Player name
{blocks}                             - Number of blocks
{direction}                          - Direction
{0}, {1}, {2}...                     - Parameters from placeholders array
```

## Automatic Flags

When creating a region, the following flags are automatically set:
- **`regionbetter-view`** - for region highlighting
- **`creator`** - region creator flag
- **Additional flags** from configuration

## Limit System

### Limit Checking
The plugin automatically checks:
- **Number of blocks** in selection
- **Number of regions** owned by player
- **Permissions to exceed limits**

### Limit Configuration
Limits are configured via LuckPerms permissions:
```
/lp user <player> permission set regionbetter.selection.5000 true
/lp user <player> permission set regionbetter.regions.10 true
```

## Debugging

To enable debug messages, set in configuration:
```yaml
Config:
  Debug: true
```

Debug messages will show:
- Command execution
- Limit checking
- Event processing
- Player states

## Installation

1. Download the plugin JAR file
2. Place it in the `plugins/` folder
3. Restart the server
4. Configure settings in `plugins/RegionBetter/config.yml`
5. Configure permissions via LuckPerms

## Requirements

- **Java 21+**
- **Bukkit/Spigot/Paper 1.21+**
- **WorldGuard** (required)
- **WorldEdit** (recommended)
- **LuckPerms** (recommended)
- **PlaceholderAPI** (optional)
- **CMI** (optional)
- **SVIS** (optional)

## Support

If you encounter problems:
1. Enable debugging (`Debug: true`)
2. Check server logs
3. Verify configuration is correct
4. Check player permissions

## License

This plugin is distributed under the MIT license.

---

**Documentation Version:** 1.0.1  
**Last Update:** 2025-01-27
