---
phase: 02-decoupling-systems
plan: 01
subsystem: events, services
tags: [eventbus, service-locator, sealed-interface, java-21, records, pattern-matching]

# Dependency graph
requires:
  - phase: 01-foundation-language-modernization
    provides: Golden master test, Java 21 features, pattern matching
provides:
  - EventBus with publish/subscribe for decoupled communication
  - ServiceLocator for testable service access
  - SoundEvent sealed hierarchy for type-safe sound events
  - All entity sound calls decoupled via events
affects: [02-02, 03-movement-extraction, testing]

# Tech tracking
tech-stack:
  added: []
  patterns: [EventBus pub/sub, Service Locator, Sealed interfaces for events]

key-files:
  created:
    - src/main/java/com/mojang/tower/event/EventBus.java
    - src/main/java/com/mojang/tower/event/SoundEvent.java
    - src/main/java/com/mojang/tower/event/*.java (11 sound event records)
    - src/main/java/com/mojang/tower/service/ServiceLocator.java
    - src/main/java/com/mojang/tower/service/AudioService.java
    - src/main/java/com/mojang/tower/service/SoundsAdapter.java
    - src/main/java/com/mojang/tower/service/NullAudioService.java
  modified:
    - src/main/java/com/mojang/tower/House.java
    - src/main/java/com/mojang/tower/Peon.java
    - src/main/java/com/mojang/tower/Monster.java
    - src/main/java/com/mojang/tower/Job.java
    - src/main/java/com/mojang/tower/Island.java
    - src/main/java/com/mojang/tower/TowerComponent.java

key-decisions:
  - "Split SoundEvent records into separate public files (Java single public class per file requirement)"
  - "Synchronous EventBus dispatch (critical for game determinism)"
  - "ConcurrentHashMap + CopyOnWriteArrayList for thread-safe listener management"

patterns-established:
  - "EventBus.publish(new XxxSound()) for sound triggers from entities"
  - "ServiceLocator.audio().play() for actual sound playback"
  - "Pattern matching switch on sealed SoundEvent in handler"

# Metrics
duration: 5min
completed: 2026-02-05
---

# Phase 2 Plan 1: EventBus and Sound Decoupling Summary

**Synchronous EventBus with sealed SoundEvent hierarchy, ServiceLocator with AudioService adapter, all 10 Sounds.play() calls replaced with event publishing**

## Performance

- **Duration:** 5 min
- **Started:** 2026-02-05T19:20:25Z
- **Completed:** 2026-02-05T19:24:54Z
- **Tasks:** 3
- **Files created:** 18
- **Files modified:** 6

## Accomplishments
- EventBus infrastructure with synchronous publish/subscribe
- ServiceLocator pattern for testable audio service access
- Sealed SoundEvent interface with 11 public record types
- All entity/job classes now fire events instead of direct Sounds.play() calls
- TowerComponent handles sound events and routes to ServiceLocator.audio()
- Mute button uses ServiceLocator instead of direct Sounds static methods
- Golden master test passes (deterministic behavior preserved)

## Task Commits

Each task was committed atomically:

1. **Task 1: Create EventBus and ServiceLocator infrastructure** - `d1a633a` (feat)
2. **Task 2: Replace all Sounds.play() calls with EventBus.publish()** - `1913b83` (feat)
3. **Task 3: Wire sound event handler and initialize services** - `29460c0` (feat)

## Files Created/Modified

**Created:**
- `src/main/java/com/mojang/tower/event/EventBus.java` - Pub/sub hub with subscribe/publish/reset
- `src/main/java/com/mojang/tower/event/SoundEvent.java` - Sealed interface for sound events
- `src/main/java/com/mojang/tower/event/SelectSound.java` - UI selection sound event
- `src/main/java/com/mojang/tower/event/PlantSound.java` - Building/tree placement sound event
- `src/main/java/com/mojang/tower/event/DestroySound.java` - Building destruction sound event
- `src/main/java/com/mojang/tower/event/GatherSound.java` - Resource deposit sound event
- `src/main/java/com/mojang/tower/event/FinishBuildingSound.java` - Construction complete sound event
- `src/main/java/com/mojang/tower/event/SpawnSound.java` - Peon spawn sound event
- `src/main/java/com/mojang/tower/event/SpawnWarriorSound.java` - Warrior conversion sound event
- `src/main/java/com/mojang/tower/event/DingSound.java` - Level up sound event
- `src/main/java/com/mojang/tower/event/DeathSound.java` - Peon death sound event
- `src/main/java/com/mojang/tower/event/MonsterDeathSound.java` - Monster death sound event
- `src/main/java/com/mojang/tower/event/WinSound.java` - Victory sound event
- `src/main/java/com/mojang/tower/service/ServiceLocator.java` - Service registry
- `src/main/java/com/mojang/tower/service/AudioService.java` - Audio interface
- `src/main/java/com/mojang/tower/service/SoundsAdapter.java` - Wraps Sounds singleton
- `src/main/java/com/mojang/tower/service/NullAudioService.java` - No-op for testing

**Modified:**
- `src/main/java/com/mojang/tower/House.java` - 4 Sounds.play() -> EventBus.publish()
- `src/main/java/com/mojang/tower/Peon.java` - 2 Sounds.play() -> EventBus.publish()
- `src/main/java/com/mojang/tower/Monster.java` - 1 Sounds.play() -> EventBus.publish()
- `src/main/java/com/mojang/tower/Job.java` - 1 Sounds.play() -> EventBus.publish()
- `src/main/java/com/mojang/tower/Island.java` - 1 Sounds.play() -> EventBus.publish()
- `src/main/java/com/mojang/tower/TowerComponent.java` - Handler wiring, ServiceLocator init, mute button

## Decisions Made
- **Split SoundEvent records into separate public files:** Java requires public classes in separate files. Initial approach of package-private records in single file didn't allow cross-package access.
- **Synchronous EventBus dispatch:** Game determinism requires events to be processed immediately within the same tick. Async would break golden master tests.
- **ConcurrentHashMap + CopyOnWriteArrayList:** Thread-safe data structures handle potential concurrent access during game loop without external synchronization.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Split SoundEvent records into separate public files**
- **Found during:** Task 2 (compilation failed)
- **Issue:** Package-private records in single file couldn't be imported from other packages. Java static imports don't work for package-private types.
- **Fix:** Created 11 separate public record files, one per sound event type
- **Files modified:** src/main/java/com/mojang/tower/event/*.java (12 files total)
- **Verification:** Project compiles, all imports resolve correctly
- **Committed in:** 1913b83 (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (blocking)
**Impact on plan:** Necessary Java language constraint. More files but same functionality. No scope creep.

## Issues Encountered
None - after the SoundEvent split, all tasks executed as planned.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- EventBus and ServiceLocator infrastructure complete and tested
- Ready for Plan 02-02: State pattern for game state management
- Entity classes now decoupled from sound system, easier to test and extend
- Pattern established for future event types (EffectEvent for puffs, etc.)

---
*Phase: 02-decoupling-systems*
*Plan: 01*
*Completed: 2026-02-05*
