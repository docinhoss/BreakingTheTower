---
phase: 03-movement-extraction
plan: 02
subsystem: peon-movement
tags: [movement, peon, service-locator, pattern-matching, refactoring]
dependency-graph:
  requires: [03-01-movement-infrastructure]
  provides: [peon-movementsystem-integration]
  affects: [03-03-monster-movement]
tech-stack:
  added: []
  patterns: [switch-expression-pattern-matching, service-locator-usage]
key-files:
  created: []
  modified:
    - src/main/java/com/mojang/tower/Peon.java
    - src/main/java/com/mojang/tower/TowerComponent.java
    - src/main/java/com/mojang/tower/movement/MovementSystem.java
    - src/test/java/com/mojang/tower/GameRunner.java
decisions:
  - "MovementSystem allows movement when island is null (construction phase)"
  - "Initialize MovementSystem before Island creation to handle entity ticks during construction"
metrics:
  duration: 2 min
  completed: 2026-02-05
---

# Phase 03 Plan 02: Peon Movement Extraction Summary

Peon.tick() now delegates movement execution to MovementSystem, using pattern matching switch expression to handle Moved/Blocked results while preserving exact collision behavior.

## What Was Built

### Peon.java Movement Refactoring
- Replaced inline position update (`x = xt; y = yt`) with `ServiceLocator.movement().move()`
- Added imports: MovementRequest, MovementResult, ServiceLocator
- Implemented switch expression with pattern matching:
  - `case MovementResult.Moved(var newX, var newY)` - position updated by system
  - `case MovementResult.Blocked(var blocker)` - handles collision callbacks

### Collision Handling Preservation
All original collision behavior preserved exactly:
- `job.collide(blocker)` when blocked by entity
- `job.cantReach()` when blocked by terrain/boundary (blocker null)
- Rotation randomization: `random.nextDouble() * Math.PI * 2`
- wanderTime assignment: `random.nextInt(30) + 3`

### MovementSystem Enhancement
- Modified to handle null island gracefully during Island construction
- Returns `Moved` without collision check when island uninitialized
- Necessary because Island.addEntity() calls tick() during construction

### Initialization Order Fix
- TowerComponent: Create MovementSystem before Island
- TowerComponent: Inject Island reference after construction
- GameRunner: Same pattern for test harness
- GameRunner: Reset ServiceLocator at start of test runs

## Commit Log

| Commit | Description |
|--------|-------------|
| cc734be | feat(03-02): refactor Peon.tick() to use MovementSystem |

## Verification Results

All verification criteria met:
- [x] Peon.java imports MovementRequest, MovementResult, ServiceLocator
- [x] Peon.tick() contains `ServiceLocator.movement().move(`
- [x] Peon.tick() contains switch on MovementResult
- [x] No inline `x = xt; y = yt;` position update in Peon.tick()
- [x] `mvn compile` succeeds
- [x] `mvn test` passes (golden master confirms exact behavior preservation)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] MovementSystem initialization order**
- **Found during:** Task 1 verification (tests failing)
- **Issue:** Island construction triggers Peon.tick() via addEntity(), but MovementSystem requires Island reference for collision detection - circular dependency
- **Fix:** Modified MovementSystem.move() to allow movement without collision check when island is null (safe during construction since entities placed at verified-free positions)
- **Files modified:** MovementSystem.java
- **Commit:** cc734be

**2. [Rule 3 - Blocking] TowerComponent/GameRunner initialization order**
- **Found during:** Task 1 verification (tests failing)
- **Issue:** GameRunner didn't initialize MovementSystem, causing IllegalStateException
- **Fix:** Updated both TowerComponent and GameRunner to create MovementSystem before Island, then inject Island reference after
- **Files modified:** TowerComponent.java, GameRunner.java
- **Commit:** cc734be

## Decisions Made

| Decision | Rationale |
|----------|-----------|
| Allow movement when island is null | Safe because entities are only created during Island construction at positions already verified as free |
| Single commit for all related changes | All changes are interdependent and must be atomic to maintain working state |

## Next Phase Readiness

**Ready for Plan 03 (Monster Movement Extraction):**
- MovementSystem patterns established (Peon integration complete)
- Initialization order documented and working
- Pattern matching switch expression template available for Monster.tick()
- Golden master confirms deterministic behavior preservation

**Key Integration Point for Monster:**
```java
// In Monster.tick() - similar pattern
double targetX = x + Math.cos(rot) * speed;
double targetY = y + Math.sin(rot) * speed;
MovementResult result = ServiceLocator.movement().move(
    new MovementRequest(this, targetX, targetY)
);
switch (result) {
    case MovementResult.Moved(var newX, var newY) -> { /* position updated */ }
    case MovementResult.Blocked(var blocker) -> { /* handle collision */ }
}
```
