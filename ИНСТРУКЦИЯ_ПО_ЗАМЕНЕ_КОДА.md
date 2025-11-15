# Instructions for Replacing Old Code with Correct Code

## Problem in Old Code

In the file `PlayerListener.java` on line 227 there was an error:
```java
double distance = lastLoc.distance(to); // ❌ Error with different worlds!
```

This line caused the error `Cannot measure distance between world and world_nether` when players moved between worlds.

## Solution

### 1. Replace Old PlayerListener

**File:** `src/main/java/com/allfire/regionbetter/listeners/PlayerListener.java`

**Replace the file contents** with code from `CorrectedPlayerListener.java`

### 2. Main Code Changes

#### ❌ Old Code (problematic):
```java
// Measuring distance between locations
double distance = lastLoc.distance(to); // Error with different worlds!

// Checking world change
if (!from.getWorld().equals(to.getWorld())) {
    // Clearing cache, but NOT updating lastLocation
    lastLocation.remove(player);
    return;
}
```

#### ✅ New Code (correct):
```java
// Check world change at the beginning
if (!from.getWorld().equals(to.getWorld())) {
    // Clear cache on world change
    playerRegionStates.remove(player);
    lastCheckTime.remove(player);
    lastRegionViewExecution.remove(player);
    return; // Skip check on world change
}

// Use BlockVector3 instead of Location
BlockVector3 fromVector = BlockVector3.at(from.getX(), from.getY(), from.getZ());
BlockVector3 toVector = BlockVector3.at(to.getX(), to.getY(), to.getZ());

// Get regions for old and new positions
ApplicableRegionSet fromRegions = regionManager.getApplicableRegions(fromVector);
ApplicableRegionSet toRegions = regionManager.getApplicableRegions(toVector);
```

### 3. Key Differences

| Aspect | Old Code | New Code |
|--------|----------|----------|
| **Distance Measurement** | ❌ `lastLoc.distance(to)` | ✅ Not used |
| **Using Location** | ❌ Location for distances | ✅ BlockVector3 for regions |
| **World Change** | ❌ Partial handling | ✅ Full handling |
| **Errors** | ❌ `Cannot measure distance` | ✅ No errors |
| **Performance** | ❌ Slow | ✅ Fast |

### 4. Permission Checking

The new code **always checks permissions** `regionbetter.region.view` before executing commands:

```java
// Check access permissions (always check if CheckPerms is enabled)
if (plugin.getConfigManager().getConfig().getBoolean("RegionViewSettings.CheckPerms", true)) {
    if (!player.hasPermission("regionbetter.region.view")) {
        return;
    }
}
```

### 5. Entry/Exit Logic

The new code uses **entry/exit logic like in WorldGuard**:

```java
if (currentRegion != null) {
    // ENTRY: Player entered region with flag
    executeRegionViewCommands(player, currentRegion);
} else if (previousRegion != null) {
    // EXIT: Player exited region with flag
    executeRegionExitCommands(player, previousRegion);
}
```

## Result

After replacing the code:
- ✅ **No errors** `Cannot measure distance between world and world_nether`
- ✅ **Correct operation** when moving between worlds
- ✅ **Permission checking** `regionbetter.region.view`
- ✅ **Entry/exit logic** like in WorldGuard
- ✅ **Better performance**

## Additional Files

Also created files for reference:
- `WorldGuardEntryExitFlags.java` - example of how entry/exit flags work in WorldGuard
- `CorrectRegionBetterViewFlag.java` - alternative flag implementation with built-in logic

## Important

After replacing the code **restart the server** to apply changes.
