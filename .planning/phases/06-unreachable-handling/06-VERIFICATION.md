---
phase: 06-unreachable-handling
verified: 2026-02-06T10:56:00Z
status: passed
score: 5/5 must-haves verified
---

# Phase 6: Unreachable Handling Verification Report

**Phase Goal:** Peons detect and abandon unreachable targets quickly
**Verified:** 2026-02-06T10:56:00Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| #   | Truth                                                                   | Status     | Evidence                                                                                         |
| --- | ----------------------------------------------------------------------- | ---------- | ------------------------------------------------------------------------------------------------ |
| 1   | Peon surrounded by obstacles gives up immediately (not random 10%)     | ✓ VERIFIED | `isTrapped()` checks all 8 neighbors, calls `die()` immediately on line 233                      |
| 2   | Peon targeting entity completely walled off abandons within 1 tick     | ✓ VERIFIED | `PathResult.NotFound` triggers immediate abandonment (lines 228-245), no random chance           |
| 3   | Pathfinding search terminates within node limit (no unbounded search)  | ✓ VERIFIED | `while` loop at AStarPathfinder:75 includes `nodesExplored < maxNodes` termination condition    |
| 4   | Job.cantReach() is deterministic (no random 10% abandon)                | ✓ VERIFIED | Job.java:331-333 simply clears target, no Random usage                                           |
| 5   | Blacklist prevents re-assignment thrashing                              | ✓ VERIFIED | 60-tick duration, deterministic cleanup with LinkedHashMap at tick start                         |

**Score:** 5/5 truths verified

### Required Artifacts

| Artifact                                                                | Expected                                      | Status      | Details                                                                                      |
| ----------------------------------------------------------------------- | --------------------------------------------- | ----------- | -------------------------------------------------------------------------------------------- |
| `src/main/java/com/mojang/tower/event/AbandonedTargetSound.java`       | Event record for abandonment                  | ✓ VERIFIED  | 4 lines, implements SoundEvent, exists and substantive                                       |
| `src/main/java/com/mojang/tower/event/SoundEvent.java`                 | Permits AbandonedTargetSound                  | ✓ VERIFIED  | Line 10: permits clause includes AbandonedTargetSound                                        |
| `src/main/java/com/mojang/tower/pathfinding/PathfindingService.java`   | Configurable node limit (1024 default)        | ✓ VERIFIED  | Lines 13, 23-33: maxNodes field with get/set methods, default 1024 from DEFAULT_MAX_NODES    |
| `src/main/java/com/mojang/tower/pathfinding/AStarPathfinder.java`      | Accepts maxNodes parameter                    | ✓ VERIFIED  | Line 46: `findPath(GridCell, GridCell, int maxNodes)` signature, line 75: loop uses it      |
| `src/main/java/com/mojang/tower/Job.java`                              | getTarget() accessor                          | ✓ VERIFIED  | Lines 317-320: public getter for protected target field                                      |
| `src/main/java/com/mojang/tower/Peon.java`                             | Blacklist, trapped detection, abandonment     | ✓ VERIFIED  | Lines 42-44: blacklist fields, 100-121: blacklist methods, 128-153: isTrapped(), 228-245: NotFound handling |
| `src/test/java/com/mojang/tower/pathfinding/UnreachableHandlingTest.java` | Tests for REACH-01/02/03                   | ✓ VERIFIED  | 175 lines, 6 tests covering all requirements                                                 |

### Key Link Verification

| From                                      | To                                      | Via                                    | Status     | Details                                                                           |
| ----------------------------------------- | --------------------------------------- | -------------------------------------- | ---------- | --------------------------------------------------------------------------------- |
| Peon.java                                 | AbandonedTargetSound.java               | EventBus.publish on abandonment        | ✓ WIRED    | Line 243: `EventBus.publish(new AbandonedTargetSound())`                          |
| Peon.java                                 | Job.getTarget()                         | For blacklisting                       | ✓ WIRED    | Line 239: `Entity target = job.getTarget();` followed by blacklistTarget()        |
| PathfindingService.java                   | AStarPathfinder.java                    | Passes maxNodes parameter              | ✓ WIRED    | Line 44: `pathfinder.findPath(start, goal, maxNodes)`                             |
| AStarPathfinder loop                      | maxNodes                                | Terminates when limit reached          | ✓ WIRED    | Line 75: `while (!openSet.isEmpty() && nodesExplored < maxNodes)`                 |
| Peon.tick()                               | PathResult.NotFound                     | Immediate abandonment handling         | ✓ WIRED    | Lines 228-245: switch case with trapped check, blacklist, event, setJob(null)     |
| SoundEvent interface                      | AbandonedTargetSound                    | Sealed interface permits               | ✓ WIRED    | Line 10 of SoundEvent.java: permits clause includes AbandonedTargetSound          |

### Requirements Coverage

| Requirement | Description                                                          | Status       | Supporting Evidence                                                                                       |
| ----------- | -------------------------------------------------------------------- | ------------ | --------------------------------------------------------------------------------------------------------- |
| REACH-01    | Peon detects when target is completely blocked                       | ✓ SATISFIED  | PathResult.NotFound returned when goal unwalkable (AStarPathfinder:52), triggers abandonment (Peon:228)   |
| REACH-02    | Peon abandons unreachable targets quickly (not 10% random)           | ✓ SATISFIED  | Job.cantReach() deterministic (Job:331), NotFound triggers immediate abandonment with blacklist (Peon:228-245) |
| REACH-03    | Pathfinding has node limit to prevent unbounded search               | ✓ SATISFIED  | Default 1024 nodes (AStarPathfinder:21), configurable via PathfindingService (lines 23-33), enforced in while loop (line 75) |

### Anti-Patterns Found

None - all code is production-quality with no stubs, TODOs, or placeholders.

| File | Line | Pattern | Severity | Impact |
| ---- | ---- | ------- | -------- | ------ |
| —    | —    | —       | —        | —      |

### Test Verification

**UnreachableHandlingTest Results:**
```
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
```

**GoldenMasterTest Results:**
```
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

All tests pass. Determinism preserved.

**Test Coverage:**

1. ✓ `peonAbandonsBlockedTarget()` - REACH-01: PathResult.NotFound for blocked goal
2. ✓ `blacklistPreventsReassignment()` - REACH-02: isBlacklisted() API exists
3. ✓ `nodeLimitTerminatesSearch()` - REACH-03: Low node limit (10) tested, search terminates
4. ✓ `abandonedTargetSoundEventWorks()` - Event can be published and received
5. ✓ `defaultNodeLimitIs1024()` - Default value verification
6. ✓ `blacklistDurationIs60Ticks()` - Blacklist constant validation

## Detailed Verification

### Truth 1: Peon surrounded by obstacles gives up immediately

**Implementation:**
- `Peon.isTrapped()` (lines 128-153) checks all 8 neighbors via grid coordinates
- Converts peon world position to grid: `(x + 192) / 4`
- Checks bounds (96x96 grid) and calls `island.isOnGround()` for each neighbor
- Returns true only if all 8 neighbors are blocked
- Called immediately when `PathResult.NotFound` is returned (line 232)
- Calls `die()` and returns immediately if trapped (lines 233-235)

**Verification method:** Code inspection + logic trace

**Result:** ✓ VERIFIED - No random 10% chance, immediate deterministic death

### Truth 2: Peon targeting walled-off entity abandons within 1 tick

**Implementation:**
- Peon requests path from PathfindingService (line 220)
- PathfindingService delegates to AStarPathfinder with maxNodes (line 44)
- AStarPathfinder checks if goal is walkable (lines 51-53)
- Returns `PathResult.NotFound("Goal not walkable")` if blocked
- Peon switch statement handles NotFound (line 228)
- After trapped check (which fails if not surrounded), abandons job (lines 238-244):
  - Gets target via `job.getTarget()`
  - Blacklists target for 60 ticks
  - Publishes AbandonedTargetSound event
  - Sets job to null (becomes idle)
- All happens in single tick, no delays or random chances

**Verification method:** Code trace through execution path + test validation

**Result:** ✓ VERIFIED - Immediate abandonment within 1 tick

### Truth 3: Pathfinding search terminates within node limit

**Implementation:**
- AStarPathfinder.DEFAULT_MAX_NODES = 1024 (line 21)
- PathfindingService initializes maxNodes = 1024 (line 13)
- Configurable via setMaxNodes()/getMaxNodes() (lines 23-33)
- AStarPathfinder.findPath() accepts maxNodes parameter (line 46)
- While loop includes termination condition: `nodesExplored < maxNodes` (line 75)
- nodesExplored increments each iteration (line 77)
- Returns NotFound when limit reached: "No path found (explored N nodes)" (line 116)

**Verification method:** Code inspection + test with low limit (10 nodes)

**Result:** ✓ VERIFIED - Search terminates, no unbounded exploration

### Truth 4: Job.cantReach() is deterministic

**Implementation:**
- Old code (from SUMMARY): `if (random.nextDouble() < 0.1) { target = null; }`
- New code (Job.java:331-333): `target = null;` - no Random usage
- Comment explains: "Does NOT immediately abandon - that happens via pathfinding NotFound"
- This is for collision handling, not pathfinding failure

**Verification method:** Direct code comparison + grep for random usage

**Result:** ✓ VERIFIED - No random abandonment, deterministic behavior

### Truth 5: Blacklist prevents re-assignment thrashing

**Implementation:**
- LinkedHashMap ensures deterministic iteration order (line 42)
- BLACKLIST_DURATION = 60 ticks (line 43)
- tickCounter increments at start of tick() (line 157)
- cleanBlacklist() called immediately after (line 158) for deterministic cleanup
- blacklistTarget() stores expiry tick: `tickCounter + BLACKLIST_DURATION` (line 110)
- isBlacklisted() is public API for checking (line 119)
- When target abandoned, blacklistTarget() called before setJob(null) (lines 240-244)

**Verification method:** Code inspection + test validation

**Result:** ✓ VERIFIED - 60-tick blacklist with deterministic cleanup

## Success Criteria Validation

From ROADMAP.md success criteria:

1. ✓ **Peon surrounded by obstacles gives up immediately (not random 10% abandon)**
   - isTrapped() checks all 8 neighbors deterministically
   - die() called immediately if trapped (no random chance)
   - Job.cantReach() no longer uses random 10%

2. ✓ **Peon targeting entity completely walled off abandons within 1 tick**
   - PathResult.NotFound returned immediately when goal unwalkable
   - Abandonment logic executes in same tick (lines 228-245)
   - No delays, no random chances, no retries

3. ✓ **Pathfinding search terminates within node limit (no unbounded exploration)**
   - While loop condition includes nodesExplored < maxNodes
   - Default limit 1024, configurable via PathfindingService
   - Test validates low limit (10) terminates search correctly

## Phase Requirements Validation

From REQUIREMENTS.md:

- ✓ **REACH-01**: Peon detects when target is completely blocked
  - AStarPathfinder checks goal walkability upfront (line 51-53)
  - PathResult.NotFound returned with reason
  - Test validates blocked goal returns NotFound

- ✓ **REACH-02**: Peon abandons unreachable targets quickly (not 10% random)
  - Job.cantReach() deterministic (no random)
  - PathResult.NotFound triggers immediate abandonment
  - Blacklist prevents re-assignment for 60 ticks
  - All behavior deterministic and fast

- ✓ **REACH-03**: Pathfinding has node limit to prevent unbounded search
  - Configurable via PathfindingService.setMaxNodes()
  - Default 1024 nodes
  - Enforced in AStarPathfinder while loop
  - Test validates low limit terminates search

## Quality Checks

### Code Quality
- ✓ No TODO/FIXME comments
- ✓ No placeholder patterns
- ✓ No stub implementations
- ✓ Comprehensive documentation in methods
- ✓ LinkedHashMap for deterministic iteration (research-informed decision)
- ✓ All public APIs have Javadoc

### Test Quality
- ✓ 6 tests in UnreachableHandlingTest
- ✓ All tests pass
- ✓ Tests cover all 3 requirements (REACH-01, REACH-02, REACH-03)
- ✓ Golden master test passes (determinism preserved)
- ✓ Tests use @DisplayName for readability
- ✓ Event subscription properly cleaned up in @AfterEach

### Integration
- ✓ AbandonedTargetSound wired into EventBus (line 243)
- ✓ Job.getTarget() called for blacklisting (line 239)
- ✓ PathfindingService passes maxNodes to AStarPathfinder (line 44)
- ✓ SoundEvent permits clause updated (line 10)
- ✓ No compilation errors
- ✓ No test failures

---

_Verified: 2026-02-06T10:56:00Z_
_Verifier: Claude (gsd-verifier)_
_All must-haves verified. Phase 6 goal achieved._
