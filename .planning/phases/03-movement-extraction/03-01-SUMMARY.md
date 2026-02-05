---
phase: 03-movement-extraction
plan: 01
subsystem: movement-infrastructure
tags: [movement, service-locator, records, sealed-interface]
dependency-graph:
  requires: [02-decoupling-systems]
  provides: [movement-service, movement-request, movement-result]
  affects: [03-02-peon-movement, 03-03-monster-movement]
tech-stack:
  added: []
  patterns: [sealed-interface, service-locator, record-factory]
key-files:
  created:
    - src/main/java/com/mojang/tower/movement/MovementRequest.java
    - src/main/java/com/mojang/tower/movement/MovementResult.java
    - src/main/java/com/mojang/tower/movement/MovementSystem.java
  modified:
    - src/main/java/com/mojang/tower/service/ServiceLocator.java
    - src/main/java/com/mojang/tower/TowerComponent.java
decisions:
  - "MovementSystem takes Island via setter (injected after Island construction)"
  - "MovementResult.Blocked.blocker can be null (terrain/boundary blocking)"
  - "MovementSystem validates island initialization with IllegalStateException"
metrics:
  duration: 3 min
  completed: 2026-02-05
---

# Phase 03 Plan 01: MovementSystem Infrastructure Summary

MovementSystem service with MovementRequest/MovementResult records, registered via ServiceLocator following Phase 2 patterns.

## What Was Built

### MovementRequest Record
- Immutable record capturing entity, targetX, targetY
- Factory method `fromDirection(entity, direction, speed)` for convenience
- Follows existing record patterns (Vec, Cost from Phase 1)

### MovementResult Sealed Interface
- `Moved(x, y)` - successful position update
- `Blocked(blocker)` - collision detected, blocker reference (null if terrain)
- Enables exhaustive pattern matching in switch expressions

### MovementSystem Service
- Central service for entity movement execution
- `move(MovementRequest)` handles collision detection via `Island.isFree()`
- On success: updates entity position, returns `Moved`
- On collision: returns `Blocked` with blocker entity

### ServiceLocator Integration
- Added `MovementSystem` field to ServiceLocator
- `provide(MovementSystem)` for registration
- `movement()` accessor with null check
- `reset()` updated to clear movement service

### TowerComponent Initialization
- MovementSystem created and initialized in `init()`
- Island reference injected via `setIsland()`
- Registered with ServiceLocator after Island creation

## Commit Log

| Commit | Description |
|--------|-------------|
| fd36063 | feat(03-01): create MovementRequest record |
| 95d9406 | feat(03-01): create MovementResult sealed interface |
| db4dbc4 | feat(03-01): create MovementSystem service with ServiceLocator integration |

## Verification Results

All verification criteria met:
- [x] All three files exist in movement package
- [x] ServiceLocator has `movement()` accessor
- [x] TowerComponent.init() registers MovementSystem
- [x] `mvn compile` succeeds
- [x] `mvn test` passes (golden master unchanged)

## Deviations from Plan

None - plan executed exactly as written.

## Decisions Made

| Decision | Rationale |
|----------|-----------|
| Island via setter injection | MovementSystem created after Island, so setter pattern allows proper initialization order |
| Blocker can be null | Island.getEntityAt() may return null for terrain/boundary collisions |
| IllegalStateException on uninitialized | Fail-fast if move() called before island set |

## Next Phase Readiness

**Ready for Plan 02 (Peon Movement Extraction):**
- MovementSystem accessible via `ServiceLocator.movement()`
- MovementRequest.fromDirection() simplifies Peon integration
- MovementResult enables clean handling of blocked movement

**Key Integration Point:**
```java
// In Peon.tick() - future refactoring
var request = MovementRequest.fromDirection(this, moveDir, moveSpeed);
var result = ServiceLocator.movement().move(request);
// Handle Moved vs Blocked
```
