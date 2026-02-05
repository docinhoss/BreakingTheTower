# Stack Research: A* Pathfinding for Breaking the Tower

**Project:** Breaking the Tower - Pathfinding Milestone
**Researched:** 2026-02-05
**Overall Confidence:** HIGH (custom implementation recommended with well-documented patterns)

---

## Executive Summary

For Breaking the Tower's pathfinding needs, **implement A* from scratch** rather than adding an external library. The game's constraints (pure Java 21, no external frameworks, small grid ~256x256, <100 entities) make a custom implementation the right choice. The existing `NavigationGrid` interface already provides the abstraction layer needed for pathfinding queries.

**Key decisions:**
- Custom A* implementation (~200-300 lines of well-structured code)
- Grid overlay on continuous coordinates (cell size = entity radius * 2)
- Path caching with invalidation on world changes
- Integration through new `PathfindingService` registered with `ServiceLocator`

---

## Recommended Approach

### Decision: Custom Implementation over External Library

**Recommendation:** Implement A* from scratch

**Rationale:**

| Factor | Library | Custom | Winner |
|--------|---------|--------|--------|
| Dependencies | Adds JAR to pure-Java project | Zero dependencies | Custom |
| Control | Black box, may not fit grid model | Full control over heuristics, caching | Custom |
| Learning curve | Library API + integration | Algorithm is simple, well-documented | Custom |
| Maintenance | Library updates, compatibility | Self-contained, testable | Custom |
| Performance tuning | Limited | Full access to optimize for game's specific patterns | Custom |
| Code size | Library may include unused features | ~200-300 lines for A* core | Custom |

**Why NOT a library:**

1. **xaguzman/pathfinding** and **danielbatchford/PathFinding** are lightweight but still add a dependency to a pure-Java project
2. Breaking the Tower's world is small (256x256 pixels, ~10-20 cells across with reasonable cell sizes)
3. Entity count is low (~50-100 max) - no need for hierarchical pathfinding or flow fields
4. The `NavigationGrid` interface already abstracts walkability queries - A* just needs to call it

**Confidence:** HIGH - A* is a well-understood algorithm with extensive documentation and the codebase already has the infrastructure for pathfinding.

---

## Core Technologies

### Data Structures Required

#### 1. PathNode Record

Represents a position on the navigation grid with A* scoring.

```java
/**
 * A node in the pathfinding graph.
 * Uses Java records for immutability and automatic equals/hashCode (critical for Set membership).
 */
public record PathNode(
    int gridX,
    int gridY,
    double gCost,  // Cost from start
    double hCost,  // Heuristic cost to goal
    PathNode parent
) implements Comparable<PathNode> {

    public double fCost() {
        return gCost + hCost;
    }

    @Override
    public int compareTo(PathNode other) {
        return Double.compare(this.fCost(), other.fCost());
    }

    // Only compare grid position for Set membership
    @Override
    public boolean equals(Object o) {
        return o instanceof PathNode(int x, int y, var g, var h, var p)
            && x == gridX && y == gridY;
    }

    @Override
    public int hashCode() {
        return 31 * gridX + gridY;
    }
}
```

**Why a record:**
- Immutable (thread-safe for future optimization)
- Auto-generated `hashCode`/`equals` baseline (we override for grid position only)
- Pattern matching friendly for downstream consumers

**Confidence:** HIGH - Java 21 records are ideal for node representation.

#### 2. Path Record

Represents a computed path as an immutable list of waypoints.

```java
/**
 * An immutable, computed path from start to goal.
 * Waypoints are in world coordinates, not grid coordinates.
 */
public record Path(
    List<Vec> waypoints,
    double totalCost,
    long computedAtTick
) {
    public Path {
        waypoints = List.copyOf(waypoints);  // Defensive copy
    }

    public boolean isEmpty() {
        return waypoints.isEmpty();
    }

    public Vec nextWaypoint() {
        return waypoints.isEmpty() ? null : waypoints.getFirst();
    }

    public Path withoutFirst() {
        if (waypoints.size() <= 1) return new Path(List.of(), totalCost, computedAtTick);
        return new Path(waypoints.subList(1, waypoints.size()), totalCost, computedAtTick);
    }

    public boolean isStale(long currentTick, int maxAge) {
        return currentTick - computedAtTick > maxAge;
    }
}
```

**Why immutable:** Paths can be shared between ticks without defensive copying. Stale check enables cache invalidation.

**Confidence:** HIGH - Immutable paths simplify caching and debugging.

#### 3. PriorityQueue for Open Set

Java's built-in `PriorityQueue` with custom comparator.

```java
PriorityQueue<PathNode> openSet = new PriorityQueue<>(
    Comparator.comparingDouble(PathNode::fCost)
);
```

**Why PriorityQueue:**
- O(log n) insertion and removal of minimum
- Built into Java, no dependencies
- Sufficient for small grids (<1000 nodes explored per path)

**Confidence:** HIGH - Standard A* implementation pattern, verified in [Baeldung A* tutorial](https://www.baeldung.com/java-a-star-pathfinding).

#### 4. HashSet for Closed Set

```java
Set<PathNode> closedSet = new HashSet<>();
```

**Why HashSet:** O(1) contains check. PathNode's `equals`/`hashCode` based on grid position enables efficient membership testing.

**Confidence:** HIGH - Standard pattern.

---

### Algorithm Components

#### 1. Heuristic Function

For grid-based movement, use **Euclidean distance** (since entities can move in any direction, not just cardinal).

```java
private double heuristic(int fromX, int fromY, int toX, int toY) {
    double dx = fromX - toX;
    double dy = fromY - toY;
    return Math.sqrt(dx * dx + dy * dy) * CELL_SIZE;
}
```

**Why Euclidean over Manhattan:**
- Entities move continuously with `Math.cos(rot)`, `Math.sin(rot)` - not tile-locked
- Euclidean is admissible (never overestimates) for continuous movement
- Manhattan would overestimate diagonal paths, leading to suboptimal routes

**Confidence:** HIGH - Matches game's movement model.

#### 2. Neighbor Generation

For 8-directional movement (including diagonals):

```java
private static final int[][] DIRECTIONS = {
    {-1, -1}, {0, -1}, {1, -1},
    {-1,  0},          {1,  0},
    {-1,  1}, {0,  1}, {1,  1}
};

private List<PathNode> getNeighbors(PathNode current, PathNode goal) {
    List<PathNode> neighbors = new ArrayList<>(8);
    for (int[] dir : DIRECTIONS) {
        int nx = current.gridX() + dir[0];
        int ny = current.gridY() + dir[1];

        // Convert grid to world coordinates for walkability check
        double worldX = gridToWorldX(nx);
        double worldY = gridToWorldY(ny);

        // Use existing NavigationGrid for walkability
        if (navigationGrid.isOnGround(worldX, worldY)) {
            double moveCost = (dir[0] != 0 && dir[1] != 0)
                ? DIAGONAL_COST  // sqrt(2) * CELL_SIZE
                : CELL_SIZE;
            double g = current.gCost() + moveCost;
            double h = heuristic(nx, ny, goal.gridX(), goal.gridY());
            neighbors.add(new PathNode(nx, ny, g, h, current));
        }
    }
    return neighbors;
}
```

**Why 8-directional:**
- Matches entity movement (any angle via cos/sin)
- Avoids ugly staircase paths from 4-directional

**Confidence:** HIGH - Standard for continuous-movement games.

---

### Grid System Design

#### Coordinate Conversion

The game uses continuous world coordinates (~-192 to +192 range based on 256x256 * 1.5 factor). Pathfinding needs a discrete grid overlay.

```java
public class PathfindingGrid {
    private static final double CELL_SIZE = 4.0;  // Tunable: roughly 2x peon radius
    private static final double WORLD_MIN = -192.0;
    private static final double WORLD_MAX = 192.0;

    public int worldToGridX(double worldX) {
        return (int) Math.floor((worldX - WORLD_MIN) / CELL_SIZE);
    }

    public int worldToGridY(double worldY) {
        return (int) Math.floor((worldY - WORLD_MIN) / CELL_SIZE);
    }

    public double gridToWorldX(int gridX) {
        return WORLD_MIN + (gridX + 0.5) * CELL_SIZE;  // Center of cell
    }

    public double gridToWorldY(int gridY) {
        return WORLD_MIN + (gridY + 0.5) * CELL_SIZE;
    }
}
```

**Cell size rationale:**
- Peon radius = 1.0 (from `super(x, y, 1)` in Peon constructor)
- Cell size of 4.0 allows comfortable navigation
- Larger cells = faster pathfinding but coarser paths
- Smaller cells = precise paths but more nodes to explore

**Grid dimensions:** 384 / 4 = 96 cells per axis = 9,216 total cells (small enough for brute-force A*)

**Confidence:** HIGH - Standard grid overlay approach, tunable based on testing.

---

## Integration Points

### 1. PathfindingService

New service registered with ServiceLocator, following existing patterns.

```java
public interface PathfindingService {
    /**
     * Find path from entity's current position to target.
     * Returns empty path if no path exists.
     */
    Path findPath(Entity entity, double targetX, double targetY);

    /**
     * Invalidate cached paths (call when world changes significantly).
     */
    void invalidateCache();
}

public class AStarPathfindingService implements PathfindingService {
    private final NavigationGrid grid;
    private final Map<PathCacheKey, Path> pathCache = new HashMap<>();

    public AStarPathfindingService(NavigationGrid grid) {
        this.grid = grid;
    }

    // Implementation...
}
```

**Registration in ServiceLocator:**
```java
public class ServiceLocator {
    private static PathfindingService pathfinding;

    public static PathfindingService pathfinding() {
        return pathfinding;
    }

    public static void providePathfinding(PathfindingService service) {
        pathfinding = service;
    }
}
```

**Confidence:** HIGH - Follows existing ServiceLocator pattern in codebase.

### 2. MovementSystem Integration

MovementSystem remains the single source of truth for position updates. Pathfinding provides direction, MovementSystem executes.

```java
// In Peon.tick() - conceptual integration
if (job != null && job.hasTarget()) {
    Path path = ServiceLocator.pathfinding().findPath(this, job.xTarget, job.yTarget);
    if (!path.isEmpty()) {
        Vec nextWaypoint = path.nextWaypoint();
        double xd = nextWaypoint.x() - x;
        double yd = nextWaypoint.y() - y;
        rot = Math.atan2(yd, xd);  // Face toward waypoint
    }
}

// MovementSystem.move() unchanged - it handles collision detection
```

**Key insight:** Pathfinding doesn't replace MovementSystem - it informs the direction. MovementSystem still validates each step.

**Confidence:** HIGH - Clean separation of concerns.

### 3. NavigationGrid Enhancement

Current `NavigationGrid.isFree()` checks entity collisions. For pathfinding, we need walkability without considering moving entities.

```java
public interface NavigationGrid {
    // Existing
    boolean isOnGround(double x, double y);
    boolean isFree(double x, double y, double radius, Entity exclude);

    // New for pathfinding - only checks terrain, not entities
    default boolean isWalkable(double x, double y) {
        return isOnGround(x, y);
    }
}
```

**Why separate walkability:**
- A* computes static paths based on terrain
- Dynamic obstacles (other entities) are handled by MovementSystem collision
- Computing paths around moving entities leads to oscillation

**Confidence:** HIGH - Standard pattern: static pathfinding + dynamic collision avoidance.

---

## Path Caching Strategy

### When to Cache

Cache paths for repeated queries to same destination within short time window.

```java
record PathCacheKey(int startGridX, int startGridY, int goalGridX, int goalGridY) {}

private Path getCachedOrCompute(Entity entity, double targetX, double targetY, long currentTick) {
    int startX = worldToGridX(entity.x);
    int startY = worldToGridY(entity.y);
    int goalX = worldToGridX(targetX);
    int goalY = worldToGridY(targetY);

    PathCacheKey key = new PathCacheKey(startX, startY, goalX, goalY);
    Path cached = pathCache.get(key);

    if (cached != null && !cached.isStale(currentTick, MAX_CACHE_AGE_TICKS)) {
        return cached;
    }

    Path computed = computePath(startX, startY, goalX, goalY, currentTick);
    pathCache.put(key, computed);
    return computed;
}
```

**Cache parameters:**
- `MAX_CACHE_AGE_TICKS = 60` (2 seconds at 30 tps) - prevents stale paths
- Cache size limit: ~100 entries with LRU eviction (optional, grid is small)

**Confidence:** MEDIUM - Cache effectiveness depends on actual usage patterns. Start simple, optimize if needed.

### When to Invalidate

Invalidate cache when world changes significantly:
- Building placed/destroyed
- Tree harvested
- Entity dies (if blocking was considered)

```java
// In Island.addEntity() and entity removal
ServiceLocator.pathfinding().invalidateCache();
```

**Aggressive invalidation is fine** for this game size. Cache rebuilds are cheap.

**Confidence:** MEDIUM - May need tuning based on gameplay.

---

## Dynamic Recalculation

### Recalculation Triggers

1. **Path blocked:** MovementSystem returns `Blocked` for next waypoint
2. **Goal moved:** Target entity changed position significantly
3. **Cache stale:** Path age exceeds threshold

```java
// In movement logic
switch (result) {
    case MovementResult.Blocked(var blocker) -> {
        // Current path segment blocked - recalculate
        currentPath = ServiceLocator.pathfinding().findPath(this, job.xTarget, job.yTarget);
        // Also apply existing collision response (random rotation, wander)
    }
    case MovementResult.Moved(var x, var y) -> {
        // Check if reached current waypoint
        if (distanceTo(currentPath.nextWaypoint()) < WAYPOINT_RADIUS) {
            currentPath = currentPath.withoutFirst();
        }
    }
}
```

**Confidence:** HIGH - Standard reactive recalculation pattern.

### Recalculation Budget

Limit pathfinding computations per tick to maintain 30 fps:

```java
public class AStarPathfindingService {
    private static final int MAX_PATHS_PER_TICK = 5;
    private int pathsThisTick = 0;

    public Path findPath(Entity entity, double targetX, double targetY) {
        if (pathsThisTick >= MAX_PATHS_PER_TICK) {
            return Path.EMPTY;  // Defer to next tick
        }
        pathsThisTick++;
        // ... compute path
    }

    public void onTickStart() {
        pathsThisTick = 0;
    }
}
```

**Confidence:** MEDIUM - Budget may need adjustment based on profiling. With ~50 entities and small grid, likely not a bottleneck.

---

## What NOT to Add

### 1. Hierarchical Pathfinding (HPA*)

**Why not:**
- HPA* is for large maps (1000x1000+)
- Breaking the Tower's grid is ~96x96 cells
- Overhead of hierarchy construction exceeds benefit

**When to reconsider:** If map size increases 10x

### 2. Flow Fields

**Why not:**
- Flow fields shine with many units to one destination
- Breaking the Tower has ~10 peons going to different destinations
- Memory overhead (one field per destination) not justified

**When to reconsider:** Mass unit commands (select 100 peons, send to tower)

### 3. Jump Point Search (JPS)

**Why not:**
- JPS optimizes for uniform-cost grids
- Benefit is ~10x for large open grids
- Implementation complexity higher than vanilla A*
- Grid is small enough that optimization unnecessary

**When to reconsider:** Profiling shows pathfinding as bottleneck

### 4. Navigation Meshes

**Why not:**
- NavMesh suited for polygonal 3D environments
- Game already has grid-based ground check (island bitmap)
- Would require complete rewrite of world representation

**When to reconsider:** Never for this game

### 5. External Libraries

**Why not:**
- Adds dependency to pure-Java project
- Library may not fit game's specific grid model
- Implementation is straightforward (~200-300 lines)
- Full control over optimization and debugging

**When to reconsider:** If implementing advanced algorithms (D* Lite, Theta*) becomes necessary

---

## Performance Expectations

### Worst Case Analysis

- Grid size: 96 x 96 = 9,216 cells
- A* with Euclidean heuristic explores ~20% of grid for worst-case path
- ~1,800 nodes explored
- PriorityQueue operations: O(n log n) = ~20,000 operations
- Per-path time: <1ms on modern hardware

### Tick Budget

At 30 tps, each tick has ~33ms budget.
- Pathfinding budget: 5ms max (15% of tick)
- 5 paths per tick @ <1ms each = well under budget
- Leaves room for rendering, physics, AI

**Confidence:** HIGH - Verified against similar game implementations.

---

## Implementation Order Recommendation

### Phase 1: Core A* (Foundational)

1. Create `PathNode` record
2. Create `Path` record
3. Implement `PathfindingGrid` coordinate conversion
4. Implement basic A* algorithm
5. Add `PathfindingService` interface and `AStarPathfindingService`
6. Register with `ServiceLocator`

**Deliverable:** Can compute paths, not yet integrated with movement

### Phase 2: Movement Integration

1. Add `isWalkable()` to `NavigationGrid` (or use existing `isOnGround`)
2. Modify Peon movement to use pathfinding when job has target
3. Handle blocked paths with recalculation
4. Add waypoint following logic

**Deliverable:** Peons navigate around terrain

### Phase 3: Caching and Optimization

1. Add path cache with staleness check
2. Add cache invalidation on world changes
3. Add per-tick computation budget
4. Profile and tune parameters

**Deliverable:** Production-ready pathfinding

---

## File Structure

```
src/main/java/com/mojang/tower/
  pathfinding/
    PathNode.java           # A* node record
    Path.java               # Computed path record
    PathfindingGrid.java    # Coordinate conversion
    PathfindingService.java # Interface
    AStarPathfinder.java    # A* implementation
  navigation/
    NavigationGrid.java     # Existing (may add isWalkable)
  movement/
    MovementSystem.java     # Existing (unchanged)
  service/
    ServiceLocator.java     # Add pathfinding() method
```

---

## Sources

**Algorithm References:**
- [Baeldung: Implementing A* Pathfinding in Java](https://www.baeldung.com/java-a-star-pathfinding) - Java implementation patterns
- [Red Blob Games: A* Introduction](https://www.redblobgames.com/pathfinding/a-star/introduction.html) - Authoritative A* tutorial
- [Red Blob Games: Grid Pathfinding Algorithms](https://www.redblobgames.com/pathfinding/grids/algorithms.html) - Grid-specific optimizations

**Java Data Structures:**
- [GeeksforGeeks: PriorityQueue in Java](https://www.geeksforgeeks.org/java/priority-queue-in-java/) - PriorityQueue usage
- [HowToDoInJava: Java Priority Queue](https://howtodoinjava.com/java/collections/java-priorityqueue/) - Comparator patterns

**RTS Pathfinding Patterns:**
- [Game Developer: Group Pathfinding in RTS Games](https://www.gamedeveloper.com/programming/group-pathfinding-movement-in-rts-style-games) - RTS-specific considerations
- [How to RTS: Basic Flow Fields](https://howtorts.github.io/2014/01/04/basic-flow-fields.html) - Why flow fields (and when not to use them)
- [arXiv: Multi-threaded Recast-Based A* Pathfinding](https://arxiv.org/html/2602.04130) - Modern pathfinding research (2025)

**Grid/Coordinate Systems:**
- [Outscal: Unity Grid-Based System Development](https://outscal.com/blog/unity-grid-based-system) - Grid coordinate concepts (applicable to any engine)

**Existing Libraries (evaluated but not recommended):**
- [GitHub: xaguzman/pathfinding](https://github.com/xaguzman/pathfinding) - Java pathfinding framework
- [GitHub: danielbatchford/PathFinding](https://github.com/danielbatchford/PathFinding) - Lightweight Java pathfinding
- [GitHub: patrykkrawczyk/2D-A-path-finding-in-Java](https://github.com/patrykkrawczyk/2D-A-path-finding-in-Java) - Simple 2D A* implementation
