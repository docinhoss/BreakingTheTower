# Architecture Research: Pathfinding Integration

**Domain:** A* pathfinding for RTS/god game
**Researched:** 2026-02-05
**Context:** Subsequent milestone adding pathfinding to existing v1 architecture
**Confidence:** HIGH (existing architecture analyzed, patterns well-established)

## Executive Summary

The v1 architecture was specifically designed for pathfinding integration. The existing `MovementSystem`, `NavigationGrid`, and `MovementRequest/MovementResult` types provide clean integration points. A* pathfinding can be added as a new `MovementStrategy` implementation that:

1. Queries `NavigationGrid.isWalkable()` for walkability
2. Returns the next step on the computed path
3. Caches paths and invalidates on obstacle changes or arrival

The recommended approach is **strategy pattern with path caching** - where pathfinding becomes a pluggable movement strategy that can coexist with the existing direct movement behavior.

## Integration Points

### Existing Architecture Summary

The v1 milestone established these components:

| Component | Role | Location |
|-----------|------|----------|
| `MovementSystem` | Single source of truth for movement execution | `movement/MovementSystem.java` |
| `NavigationGrid` | Interface for walkability/collision queries | `navigation/NavigationGrid.java` |
| `MovementRequest` | Intent record (entity, targetX, targetY) | `movement/MovementRequest.java` |
| `MovementResult` | Sealed result (Moved/Blocked) | `movement/MovementResult.java` |
| `Island` | Implements `NavigationGrid` | `Island.java` |

### Where Pathfinding Plugs In

**Primary Integration Point:** New `Pathfinder` service that computes paths using `NavigationGrid`.

```
Current Flow:
  Peon.tick() → calculates direction to target → MovementSystem.move(request)
                                                        ↓
                                            NavigationGrid.isFree() → position update

Pathfinding Flow:
  Peon.tick() → Pathfinder.getNextStep(current, target) → MovementSystem.move(request)
                        ↓                                          ↓
           NavigationGrid.isWalkable()              NavigationGrid.isFree() → position update
                        ↓
               A* algorithm → cached path → next waypoint
```

**Key Insight:** The pathfinder does NOT replace `MovementSystem`. It provides the direction/next position; `MovementSystem` still executes the actual move with collision detection. This separation allows:
- Pathfinder to work at grid resolution
- MovementSystem to handle sub-grid movement and dynamic collisions
- Entity-entity collision still handled by `isFree()` even on a valid path

### Integration Contract

The pathfinder should:

1. **Input:** Current position (x, y), target position (targetX, targetY), NavigationGrid reference
2. **Output:** Next position to move toward (or null if no path exists)
3. **Query:** Uses `NavigationGrid.isWalkable(x, y)` for static obstacle queries

The existing `NavigationGrid.isOnGround(x, y)` method checks walkable terrain. The `isFree()` method checks entity collisions. Pathfinding uses `isOnGround()` for grid queries (static obstacles); `MovementSystem` uses `isFree()` for dynamic collision (entities).

## New Components

### 1. PathfindingGrid Interface

Extends `NavigationGrid` with grid-specific queries for A*.

```java
package com.mojang.tower.navigation;

/**
 * Grid-based navigation queries for pathfinding algorithms.
 * Provides discrete cell-based walkability for A* and similar algorithms.
 */
public interface PathfindingGrid extends NavigationGrid {
    /**
     * Check if a grid cell is walkable (no static obstacles).
     * Uses discrete grid coordinates, not world coordinates.
     */
    boolean isWalkable(int gridX, int gridY);

    /**
     * Convert world coordinates to grid coordinates.
     */
    int toGridX(double worldX);
    int toGridY(double worldY);

    /**
     * Convert grid coordinates to world coordinates (cell center).
     */
    double toWorldX(int gridX);
    double toWorldY(int gridY);

    /**
     * Get the grid dimensions.
     */
    int getWidth();
    int getHeight();
}
```

**Rationale:** A* operates on discrete cells, but entities use continuous coordinates. This interface handles the conversion and provides grid-resolution walkability queries.

### 2. GridCell Record

Immutable grid position for A* node representation.

```java
package com.mojang.tower.pathfinding;

/**
 * Immutable grid cell position.
 * Used as keys in A* open/closed sets.
 */
public record GridCell(int x, int y) {
    public double distanceTo(GridCell other) {
        int dx = other.x - x;
        int dy = other.y - y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public int manhattanDistance(GridCell other) {
        return Math.abs(other.x - x) + Math.abs(other.y - y);
    }
}
```

**Rationale:** Records provide equals/hashCode automatically, essential for HashMap/HashSet usage in A*. Immutability ensures safe use as map keys.

### 3. Path Record

Represents a computed path as a list of waypoints.

```java
package com.mojang.tower.pathfinding;

import java.util.List;

/**
 * A computed path from start to goal.
 * Immutable - paths are computed once and consumed.
 */
public record Path(
    GridCell start,
    GridCell goal,
    List<GridCell> waypoints  // Includes goal, excludes start
) {
    public boolean isEmpty() {
        return waypoints.isEmpty();
    }

    public GridCell nextWaypoint() {
        return waypoints.isEmpty() ? null : waypoints.get(0);
    }

    public Path advanceToNext() {
        if (waypoints.size() <= 1) {
            return new Path(start, goal, List.of());
        }
        return new Path(waypoints.get(0), goal, waypoints.subList(1, waypoints.size()));
    }
}
```

**Rationale:** Immutable path allows safe caching and sharing. `advanceToNext()` creates new Path instance when entity reaches a waypoint.

### 4. AStarPathfinder Class

Core A* algorithm implementation.

```java
package com.mojang.tower.pathfinding;

import com.mojang.tower.navigation.PathfindingGrid;
import java.util.*;

/**
 * A* pathfinding algorithm implementation.
 * Finds shortest paths on a PathfindingGrid.
 */
public final class AStarPathfinder {
    private final PathfindingGrid grid;

    // Directions: 8-way movement (orthogonal + diagonal)
    private static final int[][] DIRECTIONS = {
        {-1, 0}, {1, 0}, {0, -1}, {0, 1},  // orthogonal
        {-1, -1}, {-1, 1}, {1, -1}, {1, 1}  // diagonal
    };
    private static final double DIAGONAL_COST = Math.sqrt(2);

    public AStarPathfinder(PathfindingGrid grid) {
        this.grid = grid;
    }

    /**
     * Find path from start to goal.
     * @return Path if found, or Path with empty waypoints if no path exists
     */
    public Path findPath(GridCell start, GridCell goal) {
        // A* implementation using PriorityQueue
        // ... (standard A* algorithm)
    }
}
```

**Rationale:** Stateless pathfinder that takes grid reference. Can be reused for multiple path requests. 8-way movement matches game's free movement.

### 5. PathCache Class

Caches computed paths with invalidation support.

```java
package com.mojang.tower.pathfinding;

import com.mojang.tower.Entity;
import java.util.*;

/**
 * Caches paths for entities to avoid recomputation.
 * Invalidates paths when obstacles change or goals change.
 */
public final class PathCache {
    private final Map<Entity, CachedPath> cache = new HashMap<>();
    private final AStarPathfinder pathfinder;

    public PathCache(AStarPathfinder pathfinder) {
        this.pathfinder = pathfinder;
    }

    /**
     * Get cached path or compute new one.
     */
    public Path getPath(Entity entity, GridCell start, GridCell goal) {
        CachedPath cached = cache.get(entity);
        if (cached != null && cached.goal.equals(goal) && !cached.isStale()) {
            return cached.path;
        }
        Path path = pathfinder.findPath(start, goal);
        cache.put(entity, new CachedPath(goal, path, System.currentTimeMillis()));
        return path;
    }

    /**
     * Invalidate path for entity (call when obstacle blocks path).
     */
    public void invalidate(Entity entity) {
        cache.remove(entity);
    }

    /**
     * Invalidate all paths (call when major world change).
     */
    public void invalidateAll() {
        cache.clear();
    }

    private record CachedPath(GridCell goal, Path path, long timestamp) {
        boolean isStale() {
            // Paths older than 5 seconds are considered stale
            return System.currentTimeMillis() - timestamp > 5000;
        }
    }
}
```

**Rationale:** Per-entity caching avoids recomputation when entities continue toward same goal. Time-based staleness handles world changes not explicitly tracked.

### 6. PathfindingService Class

High-level service coordinating pathfinding for entities.

```java
package com.mojang.tower.pathfinding;

import com.mojang.tower.Entity;
import com.mojang.tower.navigation.PathfindingGrid;
import com.mojang.tower.Vec;

/**
 * High-level pathfinding service.
 * Coordinates grid conversion, path computation, and caching.
 */
public final class PathfindingService {
    private final PathfindingGrid grid;
    private final AStarPathfinder pathfinder;
    private final PathCache cache;

    public PathfindingService(PathfindingGrid grid) {
        this.grid = grid;
        this.pathfinder = new AStarPathfinder(grid);
        this.cache = new PathCache(pathfinder);
    }

    /**
     * Get next position entity should move toward.
     * @return World coordinates of next waypoint, or null if no path
     */
    public Vec getNextPosition(Entity entity, double targetX, double targetY) {
        GridCell start = new GridCell(grid.toGridX(entity.x), grid.toGridY(entity.y));
        GridCell goal = new GridCell(grid.toGridX(targetX), grid.toGridY(targetY));

        // If already at goal cell, return exact target
        if (start.equals(goal)) {
            return new Vec(targetX, targetY, 0);
        }

        Path path = cache.getPath(entity, start, goal);
        if (path.isEmpty()) {
            return null;  // No path exists
        }

        GridCell next = path.nextWaypoint();
        return new Vec(grid.toWorldX(next.x()), grid.toWorldY(next.y()), 0);
    }

    /**
     * Notify that entity reached a waypoint (advance path).
     */
    public void waypointReached(Entity entity) {
        // Update cached path to next segment
    }

    /**
     * Invalidate path when entity is blocked.
     */
    public void pathBlocked(Entity entity) {
        cache.invalidate(entity);
    }
}
```

**Rationale:** Single service hides complexity of grid conversion and caching. Entities only need to call `getNextPosition()`.

## Modified Components

### 1. Island.java

Add `PathfindingGrid` implementation.

**Changes:**
- Implement `PathfindingGrid` interface (extends `NavigationGrid`)
- Add grid conversion methods based on existing `isOnGround()` scale factor (1.5)
- Grid resolution: recommend 1 grid cell = 3 world units (matches entity radius ~1-2)

```java
// Add to Island.java
@Override
public boolean isWalkable(int gridX, int gridY) {
    double worldX = toWorldX(gridX);
    double worldY = toWorldY(gridY);
    return isOnGround(worldX, worldY);
}

@Override
public int toGridX(double worldX) {
    return (int) Math.floor((worldX + 192) / 3);  // 256 * 1.5 / 2 = 192 offset
}
// ... similar for other methods
```

**Risk:** LOW - adds methods, no behavior change to existing code.

### 2. Peon.java

Integrate pathfinding into movement calculation.

**Changes:**
- When Job has target, query `PathfindingService.getNextPosition()` instead of direct angle calculation
- On `MovementResult.Blocked`, call `PathfindingService.pathBlocked()` to invalidate
- Preserve existing random wander behavior when no job

**Affected lines:** 117-130 (target direction calculation)

```java
// Current:
if (wanderTime == 0 && job != null && job.hasTarget()) {
    double xd = job.xTarget - x;
    double yd = job.yTarget - y;
    // ... direct angle to target
    rot = Math.atan2(yd, xd);
}

// With pathfinding:
if (wanderTime == 0 && job != null && job.hasTarget()) {
    Vec next = pathfindingService.getNextPosition(this, job.xTarget, job.yTarget);
    if (next != null) {
        rot = Math.atan2(next.y() - y, next.x() - x);
    } else {
        job.cantReach();  // No path exists
    }
}
```

**Risk:** MEDIUM - core movement logic change, needs golden master validation.

### 3. Monster.java

Similar pathfinding integration for monster movement.

**Affected lines:** 67-79 (target direction calculation)

**Risk:** MEDIUM - same pattern as Peon.

### 4. ServiceLocator.java

Add `PathfindingService` registration.

**Changes:**
- Add `pathfindingService` field
- Add `provide(PathfindingService)` and `pathfinding()` methods
- Update `reset()` to clear pathfinding service

**Risk:** LOW - follows existing pattern for `MovementSystem`.

### 5. TowerComponent.java

Wire pathfinding service at initialization.

**Changes:**
- Create `PathfindingService` after `Island` creation
- Register with `ServiceLocator.provide()`

**Risk:** LOW - follows existing initialization pattern.

## Data Flow

### Complete Path Request Flow

```
1. Peon.tick()
   |
   +-- job.hasTarget() → true, job has (xTarget, yTarget)
   |
   +-- ServiceLocator.pathfinding().getNextPosition(this, xTarget, yTarget)
       |
       +-- PathfindingService.getNextPosition()
           |
           +-- Convert world coords to grid coords
           |
           +-- PathCache.getPath(entity, start, goal)
               |
               +-- Cache hit? → return cached path
               |
               +-- Cache miss:
                   |
                   +-- AStarPathfinder.findPath(start, goal)
                       |
                       +-- PathfindingGrid.isWalkable() queries
                       |
                       +-- A* algorithm with PriorityQueue
                       |
                       +-- Return Path(waypoints)
                   |
                   +-- Store in cache
           |
           +-- Path.nextWaypoint() → GridCell
           |
           +-- Convert grid coords to world coords → Vec
   |
   +-- Calculate rotation: Math.atan2(next.y - y, next.x - x)
   |
   +-- Create MovementRequest with next step direction
   |
   +-- ServiceLocator.movement().move(request)
       |
       +-- MovementSystem.move()
           |
           +-- NavigationGrid.isFree() → collision with entities
           |
           +-- Return MovementResult.Moved or Blocked

2. If Blocked:
   |
   +-- ServiceLocator.pathfinding().pathBlocked(this)
   |
   +-- PathCache.invalidate(entity)
   |
   +-- Next tick: recompute path around obstacle
```

### Path Invalidation Triggers

| Trigger | Action | Scope |
|---------|--------|-------|
| `MovementResult.Blocked` | `pathBlocked(entity)` | Single entity |
| Entity reaches waypoint | `waypointReached(entity)` | Single entity |
| Job target changes | Cache checks goal mismatch | Single entity |
| Time-based staleness | 5 second expiry | Single entity |
| Major world change (building placed) | `invalidateAll()` | All entities |

## Build Order

Recommended implementation sequence based on dependencies:

### Phase 1: Foundation (No Behavior Change)

| Step | Component | Why First |
|------|-----------|-----------|
| 1.1 | `GridCell` record | No dependencies, used by everything |
| 1.2 | `Path` record | Depends only on GridCell |
| 1.3 | `PathfindingGrid` interface | Extends NavigationGrid |

**Verification:** Compiles, no runtime changes.

### Phase 2: Algorithm Implementation

| Step | Component | Depends On |
|------|-----------|------------|
| 2.1 | `AStarPathfinder` class | GridCell, Path, PathfindingGrid |
| 2.2 | Unit tests for A* | AStarPathfinder |

**Verification:** Unit tests pass for pathfinding algorithm in isolation.

### Phase 3: Island Grid Integration

| Step | Component | Depends On |
|------|-----------|------------|
| 3.1 | `Island` implements `PathfindingGrid` | PathfindingGrid interface |
| 3.2 | Grid conversion tests | Island changes |

**Verification:** Grid queries work correctly, existing behavior unchanged.

### Phase 4: Service Layer

| Step | Component | Depends On |
|------|-----------|------------|
| 4.1 | `PathCache` class | AStarPathfinder, Entity |
| 4.2 | `PathfindingService` class | PathCache, PathfindingGrid |
| 4.3 | `ServiceLocator` registration | PathfindingService |
| 4.4 | `TowerComponent` wiring | ServiceLocator |

**Verification:** Service can be obtained via ServiceLocator.

### Phase 5: Entity Integration

| Step | Component | Depends On |
|------|-----------|------------|
| 5.1 | `Peon` pathfinding integration | PathfindingService |
| 5.2 | `Monster` pathfinding integration | PathfindingService |
| 5.3 | Golden master test validation | All above |

**Verification:** Golden master tests pass, entities navigate around obstacles.

### Dependency Diagram

```
GridCell (record)
    |
    v
Path (record)
    |
    v
PathfindingGrid (interface) ←── NavigationGrid
    |
    +──────────────────────┐
    |                      |
    v                      v
AStarPathfinder       Island (implements)
    |
    v
PathCache
    |
    v
PathfindingService ──────→ ServiceLocator
    |
    +────────────────┐
    |                |
    v                v
  Peon           Monster
```

## Grid Resolution Considerations

The existing world uses continuous coordinates with:
- Island bitmap: 256x256 pixels
- Coordinate range: approximately -192 to +192 (256 * 1.5 / 2)
- Entity radii: 1-2 units (Peon r=1, Monster r=2, House r=4-8)

**Recommended grid resolution:** 3 world units per cell

- Grid size: ~128x128 cells
- Cell size larger than entity radius ensures entities fit in cells
- Balances path accuracy vs. A* performance
- Approximately 16,384 cells - manageable for A*

**Alternative:** 2 units per cell (192x192 grid, ~37,000 cells) for finer paths but more computation.

## Performance Considerations

### A* Complexity

For a 128x128 grid:
- Worst case: O(n log n) where n = 16,384 cells
- Typical case: much lower due to heuristic guidance
- Expected path computation: <1ms on modern hardware

### Caching Strategy

- **Per-entity caching:** Each entity maintains its own cached path
- **Goal-based invalidation:** Path recomputed only when goal changes
- **Time-based expiry:** 5 second staleness threshold catches world changes
- **Explicit invalidation:** `pathBlocked()` triggers recomputation on collision

### When NOT to Cache

- Very short paths (< 5 cells): Recomputation is cheap
- Rapidly changing targets: Cache thrashing overhead
- Consider direct movement fallback for adjacent targets

## Fallback Behavior

The pathfinding system should gracefully handle edge cases:

| Scenario | Behavior |
|----------|----------|
| No path exists | Return null, job calls `cantReach()` |
| Start/goal same cell | Return exact target position (direct movement) |
| Pathfinding takes too long | Time-limit A* iterations, return partial path |
| Grid not initialized | Fall back to direct movement |

## Testing Strategy

### Unit Tests (Phase 2)

- A* finds path on simple grid
- A* returns empty path when blocked
- A* handles diagonal movement
- Heuristic admissibility (never overestimates)

### Integration Tests (Phase 3-4)

- Island grid conversion round-trips correctly
- PathCache invalidation works
- ServiceLocator wiring correct

### Golden Master Tests (Phase 5)

- Existing behavior preserved when pathfinding disabled
- Entities navigate around obstacles
- Jobs complete successfully with pathfinding
- Performance acceptable (no frame drops)

## Sources

### Primary (HIGH confidence)

- [Implementing A* Pathfinding in Java | Baeldung](https://www.baeldung.com/java-a-star-pathfinding)
- [Pathfinding API - gdx-ai Wiki | GitHub](https://github.com/libgdx/gdx-ai/wiki/Pathfinding-API)
- [Dealing with Moving Obstacles | Stanford](http://theory.stanford.edu/~amitp/GameProgramming/MovingObstacles.html)
- Direct codebase analysis of Breaking the Tower v1 architecture

### Secondary (MEDIUM confidence)

- [Pathfinding Architecture Optimizations | Game AI Pro](http://www.gameaipro.com/GameAIPro/GameAIPro_Chapter17_Pathfinding_Architecture_Optimizations.pdf)
- [RTS Pathfinding: Flowfields | jdxdev](https://www.jdxdev.com/blog/2020/05/03/flowfields/)
- [A* Pathfinding Project Documentation](https://arongranberg.com/astar/documentation/stable/optimization.html)
- [Pathfinding with A Star Algorithm in Java | Medium](https://medium.com/@AlexanderObregon/pathfinding-with-the-a-star-algorithm-in-java-3a66446a2352)
- [Dynamic Pathfinding Algorithms in Game Development | peerdh](https://peerdh.com/blogs/programming-insights/dynamic-pathfinding-algorithms-in-game-development)

---

*Architecture research for v2 pathfinding milestone. Integrates with existing v1 MovementSystem architecture.*
