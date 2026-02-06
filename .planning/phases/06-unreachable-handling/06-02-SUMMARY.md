---
phase: 06-unreachable-handling
plan: 02
subsystem: movement
tags: [pathfinding, blacklist, abandonment, determinism, a-star]

# Dependency graph
requires:
  - phase: 06-01
    provides: "AbandonedTargetSound event, configurable node limit, Job.getTarget() accessor"
  - phase: 05
    provides: "A* pathfinding with PathResult sealed interface"
provides:
  - "Peon blacklist infrastructure with 60-tick expiry"
  - "isTrapped() detection for surrounded peons"
  - "Deterministic job abandonment on PathResult.NotFound"
  - "AbandonedTargetSound event publishing on abandonment"
  - "Deterministic Job.cantReach() (no random 10%)"
affects: ["07-final-validation", "future pathfinding changes"]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "LinkedHashMap for deterministic blacklist iteration"
    - "Tick-based expiry for time-limited blacklisting"
    - "Immediate abandonment on pathfinding failure"

key-files:
  created:
    - "src/test/java/com/mojang/tower/pathfinding/UnreachableHandlingTest.java"
  modified:
    - "src/main/java/com/mojang/tower/Peon.java"
    - "src/main/java/com/mojang/tower/Job.java"
    - "src/test/resources/golden/full-game-snapshot.json"

key-decisions:
  - "LinkedHashMap for blacklist ensures deterministic iteration order during cleanup"
  - "Integer tick expiry (not long) - game won't run billions of ticks"
  - "isTrapped() checks all 8 neighbors via isOnGround(), not entity collision"
  - "Trapped peon dies immediately and respawns via normal House spawning"
  - "Job.cantReach() now deterministic - just clears target, no random 10%"

patterns-established:
  - "Blacklist pattern: LinkedHashMap<Entity, Integer> with tick-based expiry"
  - "Trapped detection: check all 8 grid neighbors for walkability"
  - "Abandonment flow: NotFound -> isTrapped() -> blacklist -> event -> setJob(null)"

# Metrics
duration: 4min
completed: 2026-02-06
---

# Phase 6 Plan 2: Unreachable Handling Implementation Summary

**Deterministic peon abandonment on NotFound, trapped detection with immediate death, and 60-tick blacklist for failed targets**

## Performance

- **Duration:** 4 min
- **Started:** 2026-02-06T10:49:03Z
- **Completed:** 2026-02-06T10:53:12Z
- **Tasks:** 3
- **Files modified:** 4

## Accomplishments
- Peon now immediately abandons job when PathResult.NotFound is returned (replacing random 10% abandonment)
- Trapped peons (all 8 neighbors blocked) die immediately with normal respawn via House
- Failed targets are blacklisted for 60 ticks to prevent re-assignment thrashing
- AbandonedTargetSound event published on abandonment for audio feedback
- Golden master updated to reflect new deterministic behavior

## Task Commits

Each task was committed atomically:

1. **Task 1: Add blacklist and tick counter to Peon** - `529d66d` (feat)
2. **Task 2: Add trapped detection and abandonment logic to Peon** - `7b76b8d` (feat)
3. **Task 3: Add tests and update golden master** - `2bfda36` (test)

## Files Created/Modified
- `src/main/java/com/mojang/tower/Peon.java` - Added targetBlacklist, tickCounter, cleanBlacklist(), blacklistTarget(), isBlacklisted(), isTrapped(), NotFound handling
- `src/main/java/com/mojang/tower/Job.java` - Made cantReach() deterministic (removes random 10%)
- `src/test/java/com/mojang/tower/pathfinding/UnreachableHandlingTest.java` - 6 tests for REACH-01/02/03 requirements
- `src/test/resources/golden/full-game-snapshot.json` - Updated for deterministic behavior changes

## Decisions Made
- LinkedHashMap for blacklist ensures deterministic iteration order during cleanup (per research pitfall)
- Integer tick expiry (not long) for simplicity - game won't run billions of ticks
- isTrapped() checks 8 neighbors via isOnGround() to match A* walkability
- cantReach() is for collision handling, NotFound triggers abandonment via pathfinding

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- Golden master test failed after behavior changes (expected) - regenerated snapshot
- EventBus subscribes by exact class type, not interface - fixed test to use AbandonedTargetSound.class

## Next Phase Readiness
- Phase 6 complete: unreachable handling fully implemented
- REACH-01: Peon abandons immediately on NotFound (not random 10%)
- REACH-02: Blacklist prevents thrashing (60-tick duration)
- REACH-03: Node limit configurable (default 1024, tested in 06-01)
- Ready for Phase 7 final validation

---
*Phase: 06-unreachable-handling*
*Completed: 2026-02-06*
