---
phase: 01-foundation-language-modernization
plan: 02
subsystem: testing
tags: [junit5, jackson, golden-master, deterministic-testing, java-records]

# Dependency graph
requires:
  - phase: 01-01
    provides: Maven build infrastructure with JUnit 5 and Jackson dependencies
provides:
  - Golden master test infrastructure for behavioral regression detection
  - Headless game runner for deterministic simulation
  - State capture records (GameState, EntityState, ResourceState)
  - Deterministic seeding mechanism for Entity and Job classes
affects: [01-03-PLAN, refactoring-phases, behavior-verification]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Golden master testing pattern for legacy code refactoring"
    - "Java records for immutable state snapshots"
    - "Headless game execution with AWT stub classes"
    - "Static test seed mechanism for deterministic Random behavior"

key-files:
  created:
    - src/test/java/com/mojang/tower/GameState.java
    - src/test/java/com/mojang/tower/EntityState.java
    - src/test/java/com/mojang/tower/ResourceState.java
    - src/test/java/com/mojang/tower/GameRunner.java
    - src/test/java/com/mojang/tower/GoldenMasterTest.java
    - src/test/java/com/mojang/tower/GameRunnerTest.java
  modified:
    - src/main/java/com/mojang/tower/Entity.java
    - src/main/java/com/mojang/tower/Job.java

key-decisions:
  - "Use Java records for state capture - immutable, auto-equals/hashCode"
  - "HeadlessTowerComponent/Bitmaps stubs for test isolation from AWT"
  - "Static testSeedBase mechanism in Entity/Job for deterministic Random"
  - "5000 max ticks for CI performance (configurable)"
  - "Sort entities by (type, x, y) for stable snapshot ordering"

patterns-established:
  - "Golden master: capture full tick state, compare via JSON equality"
  - "Headless execution: stub AWT components, use dummy island image"
  - "Deterministic seeding: static base + counter for unique but reproducible randoms"

# Metrics
duration: 5min
completed: 2026-02-05
---

# Phase 01 Plan 02: Golden Master Test Infrastructure Summary

**Tick-by-tick state capture with deterministic seeding for behavioral regression detection during refactoring**

## Performance

- **Duration:** 5 min
- **Started:** 2026-02-05T18:41:31Z
- **Completed:** 2026-02-05T18:46:00Z
- **Tasks:** 2
- **Files modified:** 8

## Accomplishments
- GameState, EntityState, ResourceState Java records for tick-by-tick state capture
- Headless GameRunner that simulates game without AWT rendering
- Deterministic seeding mechanism added to Entity and Job classes
- GoldenMasterTest with fullGameplayMatchesGoldenMaster test method
- GameRunnerTest sanity tests verifying runner execution and determinism

## Task Commits

Each task was committed atomically:

1. **Task 1: Create state capture records and headless game runner** - `d08cc8a` (feat)
2. **Task 1b: Add deterministic seeding for Entity and Job** - `3aa1df5` (feat)
3. **Task 2: Create golden master test class** - `bef8896` (test)

## Files Created/Modified

### Created
- `src/test/java/com/mojang/tower/GameState.java` - Per-tick game state record
- `src/test/java/com/mojang/tower/EntityState.java` - Entity state record with Comparable for sorting
- `src/test/java/com/mojang/tower/ResourceState.java` - Resource state record (wood, rock, food)
- `src/test/java/com/mojang/tower/GameRunner.java` - Headless game runner with state capture
- `src/test/java/com/mojang/tower/GoldenMasterTest.java` - Main golden master test class
- `src/test/java/com/mojang/tower/GameRunnerTest.java` - Sanity tests for runner

### Modified
- `src/main/java/com/mojang/tower/Entity.java` - Added testSeedBase/setTestSeed for deterministic Random
- `src/main/java/com/mojang/tower/Job.java` - Added testSeedBase/setTestSeed, fixed Math.random() to use instance random

## Decisions Made
- **Java records for state capture:** Immutable, auto-generated equals/hashCode, concise syntax
- **HeadlessTowerComponent extends TowerComponent:** Minimal implementation satisfying Island constructor
- **HeadlessBitmaps extends Bitmaps:** Null-safe stubs preventing NPE during headless execution
- **Static testSeedBase mechanism:** Entity.setTestSeed() and Job.setTestSeed() enable deterministic behavior
- **Sorted entity list:** EntityState implements Comparable, sorted by (type, x, y) for stable JSON output
- **5000 MAX_TICKS default:** Balance between coverage and CI performance (configurable via parameter)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Fixed Math.random() in Job.cantReach()**
- **Found during:** Task 1 (deterministic seeding implementation)
- **Issue:** Job.cantReach() used Math.random() instead of instance random, breaking determinism
- **Fix:** Changed to use `random.nextDouble()` like other methods
- **Files modified:** src/main/java/com/mojang/tower/Job.java
- **Verification:** GameRunnerTest.runnerProducesDeterministicResults passes
- **Committed in:** 3aa1df5

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Essential fix for deterministic behavior. No scope creep.

## Issues Encountered
None

## Snapshot Generation Pending

**IMPORTANT:** The golden master snapshot has NOT been generated yet.

To generate the initial snapshot, run:
```bash
mvn test -Dtest=GoldenMasterTest
```

The test will:
1. Run deterministic game simulation (up to 5000 ticks)
2. Generate `src/test/resources/golden/full-game-snapshot.json`
3. Fail with message "Golden master snapshot created"
4. Re-run to verify snapshot matches

The snapshot should be committed to git as the approved baseline.

## Next Phase Readiness
- Golden master test infrastructure complete
- Ready for Plan 03: Annotation-based Java modernization
- Any behavioral changes during refactoring will cause golden master test to fail
- Run `mvn test -Dtest=GoldenMasterTest` to verify behavior preservation after changes

---
*Phase: 01-foundation-language-modernization*
*Completed: 2026-02-05*
