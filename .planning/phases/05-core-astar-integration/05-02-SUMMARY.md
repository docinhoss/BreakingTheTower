---
phase: 05-core-astar-integration
plan: 02
subsystem: pathfinding
tags: [pathfinding, service-locator, peon, navigation, integration]

requires:
  - phase: 05-01
    provides: AStarPathfinder, GridCell, PathResult, PathNode
  - phase: 04-navigation
    provides: NavigationGrid interface
provides:
  - PathfindingService public facade with world coordinate conversion
  - ServiceLocator.pathfinding() accessor
  - Peon path following with waypoint navigation
affects: [06, 07]

tech-stack:
  added: []
  patterns:
    - World-to-grid coordinate conversion (4 units per cell)
    - Path invalidation on target movement (>= 4 world units)
    - Fallback to random movement when no path available

key-files:
  created:
    - src/main/java/com/mojang/tower/pathfinding/PathfindingService.java
    - src/test/java/com/mojang/tower/pathfinding/PathfindingServiceTest.java
  modified:
    - src/main/java/com/mojang/tower/service/ServiceLocator.java
    - src/main/java/com/mojang/tower/TowerComponent.java
    - src/main/java/com/mojang/tower/Peon.java
    - src/test/java/com/mojang/tower/GameRunner.java
    - src/test/resources/golden/full-game-snapshot.json

key-decisions:
  - "PathfindingService as public facade, AStarPathfinder as internal implementation"
  - "Grid cell center = (x * 4) - 192 + 2 for world coordinates"
  - "Path invalidation threshold of 4 world units (1 grid cell)"
  - "Clear path when job changes to avoid stale routes"
  - "Regenerated golden master to reflect new A* behavior"

patterns-established:
  - "PathfindingService registered in ServiceLocator like MovementSystem"
  - "Peon tracks path target for invalidation on target movement"

duration: 4 min
completed: 2026-02-05
---

# Phase 05 Plan 02: Pathfinding Integration Summary

**PathfindingService facade wired into game: Peons now follow A* computed paths around obstacles**

## Performance

- **Duration:** 4 min
- **Started:** 2026-02-05T22:57:03Z
- **Completed:** 2026-02-05T23:01:32Z
- **Tasks:** 3/3
- **Files modified:** 6 (1 created, 5 modified)

## Accomplishments

- Created PathfindingService public facade with world coordinate conversion
- Registered PathfindingService with ServiceLocator following established pattern
- Modified Peon to request and follow A* paths when navigating to job targets
- Preserved exact wanderTime behavior (guard, decrement, blocked handling)
- Added path invalidation when target moves significantly (>= 4 world units)
- Verified 20 simultaneous pathfinding calls complete under 8ms

## Task Commits

Each task was committed atomically:

1. **Task 1: Create PathfindingService and register with ServiceLocator** - `ce2a6f1` (feat)
2. **Task 2: Modify Peon to follow computed paths** - `8a35276` (feat)
3. **Task 3: Add integration test for PathfindingService** - `263d54a` (test)

## Files Created/Modified

- `src/main/java/com/mojang/tower/pathfinding/PathfindingService.java` - Public facade converting world to grid coords
- `src/main/java/com/mojang/tower/service/ServiceLocator.java` - Added provide/pathfinding methods
- `src/main/java/com/mojang/tower/TowerComponent.java` - Initialize PathfindingService at startup
- `src/main/java/com/mojang/tower/Peon.java` - Path following with currentPath, pathIndex, pathTargetX/Y
- `src/test/java/com/mojang/tower/GameRunner.java` - Provide PathfindingService for test runs
- `src/test/java/com/mojang/tower/pathfinding/PathfindingServiceTest.java` - 6 integration tests
- `src/test/resources/golden/full-game-snapshot.json` - Regenerated with new A* behavior

## Decisions Made

1. **PathfindingService as public facade** - Hides AStarPathfinder implementation, provides clean world coordinate API
2. **World coordinate conversion** - Grid cell center = (x * 4) - 192 + 2 matches existing isWalkable calculation
3. **Path invalidation threshold** - 4 world units (1 grid cell) balances responsiveness with stability
4. **Golden master regeneration** - New A* paths change Peon positions, requiring snapshot update

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Updated GameRunner to provide PathfindingService**
- **Found during:** Task 2 (Peon modification)
- **Issue:** GoldenMasterTest failed with "PathfindingService not initialized" - GameRunner set up MovementSystem but not PathfindingService
- **Fix:** Added PathfindingService initialization to GameRunner.runDeterministicGame()
- **Files modified:** src/test/java/com/mojang/tower/GameRunner.java
- **Verification:** GoldenMasterTest runs to completion
- **Committed in:** 8a35276 (Task 2 commit)

**2. [Expected Change] Regenerated golden master snapshot**
- **Found during:** Task 2 (Peon modification)
- **Issue:** A* pathfinding changes Peon movement patterns, breaking old golden master
- **Fix:** Deleted old snapshot, regenerated with new A* behavior
- **Files modified:** src/test/resources/golden/full-game-snapshot.json
- **Verification:** GoldenMasterTest passes on subsequent runs
- **Committed in:** 8a35276 (Task 2 commit)

---

**Total deviations:** 1 blocking issue auto-fixed, 1 expected golden master update
**Impact on plan:** GameRunner fix was necessary for tests to run. Golden master update expected with pathfinding integration.

## Issues Encountered

None - all issues were handled via deviation rules.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Peons now navigate around obstacles using A* pathfinding
- Phase 5 complete with both plans (05-01 core algorithm, 05-02 integration)
- Ready for Phase 6: Unreachable Handling (graceful abandon when no path exists)
- PathResult.NotFound already returns reason, can be used for immediate abandon logic

---
*Phase: 05-core-astar-integration*
*Completed: 2026-02-05*
