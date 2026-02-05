---
phase: 05-core-astar-integration
plan: 01
subsystem: pathfinding
tags: [a-star, pathfinding, algorithm, navigation, java-records, sealed-interface]

requires:
  - phase: 04-navigation
    provides: NavigationGrid interface for walkability queries
provides:
  - GridCell immutable coordinate type for grid-based pathfinding
  - PathNode A* node with g/h/f scores for algorithm state
  - PathResult sealed type (Found/NotFound) for safe result handling
  - AStarPathfinder deterministic 8-directional pathfinding
affects: [05-02, 06, 07]

tech-stack:
  added: []
  patterns:
    - Java record for immutable value type (GridCell)
    - Sealed interface for exhaustive result handling (PathResult)
    - LinkedHashMap for deterministic iteration (golden master safe)
    - Integer cost heuristic (10/14) avoiding floating-point

key-files:
  created:
    - src/main/java/com/mojang/tower/pathfinding/GridCell.java
    - src/main/java/com/mojang/tower/pathfinding/PathNode.java
    - src/main/java/com/mojang/tower/pathfinding/PathResult.java
    - src/main/java/com/mojang/tower/pathfinding/AStarPathfinder.java
    - src/test/java/com/mojang/tower/pathfinding/AStarPathfinderTest.java
  modified: []

key-decisions:
  - "Integer costs (10 cardinal, 14 diagonal) for determinism"
  - "LinkedHashMap for closed set ensures consistent iteration order"
  - "Octile heuristic is admissible for 8-directional movement"
  - "MAX_NODES limit (1000) prevents unbounded search"

patterns-established:
  - "Pathfinding types in com.mojang.tower.pathfinding package"
  - "PathResult sealed interface pattern matches MovementResult"
  - "Grid coordinates are discrete (GridCell), world coordinates are continuous (double)"

duration: 2 min
completed: 2026-02-05
---

# Phase 05 Plan 01: Core A* Pathfinding Summary

**Deterministic A* pathfinding with 8-directional movement, integer costs, and sealed result types**

## Performance

- **Duration:** 2 min
- **Started:** 2026-02-05T22:51:34Z
- **Completed:** 2026-02-05T22:53:44Z
- **Tasks:** 3/3
- **Files modified:** 5 (created)

## Accomplishments

- Created immutable GridCell record for 96x96 grid (4 world units per cell)
- Implemented A* algorithm with deterministic tie-breaking comparator
- Established PathResult sealed interface matching codebase patterns
- Verified determinism with 100-iteration test proving identical paths
- All 8 unit tests pass confirming optimal path finding

## Task Commits

Each task was committed atomically:

1. **Task 1: Create pathfinding value types** - `f951ce4` (feat)
2. **Task 2: Implement A* algorithm with deterministic tie-breaking** - `6c4e78a` (feat)
3. **Task 3: Add unit tests for A* pathfinding** - `8395a0f` (test)

## Files Created/Modified

- `src/main/java/com/mojang/tower/pathfinding/GridCell.java` - Immutable record for grid coordinates with GRID_SIZE constant
- `src/main/java/com/mojang/tower/pathfinding/PathNode.java` - Mutable A* node with g, h, f scores and parent reference
- `src/main/java/com/mojang/tower/pathfinding/PathResult.java` - Sealed interface with Found(path) and NotFound(reason) variants
- `src/main/java/com/mojang/tower/pathfinding/AStarPathfinder.java` - A* implementation with 8-dir movement, Octile heuristic, LinkedHashMap
- `src/test/java/com/mojang/tower/pathfinding/AStarPathfinderTest.java` - 8 unit tests covering paths, obstacles, determinism

## Decisions Made

1. **Integer costs (10/14)** - Avoids floating-point comparison issues that break determinism
2. **LinkedHashMap for allNodes** - Guarantees insertion-order iteration, critical for golden master tests
3. **Octile heuristic** - Admissible and consistent for 8-directional grid movement
4. **MAX_NODES = 1000** - Prevents unbounded exploration while allowing reasonable search depth

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- AStarPathfinder is ready for integration with Peon path following
- Next plan (05-02) will create PathfindingService facade and integrate with MovementSystem
- NavigationGrid injection already working, tested with mock implementation

---
*Phase: 05-core-astar-integration*
*Completed: 2026-02-05*
