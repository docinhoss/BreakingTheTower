---
phase: 05-core-astar-integration
verified: 2026-02-05T23:15:00Z
status: passed
score: 11/11 must-haves verified
---

# Phase 5: Core A* and Integration Verification Report

**Phase Goal:** Peons find and follow walkable paths around obstacles
**Verified:** 2026-02-05T23:15:00Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Pathfinder finds route through walkable terrain avoiding obstacles | ✓ VERIFIED | AStarPathfinder.findPath() implements A* with NavigationGrid.isOnGround() checks. Test `findPath_aroundObstacle_findsRoute` passes. |
| 2 | Pathfinder reports when no route exists (blocked terrain) | ✓ VERIFIED | Returns PathResult.NotFound with reason. Tests pass for unreachable targets and unwalkable start/goal. |
| 3 | Diagonal paths are shorter than cardinal-only paths when going corner-to-corner | ✓ VERIFIED | 8-directional DIRECTIONS array with diagonal costs (14 vs 10). Test `findPath_diagonal_shortestPath` verifies 4-cell diagonal path vs 7-cell cardinal. |
| 4 | Same start/goal always produces identical path (deterministic) | ✓ VERIFIED | LinkedHashMap for allNodes (line 65), deterministic Comparator with 4-level tie-breaking (lines 58-62), integer costs (10/14). Test runs same pathfind 100 times, all paths identical. |
| 5 | Peon walks around a rock to reach a target on the other side | ✓ VERIFIED | Peon.tick() calls ServiceLocator.pathfinding().findPath() (line 151), follows waypoints from currentPath (lines 167-182), steers toward waypoint using PathfindingService.gridToWorldX/Y (lines 169-170). |
| 6 | Peon moves diagonally when that is the shortest path | ✓ VERIFIED | AStarPathfinder uses 8-directional movement with diagonal cost 14. Test verifies diagonal path (0,0) to (3,3) = 4 cells not 7. |
| 7 | Running the same scenario twice produces identical peon movement | ✓ VERIFIED | GoldenMasterTest passes (1 test, 0 failures, 3.256s runtime). Deterministic A* ensures same paths every run. |
| 8 | Game maintains 30 tps with 20 peons pathfinding simultaneously | ✓ VERIFIED | Performance test `performance_20PeonsPerTick_completesWithinBudget` passes: 20 long-distance paths complete < 8ms (half of 16.67ms frame budget at 60fps). |
| 9 | Peon falls back to random movement when path cannot be found | ✓ VERIFIED | When PathResult.NotFound, currentPath stays null (line 161), triggers random rotation (line 185). |
| 10 | PathfindingService is accessible via ServiceLocator.pathfinding() | ✓ VERIFIED | ServiceLocator has provide(PathfindingService) and pathfinding() methods (lines 70-85). TowerComponent initializes service at startup (lines 126-127). |
| 11 | Golden master test still passes (determinism preserved) | ✓ VERIFIED | GoldenMasterTest: 1 test, 0 failures. Golden master snapshot regenerated with new A* behavior (documented in 05-02-SUMMARY.md). |

**Score:** 11/11 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/java/com/mojang/tower/pathfinding/GridCell.java` | Immutable grid coordinate type | ✓ VERIFIED | 22 lines, record with GRID_SIZE=96, isValid(), no stubs. Used by AStarPathfinder. |
| `src/main/java/com/mojang/tower/pathfinding/PathNode.java` | A* node with g/h/f scores | ✓ VERIFIED | 47 lines, has g/h/f/parent/closed fields, update() method, no stubs. Used internally by AStarPathfinder. |
| `src/main/java/com/mojang/tower/pathfinding/PathResult.java` | Sealed result type | ✓ VERIFIED | 23 lines, sealed interface with Found/NotFound variants, no stubs. Returned by findPath(). |
| `src/main/java/com/mojang/tower/pathfinding/AStarPathfinder.java` | A* algorithm implementation | ✓ VERIFIED | 157 lines, implements A* with 8-directional movement, Octile heuristic, deterministic comparator, LinkedHashMap, integer costs 10/14. Used by PathfindingService. |
| `src/main/java/com/mojang/tower/pathfinding/PathfindingService.java` | Public API with world coordinate conversion | ✓ VERIFIED | 56 lines, delegates to AStarPathfinder, provides findPath(double x4), gridToWorld/worldToGrid conversions. Registered in ServiceLocator. |
| `src/test/java/com/mojang/tower/pathfinding/AStarPathfinderTest.java` | Unit tests for pathfinding | ✓ VERIFIED | 195 lines, 8 tests covering straight line, obstacles, diagonal, no path, determinism. All pass. |
| `src/test/java/com/mojang/tower/pathfinding/PathfindingServiceTest.java` | Integration tests | ✓ VERIFIED | 159 lines, 6 tests covering coordinate conversion, cross-map paths, performance (20 peons < 8ms). All pass. |
| `src/main/java/com/mojang/tower/Peon.java` | Path following logic | ✓ VERIFIED | 284 lines, has currentPath/pathIndex/pathTargetX/Y fields (lines 26-28), calls pathfinding service (line 151), follows waypoints (lines 167-182), invalidates path on target movement (lines 129-134). |
| `src/main/java/com/mojang/tower/service/ServiceLocator.java` | PathfindingService registration | ✓ VERIFIED | Imports PathfindingService, has private field, provide()/pathfinding()/reset() methods. |
| `src/main/java/com/mojang/tower/TowerComponent.java` | PathfindingService initialization | ✓ VERIFIED | Creates PathfindingService(island) and calls ServiceLocator.provide() at startup (lines 126-127). |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|----|--------|---------|
| PathfindingService | AStarPathfinder | constructor field | ✓ WIRED | Line 12: `private final AStarPathfinder pathfinder;` Line 15: initialized in constructor. Line 27: delegates findPath(). |
| AStarPathfinder | NavigationGrid | constructor injection | ✓ WIRED | Line 28: `private final NavigationGrid grid;` Line 34: constructor parameter. Line 139: calls `grid.isOnGround()`. |
| AStarPathfinder | GridCell | uses for coordinates | ✓ WIRED | Imports GridCell (line 13 in Peon). Creates GridCell instances (line 86), returns List<GridCell> in PathResult.Found. |
| AStarPathfinder | PathResult | return type | ✓ WIRED | Method signature line 44: `public PathResult findPath()`. Returns Found (line 78) or NotFound (lines 47, 50, 114). |
| ServiceLocator | PathfindingService | provide/accessor | ✓ WIRED | Field line 15, provide() line 70, pathfinding() line 80, reset() line 102. Initialized in TowerComponent line 126-127. |
| TowerComponent | ServiceLocator.provide(PathfindingService) | initialization | ✓ WIRED | Lines 126-127: creates PathfindingService(island), calls ServiceLocator.provide(). |
| Peon | ServiceLocator.pathfinding() | requests paths | ✓ WIRED | Line 151: `ServiceLocator.pathfinding().findPath(x, y, job.xTarget, job.yTarget)`. Pattern matches switches on PathResult. |
| Peon | PathfindingService.gridToWorld | waypoint conversion | ✓ WIRED | Lines 169-170: `PathfindingService.gridToWorldX(waypoint)` and `gridToWorldY(waypoint)`. Static methods convert GridCell to world coordinates. |
| Peon | job.xTarget/yTarget | path invalidation | ✓ WIRED | Lines 129-134: checks if target moved >= 4 world units (1 grid cell), nulls currentPath. Lines 156-157: stores pathTargetX/Y when path computed. |
| Peon.setJob() | currentPath clearing | job change | ✓ WIRED | Line 85: `this.currentPath = null;` when job changes. Ensures stale paths cleared. |

### Requirements Coverage

| Requirement | Status | Supporting Truths |
|-------------|--------|-------------------|
| PATH-01: Peon finds walkable route around obstacles | ✓ SATISFIED | Truths 1, 5 |
| PATH-02: 8-directional movement (diagonals) | ✓ SATISFIED | Truths 3, 6 |
| PATH-03: Deterministic data structures | ✓ SATISFIED | Truths 4, 7 |
| PATH-04: Completes within tick budget | ✓ SATISFIED | Truth 8 |
| INT-01: Integrates with MovementSystem | ✓ SATISFIED | Truth 5 (Peon uses MovementRequest after pathfinding) |
| INT-02: Uses NavigationGrid for walkability | ✓ SATISFIED | Truths 1, 2 (AStarPathfinder.isWalkable calls grid.isOnGround) |
| INT-03: Accessible via ServiceLocator | ✓ SATISFIED | Truth 10 |

**Coverage:** 7/7 requirements satisfied

### Anti-Patterns Found

No anti-patterns detected.

**Scan results:**
- TODO/FIXME/XXX/HACK: 0 occurrences
- Placeholder content: 0 occurrences
- Empty returns (return null/{}/(]): 0 occurrences
- Console.log only implementations: N/A (Java System.out not detected)

### Test Results

**Unit Tests (AStarPathfinderTest):**
- Tests run: 8
- Failures: 0
- Errors: 0
- Time: 0.035s
- Coverage: straight line, around obstacle, diagonal, no path, start=goal, start not walkable, determinism (100 iterations), diagonal cost preference

**Integration Tests (PathfindingServiceTest):**
- Tests run: 6
- Failures: 0
- Errors: 0
- Time: 0.037s
- Coverage: world-to-grid origin, bounds clamping, round-trip conversion, world coordinates to grid path, cross-map pathfinding, performance (20 peons < 8ms)

**Golden Master Test:**
- Tests run: 1
- Failures: 0
- Errors: 0
- Time: 3.256s
- Note: Golden master snapshot regenerated with new A* behavior (documented deviation in 05-02-SUMMARY.md)

### Determinism Evidence

**Critical determinism features verified:**

1. **LinkedHashMap for allNodes** (AStarPathfinder.java:65)
   - Ensures consistent iteration order across runs
   - Prevents HashMap insertion-order randomness

2. **Integer costs (10/14)** (AStarPathfinder.java:18-19)
   - Cardinal movement: cost 10
   - Diagonal movement: cost 14 (approximates 10 * sqrt(2))
   - Avoids floating-point comparison issues

3. **Deterministic Comparator** (AStarPathfinder.java:58-62)
   - Primary: lowest f-score (g + h)
   - Secondary: highest g-score (prefer nodes closer to goal)
   - Tertiary: x coordinate
   - Quaternary: y coordinate
   - Four-level tie-breaking ensures consistent path selection

4. **Octile heuristic** (AStarPathfinder.java:121-126)
   - Admissible and consistent for 8-directional movement
   - Integer-only arithmetic

5. **Test verification** (AStarPathfinderTest.java:160-179)
   - Runs same pathfind 100 times
   - Verifies all paths identical
   - Test passes

### Performance Evidence

**Performance test results:**
- Test: `performance_20PeonsPerTick_completesWithinBudget`
- Scenario: 20 peons computing long-distance paths across map
- Target: < 8ms (half of 16.67ms frame budget at 60fps)
- Result: PASSED
- Note: Test includes 5-iteration warm-up to avoid JIT compilation skew

**Actual tick budget:**
- Game target: 30 tps (33.33ms per tick)
- Pathfinding budget: < 8ms for 20 simultaneous paths
- Headroom: ~25ms remaining for other tick work
- Performance constraint: MAX_NODES = 1000 (prevents unbounded search)

---

## Phase Success

**Status:** All must-haves verified. Phase goal achieved.

**Phase Goal:** Peons find and follow walkable paths around obstacles
**Outcome:** ACHIEVED

**Evidence:**
1. Peons request paths from PathfindingService when navigating to job targets
2. Paths computed by A* algorithm with 8-directional movement
3. Peons follow waypoints, converting grid cells to world coordinates
4. Peons walk diagonally when that's the shortest route
5. Path invalidation when target moves (>= 4 world units)
6. Fallback to random movement when no path exists
7. Deterministic behavior (LinkedHashMap, integer costs, 4-level comparator tie-breaking)
8. Golden master test passes (determinism preserved)
9. Performance within budget (20 peons < 8ms)

**Next Phase:** Ready for Phase 6 (Unreachable Handling). PathResult.NotFound already provides reason string for future abandon logic.

---

_Verified: 2026-02-05T23:15:00Z_
_Verifier: Claude (gsd-verifier)_
