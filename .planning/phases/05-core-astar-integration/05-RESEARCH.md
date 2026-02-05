# Phase 5: Core A* and Integration - Research

**Researched:** 2026-02-05
**Domain:** A* pathfinding algorithm, grid-based navigation, Java implementation
**Confidence:** HIGH

## Summary

This phase implements the core A* pathfinding algorithm for peons to navigate around obstacles. The research focused on: (1) deterministic A* implementation patterns for Java that preserve golden master compatibility, (2) 8-directional movement with proper diagonal cost handling, and (3) integration with the existing MovementSystem and NavigationGrid interfaces.

The codebase is well-prepared for pathfinding integration. Phase 4 established the NavigationGrid interface that abstracts walkability queries (`isOnGround`, `isFree`). The MovementSystem already handles collision detection and position updates. Peons currently move directly toward targets and bounce randomly when blocked. Pathfinding will replace this with computed routes.

Key insight: The game world uses continuous coordinates (doubles), not a discrete grid. Pathfinding needs a discrete grid for A*, but movement remains continuous. This requires converting between world coordinates and grid cells, and following the path waypoint-by-waypoint while using MovementSystem for actual movement.

**Primary recommendation:** Implement A* with a deterministic priority queue (TreeSet or PriorityQueue with tie-breaking comparator), use Octile/Chebyshev heuristic for 8-directional movement, store paths as List<GridCell>, and have peons follow paths waypoint-by-waypoint using existing MovementSystem.

## Standard Stack

The established libraries/tools for this domain:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Java PriorityQueue | 21 LTS | Open set for A* | O(log n) insert/remove, built-in |
| Java LinkedHashMap | 21 LTS | Closed set with deterministic iteration | O(1) lookup, insertion-order iteration |
| Java ArrayList | 21 LTS | Path storage | O(1) access, cache-friendly |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Java record | 21 LTS | GridCell, PathNode types | Immutable value types for nodes |
| Java sealed interface | 21 LTS | PathResult (Found/NotFound) | Exhaustive result handling |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| PriorityQueue | TreeSet | TreeSet guarantees deterministic tie-breaking but slower; use PriorityQueue with custom comparator instead |
| LinkedHashMap for closed set | HashSet | HashSet is faster but non-deterministic iteration; LinkedHashMap ensures consistent behavior |
| ArrayList for path | ArrayDeque | ArrayDeque better for stack-like access but ArrayList simpler for indexed access |

**Installation:**
```bash
# No installation needed - all Java 21 built-in types
```

## Architecture Patterns

### Recommended Project Structure
```
src/main/java/com/mojang/tower/
├── pathfinding/                    # NEW: Pathfinding subsystem
│   ├── PathfindingService.java     # Public API, registered with ServiceLocator
│   ├── AStarPathfinder.java        # A* algorithm implementation
│   ├── GridCell.java               # record(int x, int y) for grid coordinates
│   ├── PathNode.java               # Internal A* node with g, h, f, parent
│   └── PathResult.java             # sealed interface: Found(List<GridCell>) | NotFound
├── navigation/
│   └── NavigationGrid.java         # Existing interface (from Phase 4)
├── movement/
│   └── MovementSystem.java         # Existing movement execution
└── service/
    └── ServiceLocator.java         # Add PathfindingService registration
```

### Pattern 1: PathfindingService Facade
**What:** A service class that provides the public API for pathfinding, hiding A* implementation details.
**When to use:** Always - keeps pathfinding logic encapsulated and testable.
**Example:**
```java
// Source: Research synthesis
public final class PathfindingService {
    private final NavigationGrid grid;
    private final AStarPathfinder pathfinder;

    public PathfindingService(NavigationGrid grid) {
        this.grid = grid;
        this.pathfinder = new AStarPathfinder(grid);
    }

    /**
     * Find a path from world coordinates to target world coordinates.
     * @return PathResult.Found with waypoints, or PathResult.NotFound
     */
    public PathResult findPath(double fromX, double fromY, double toX, double toY) {
        GridCell start = worldToGrid(fromX, fromY);
        GridCell goal = worldToGrid(toX, toY);
        return pathfinder.findPath(start, goal);
    }

    private GridCell worldToGrid(double x, double y) {
        // World: -192 to +192 (256 * 1.5 / 2)
        // Grid: 0-95 (choosing 96x96 cells, ~4 world units per cell)
        int gx = (int) ((x + 192) / 4);
        int gy = (int) ((y + 192) / 4);
        return new GridCell(
            Math.clamp(gx, 0, 95),
            Math.clamp(gy, 0, 95)
        );
    }
}
```

### Pattern 2: Deterministic A* with Tie-Breaking
**What:** A* implementation using PriorityQueue with a comparator that breaks ties deterministically.
**When to use:** Always in games with golden master tests - standard PriorityQueue breaks ties arbitrarily.
**Example:**
```java
// Source: Stanford GameProgramming + Java PriorityQueue docs
// Tie-breaking: when f-scores equal, prefer higher g (closer to goal in search)
// Secondary: use grid coordinates for final determinism
Comparator<PathNode> comparator = Comparator
    .comparingInt(PathNode::f)           // Primary: lowest f first
    .thenComparingInt(PathNode::g)       // Tie-break: prefer higher g (reversed)
    .reversed()                           // Reverse to get higher g first
    .thenComparingInt(n -> n.cell().x()) // Final tie-break: x coordinate
    .thenComparingInt(n -> n.cell().y()); // Final tie-break: y coordinate

PriorityQueue<PathNode> openSet = new PriorityQueue<>(comparator);
```

### Pattern 3: GridCell Value Type
**What:** Immutable record for grid coordinates with precomputed hashCode.
**When to use:** For all grid coordinate handling - enables use as HashMap key.
**Example:**
```java
// Source: Java records best practice
public record GridCell(int x, int y) {
    // Record auto-generates equals(), hashCode(), toString()
    // These are deterministic based on x and y values
}
```

### Pattern 4: Octile Heuristic for 8-Directional Movement
**What:** Heuristic that accounts for diagonal movement costing sqrt(2) more than cardinal.
**When to use:** For 8-directional grid movement with diagonal cost != cardinal cost.
**Example:**
```java
// Source: theory.stanford.edu/~amitp/GameProgramming/Heuristics.html
// D = cardinal cost (10), D2 = diagonal cost (14, approximates 10*sqrt(2))
private static final int D = 10;   // Cardinal movement cost
private static final int D2 = 14;  // Diagonal movement cost (10 * 1.414...)

private int heuristic(GridCell from, GridCell to) {
    int dx = Math.abs(from.x() - to.x());
    int dy = Math.abs(from.y() - to.y());
    // Octile distance: straight moves + diagonal bonus
    return D * (dx + dy) + (D2 - 2 * D) * Math.min(dx, dy);
}
```

### Pattern 5: Path Following via Waypoints
**What:** Store path as list of grid cells, peon moves toward current waypoint using MovementSystem.
**When to use:** Always - separates pathfinding (discrete) from movement (continuous).
**Example:**
```java
// In Peon or a PathFollower component
private List<GridCell> path;
private int currentWaypointIndex;

public void followPath() {
    if (path == null || currentWaypointIndex >= path.size()) {
        path = null;
        return;
    }

    GridCell waypoint = path.get(currentWaypointIndex);
    double targetX = gridToWorldX(waypoint);
    double targetY = gridToWorldY(waypoint);

    // Check if close enough to waypoint
    double dx = targetX - x;
    double dy = targetY - y;
    if (dx * dx + dy * dy < 4.0) { // Within ~2 world units
        currentWaypointIndex++;
        return;
    }

    // Move toward waypoint using existing movement logic
    rot = Math.atan2(dy, dx);
    // ... existing movement code using MovementSystem
}
```

### Anti-Patterns to Avoid
- **HashMap for closed set:** Use LinkedHashMap - HashMap iteration order is non-deterministic and will break golden master tests.
- **Floating-point in heuristic:** Use integer costs (10/14) not floats (1.0/1.414) - floating-point comparison issues cause non-determinism.
- **Recomputing path every tick:** Cache path and only recompute when blocked or target moves significantly.
- **Skipping NavigationGrid:** Always use NavigationGrid.isOnGround() for walkability - don't duplicate logic.
- **Storing Entity references in path:** Store GridCell coordinates, not Entity references - entities move.

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Priority queue | Custom linked list | Java PriorityQueue | Binary heap is O(log n), linked list is O(n) |
| Coordinate hashing | Custom hash function | Java record hashCode | Records generate consistent hashCode |
| Square root for diagonal | Math.sqrt(2) | Integer constant 14 | Avoids floating-point comparison issues |
| Path smoothing | Custom algorithm | None for v2 | Out of scope; paths will be "blocky" but functional |

**Key insight:** A* is a well-understood algorithm. The value is in correct integration with existing systems (NavigationGrid, MovementSystem, ServiceLocator) not in algorithmic novelty.

## Common Pitfalls

### Pitfall 1: Non-Deterministic Tie-Breaking
**What goes wrong:** Same scenario produces different paths across runs, breaking golden master tests.
**Why it happens:** PriorityQueue breaks ties arbitrarily; HashMap iteration order varies.
**How to avoid:** Use comparator with secondary sort keys (g-score, then coordinates). Use LinkedHashMap for closed set.
**Warning signs:** Golden master test fails intermittently or after code changes that "shouldn't" affect paths.

### Pitfall 2: Heuristic Overestimates (Inadmissible)
**What goes wrong:** A* finds suboptimal paths or fails to find paths that exist.
**Why it happens:** Heuristic returns value greater than actual cost, violating A* guarantee.
**How to avoid:** For 8-directional movement, use Octile distance which is admissible. Never use Euclidean for grid-based movement.
**Warning signs:** Paths go through obstacles or are clearly longer than necessary.

### Pitfall 3: Grid-World Coordinate Mismatch
**What goes wrong:** Peon walks into walls, pathfinding returns "no path" when path clearly exists.
**Why it happens:** World uses continuous doubles (-192 to +192), grid uses discrete ints (0-95). Off-by-one in conversion.
**How to avoid:** Centralize world<->grid conversion in PathfindingService. Test edge cases explicitly.
**Warning signs:** Problems at world edges or near grid cell boundaries.

### Pitfall 4: Entity Collision vs Grid Walkability
**What goes wrong:** Path goes through spaces occupied by entities, or avoids empty spaces.
**Why it happens:** Confusion between NavigationGrid.isOnGround() (terrain) and NavigationGrid.isFree() (terrain + entities).
**How to avoid:** For path computation, check TERRAIN walkability only (isOnGround). Entity collisions handled at movement time by MovementSystem.
**Warning signs:** Paths avoid areas where peons once stood; paths go through rocks.

### Pitfall 5: Forgetting to Handle "No Path Found"
**What goes wrong:** NullPointerException or infinite loops when path cannot be found.
**Why it happens:** Target surrounded by obstacles, or off-map. Code assumes path always exists.
**How to avoid:** Return sealed PathResult (Found | NotFound). Always handle NotFound case.
**Warning signs:** Crashes when clicking on unreachable locations.

### Pitfall 6: Recomputing Path Every Tick
**What goes wrong:** Performance drops, game stutters with multiple peons.
**Why it happens:** Calling findPath() every tick() instead of caching result.
**How to avoid:** Cache path in Peon. Only recompute when: blocked mid-path, target moved significantly, or path completed.
**Warning signs:** FPS drops proportional to number of peons.

## Code Examples

Verified patterns for this codebase:

### GridCell Record
```java
// Source: Java 21 records + codebase convention
package com.mojang.tower.pathfinding;

/**
 * Represents a discrete cell in the pathfinding grid.
 * Immutable value type suitable for use as HashMap key.
 */
public record GridCell(int x, int y) {
    public static final int GRID_SIZE = 96; // 96x96 grid

    public boolean isValid() {
        return x >= 0 && x < GRID_SIZE && y >= 0 && y < GRID_SIZE;
    }
}
```

### PathResult Sealed Interface
```java
// Source: Codebase pattern (MovementResult)
package com.mojang.tower.pathfinding;

import java.util.List;

/**
 * Result of a pathfinding query.
 * Either a path was found, or the target is unreachable.
 */
public sealed interface PathResult {
    record Found(List<GridCell> path) implements PathResult {}
    record NotFound(String reason) implements PathResult {}
}
```

### A* Core Algorithm
```java
// Source: theory.stanford.edu/~amitp/GameProgramming/ImplementationNotes.html + Java adaptation
package com.mojang.tower.pathfinding;

import java.util.*;

final class AStarPathfinder {
    private static final int D = 10;   // Cardinal cost
    private static final int D2 = 14;  // Diagonal cost
    private static final int MAX_NODES = 1000; // Prevent unbounded search

    // 8 directions: N, NE, E, SE, S, SW, W, NW
    private static final int[][] DIRECTIONS = {
        {0, -1}, {1, -1}, {1, 0}, {1, 1},
        {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}
    };

    private final NavigationGrid grid;

    AStarPathfinder(NavigationGrid grid) {
        this.grid = grid;
    }

    PathResult findPath(GridCell start, GridCell goal) {
        if (!isWalkable(start)) return new PathResult.NotFound("Start not walkable");
        if (!isWalkable(goal)) return new PathResult.NotFound("Goal not walkable");
        if (start.equals(goal)) return new PathResult.Found(List.of(goal));

        // Deterministic comparator: f, then g (descending), then x, then y
        Comparator<PathNode> cmp = Comparator
            .comparingInt(PathNode::f)
            .thenComparing(Comparator.comparingInt(PathNode::g).reversed())
            .thenComparingInt(n -> n.cell().x())
            .thenComparingInt(n -> n.cell().y());

        PriorityQueue<PathNode> openSet = new PriorityQueue<>(cmp);
        Map<GridCell, PathNode> allNodes = new LinkedHashMap<>(); // Deterministic iteration

        PathNode startNode = new PathNode(start, 0, heuristic(start, goal), null);
        openSet.add(startNode);
        allNodes.put(start, startNode);

        int nodesExplored = 0;

        while (!openSet.isEmpty() && nodesExplored < MAX_NODES) {
            PathNode current = openSet.poll();
            nodesExplored++;

            if (current.cell().equals(goal)) {
                return new PathResult.Found(reconstructPath(current));
            }

            current.setClosed(true);

            for (int[] dir : DIRECTIONS) {
                int nx = current.cell().x() + dir[0];
                int ny = current.cell().y() + dir[1];
                GridCell neighbor = new GridCell(nx, ny);

                if (!neighbor.isValid() || !isWalkable(neighbor)) continue;

                PathNode neighborNode = allNodes.get(neighbor);
                if (neighborNode != null && neighborNode.isClosed()) continue;

                // Diagonal costs 14, cardinal costs 10
                int moveCost = (dir[0] != 0 && dir[1] != 0) ? D2 : D;
                int tentativeG = current.g() + moveCost;

                if (neighborNode == null) {
                    neighborNode = new PathNode(neighbor, tentativeG, heuristic(neighbor, goal), current);
                    allNodes.put(neighbor, neighborNode);
                    openSet.add(neighborNode);
                } else if (tentativeG < neighborNode.g()) {
                    // Found better path - update node
                    openSet.remove(neighborNode);
                    neighborNode.update(tentativeG, current);
                    openSet.add(neighborNode);
                }
            }
        }

        return new PathResult.NotFound("No path found (explored " + nodesExplored + " nodes)");
    }

    private int heuristic(GridCell from, GridCell to) {
        int dx = Math.abs(from.x() - to.x());
        int dy = Math.abs(from.y() - to.y());
        return D * (dx + dy) + (D2 - 2 * D) * Math.min(dx, dy);
    }

    private boolean isWalkable(GridCell cell) {
        double worldX = (cell.x() * 4.0) - 192 + 2; // Center of cell
        double worldY = (cell.y() * 4.0) - 192 + 2;
        return grid.isOnGround(worldX, worldY);
    }

    private List<GridCell> reconstructPath(PathNode goal) {
        List<GridCell> path = new ArrayList<>();
        PathNode current = goal;
        while (current != null) {
            path.add(current.cell());
            current = current.parent();
        }
        Collections.reverse(path);
        return path;
    }
}
```

### PathNode Internal Class
```java
// Source: Standard A* node structure
package com.mojang.tower.pathfinding;

/**
 * Internal node for A* algorithm.
 * Mutable to allow g-score updates when better paths found.
 */
final class PathNode {
    private final GridCell cell;
    private int g;        // Cost from start
    private final int h;  // Heuristic to goal (constant)
    private PathNode parent;
    private boolean closed;

    PathNode(GridCell cell, int g, int h, PathNode parent) {
        this.cell = cell;
        this.g = g;
        this.h = h;
        this.parent = parent;
        this.closed = false;
    }

    GridCell cell() { return cell; }
    int g() { return g; }
    int f() { return g + h; }
    PathNode parent() { return parent; }
    boolean isClosed() { return closed; }
    void setClosed(boolean closed) { this.closed = closed; }

    void update(int newG, PathNode newParent) {
        this.g = newG;
        this.parent = newParent;
    }
}
```

### ServiceLocator Integration
```java
// Source: Codebase ServiceLocator.java pattern
// Add to ServiceLocator.java:
private static PathfindingService pathfindingService;

public static void provide(PathfindingService service) {
    pathfindingService = service;
}

public static PathfindingService pathfinding() {
    if (pathfindingService == null) {
        throw new IllegalStateException("PathfindingService not initialized");
    }
    return pathfindingService;
}
```

### Peon Integration Sketch
```java
// Source: Codebase Peon.java analysis + research synthesis
// In Peon.tick() - replace random bounce with path following:

// When job has target but no path:
if (job != null && job.hasTarget() && currentPath == null) {
    PathResult result = ServiceLocator.pathfinding().findPath(x, y, job.xTarget, job.yTarget);
    switch (result) {
        case PathResult.Found(var path) -> {
            this.currentPath = path;
            this.pathIndex = 0;
        }
        case PathResult.NotFound(var reason) -> {
            // Fall back to existing random movement
            job.cantReach();
        }
    }
}

// When following path:
if (currentPath != null && pathIndex < currentPath.size()) {
    GridCell waypoint = currentPath.get(pathIndex);
    double wx = gridToWorldX(waypoint);
    double wy = gridToWorldY(waypoint);

    // Check arrival at waypoint
    if (distanceSq(x, y, wx, wy) < 4.0) {
        pathIndex++;
        if (pathIndex >= currentPath.size()) {
            currentPath = null; // Path complete
        }
        return;
    }

    // Move toward waypoint
    rot = Math.atan2(wy - y, wx - x);
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Dijkstra's algorithm | A* with heuristic | 1968 (A* published) | 10-100x fewer nodes explored |
| Float heuristics | Integer heuristics | Best practice | Deterministic behavior |
| HashSet closed list | LinkedHashMap | Determinism requirement | Predictable iteration |
| Full path recompute | Path caching + partial recompute | Standard optimization | Performance |

**Deprecated/outdated:**
- Using `Math.sqrt()` in heuristic: Use integer approximation (14 for sqrt(2)*10)
- Storing paths as LinkedList: ArrayList has better cache locality for iteration
- Breadth-first search for pathfinding: A* is strictly better with good heuristic

## Open Questions

Things that couldn't be fully resolved:

1. **Optimal grid cell size**
   - What we know: World is ~384x384 units (-192 to +192). Entity radius is typically 1-8 units.
   - What's unclear: Whether 96x96 grid (4 units/cell) is right granularity or if 64x64 (6 units/cell) or 48x48 (8 units/cell) would be better.
   - Recommendation: Start with 96x96 (4 units/cell). Tune based on path quality vs performance testing.

2. **Path caching strategy**
   - What we know: Recomputing every tick is too expensive. Paths should be cached.
   - What's unclear: Exact invalidation triggers - how far must target move? How to detect mid-path obstacles?
   - Recommendation: Phase 5 implements basic caching (path until blocked). Phase 7 adds dynamic recalculation.

3. **Diagonal movement through narrow gaps**
   - What we know: A* will try diagonal moves. If gap is narrow, diagonal might clip corners.
   - What's unclear: Whether to forbid diagonal movement when adjacent cardinals are blocked.
   - Recommendation: For Phase 5, allow all diagonals. If corner-clipping is visible problem, add check in Phase 7.

## Sources

### Primary (HIGH confidence)
- [Stanford GameProgramming - Heuristics](http://theory.stanford.edu/~amitp/GameProgramming/Heuristics.html) - Octile distance formula, tie-breaking
- [Stanford GameProgramming - Implementation Notes](http://theory.stanford.edu/~amitp/GameProgramming/ImplementationNotes.html) - A* pseudocode, data structures
- [Stanford GameProgramming - Moving Obstacles](http://theory.stanford.edu/~amitp/GameProgramming/MovingObstacles.html) - Path following, recalculation strategies
- [Java LinkedHashMap docs](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/LinkedHashMap.html) - Insertion-order iteration guarantee
- [Java PriorityQueue docs](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/PriorityQueue.html) - Arbitrary tie-breaking note
- Codebase analysis: NavigationGrid.java, MovementSystem.java, Peon.java, Island.java - Existing patterns

### Secondary (MEDIUM confidence)
- [Baeldung - A* Pathfinding in Java](https://www.baeldung.com/java-a-star-pathfinding) - Java-specific patterns
- [Medium - A* Algorithm in 2D Grid](https://medium.com/akatsuki-taiwan-technology/a-pathfinding-algorithm-in-2d-grid-11f5a5354cc2) - Diagonal cost 10/14 pattern
- [GameDev.net - Combining A* with Steering](https://gamedev.net/forums/topic/616507-combining-a-pathfinding-with-steering-behaviours/4892038/) - Integration patterns

### Tertiary (LOW confidence)
- WebSearch results on "deterministic A* pathfinding" - General concepts, no authoritative source
- Training data on A* algorithm - Treated as hypothesis, verified against Stanford sources

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Java collections are well-documented; A* algorithm is textbook
- Architecture patterns: HIGH - Based on codebase analysis and established patterns
- Pitfalls: HIGH - Common issues verified across multiple sources
- Grid sizing: MEDIUM - Reasonable estimate, may need tuning

**Research date:** 2026-02-05
**Valid until:** 90 days (algorithm fundamentals are stable; only grid sizing may need adjustment)
