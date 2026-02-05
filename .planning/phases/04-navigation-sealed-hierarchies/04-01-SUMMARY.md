---
phase: 04
plan: 01
subsystem: navigation
tags: [interface, abstraction, dependency-inversion]
dependency-graph:
  requires: [03-movement-extraction]
  provides: [NavigationGrid interface, interface-based movement]
  affects: [04-02-pathfinding]
tech-stack:
  added: []
  patterns: [Dependency Inversion, Interface Segregation]
key-files:
  created:
    - src/main/java/com/mojang/tower/navigation/NavigationGrid.java
  modified:
    - src/main/java/com/mojang/tower/Island.java
    - src/main/java/com/mojang/tower/movement/MovementSystem.java
    - src/main/java/com/mojang/tower/TowerComponent.java
    - src/test/java/com/mojang/tower/GameRunner.java
decisions:
  - id: 04-01-001
    decision: "NavigationGrid interface with 3 methods matching Island's API"
    rationale: "Focused interface for movement/pathfinding queries only"
metrics:
  duration: 3 min
  completed: 2026-02-05
---

# Phase 4 Plan 1: NavigationGrid Interface Summary

**One-liner:** NavigationGrid interface abstracts world queries; Island implements it, MovementSystem depends on interface not concrete class.

## What Was Done

1. **Created NavigationGrid interface** - New `com.mojang.tower.navigation` package with interface defining 3 methods:
   - `isOnGround(double x, double y)` - terrain walkability check
   - `isFree(double x, double y, double radius, Entity exclude)` - collision check
   - `getEntityAt(double x, double y, double radius, TargetFilter filter, Entity exclude)` - entity lookup

2. **Island implements NavigationGrid** - Added `implements NavigationGrid` clause. No method changes needed since existing methods already matched interface signatures.

3. **MovementSystem depends on interface** - Changed dependency from concrete `Island` to `NavigationGrid` interface:
   - Field: `private Island island` -> `private NavigationGrid grid`
   - Method: `setIsland(Island)` -> `setNavigationGrid(NavigationGrid)`

4. **Updated wiring** - TowerComponent and GameRunner now call `setNavigationGrid(island)`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Updated GameRunner.java test infrastructure**
- **Found during:** Task 2 verification
- **Issue:** GameRunner.java also called `setIsland()`, causing test failure
- **Fix:** Updated call to `setNavigationGrid()` to match new API
- **Files modified:** src/test/java/com/mojang/tower/GameRunner.java
- **Commit:** b678b34

## Technical Details

### Interface Design

The NavigationGrid interface exposes only what MovementSystem needs for collision detection:

```java
public interface NavigationGrid {
    boolean isOnGround(double x, double y);
    boolean isFree(double x, double y, double radius, Entity exclude);
    Entity getEntityAt(double x, double y, double radius, TargetFilter filter, Entity exclude);
}
```

Convenience overloads (like `isFree(x, y, r)` without exclude parameter) remain in Island - they're implementation details, not part of the abstraction.

### Dependency Inversion Benefits

- **MovementSystem** now depends on abstraction, not concrete Island
- **Future pathfinding** can depend on NavigationGrid without knowing about Island
- **Testing** could inject mock NavigationGrid for unit tests
- **Clean separation** between "what queries I need" vs "how Island implements them"

## Verification Results

| Check | Result |
|-------|--------|
| mvn compile -q | Pass |
| mvn test -q | Pass |
| NavigationGrid has 3 methods | Pass |
| Island implements NavigationGrid | Pass |
| MovementSystem uses NavigationGrid field | Pass |
| TowerComponent calls setNavigationGrid | Pass |

## Commits

| Hash | Type | Description |
|------|------|-------------|
| 7c9f333 | feat | create NavigationGrid interface |
| b678b34 | feat | Island implements NavigationGrid, MovementSystem uses interface |

## Next Phase Readiness

**Ready for 04-02:** NavigationGrid interface is in place. Pathfinding system can depend on this interface rather than concrete Island, enabling clean architecture for path calculation.

**Concerns:** None. Clean extraction with no behavior changes.
