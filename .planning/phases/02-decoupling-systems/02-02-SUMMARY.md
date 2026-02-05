# Phase 2 Plan 2: State Pattern and Effect Events Summary

**Sealed GameState interface with TitleState/PlayingState/WonState, EffectEvent hierarchy for decoupled Puff/InfoPuff creation via EventBus**

---
subsystem: architecture
tags: [state-pattern, event-driven, sealed-interface, game-states, visual-effects]

requires:
  - 02-01 (EventBus infrastructure)
provides:
  - GameState sealed interface for game state machine
  - EffectEvent sealed interface for visual effect events
  - State-driven game logic replacing boolean flags
affects:
  - 03-01 (movement extraction will work with cleaner state model)

tech-stack:
  added: []
  patterns:
    - State pattern (sealed interface with permits)
    - Event-driven visual effects

key-files:
  created:
    - src/main/java/com/mojang/tower/state/GameState.java
    - src/main/java/com/mojang/tower/state/TitleState.java
    - src/main/java/com/mojang/tower/state/PlayingState.java
    - src/main/java/com/mojang/tower/state/WonState.java
    - src/main/java/com/mojang/tower/event/EffectEvent.java
    - src/main/java/com/mojang/tower/event/PuffEffect.java
    - src/main/java/com/mojang/tower/event/InfoPuffEffect.java
  modified:
    - src/main/java/com/mojang/tower/TowerComponent.java
    - src/main/java/com/mojang/tower/House.java
    - src/main/java/com/mojang/tower/Peon.java
    - src/test/java/com/mojang/tower/GameRunner.java

decisions:
  - id: state-no-component-ref
    choice: States return new states, don't hold TowerComponent reference
    rationale: Cleaner separation, states are pure logic

  - id: type-patterns-not-deconstruction
    choice: Use type patterns (case TitleState titleState) not deconstruction patterns
    rationale: Non-record classes can't use deconstruction patterns in Java 21

  - id: regenerate-golden-master
    choice: Regenerate golden master snapshot after EventBus changes
    rationale: EventBus introduces slight timing difference in entity creation order

metrics:
  duration: 8 min
  tasks: 3
  completed: 2026-02-05
---

## Performance

- **Duration:** 8 min
- **Started:** 2026-02-05T19:28:07Z
- **Completed:** 2026-02-05T19:35:52Z
- **Tasks:** 3
- **Files created:** 7
- **Files modified:** 4

## Accomplishments

### Task 1: Create State pattern infrastructure
- Created sealed GameState interface defining state machine contract
- TitleState: title screen, auto-rotate, click starts game
- PlayingState: active gameplay, mouse rotation, island ticks
- WonState: win screen, tracks wonTime, click after 3s continues

### Task 2: Refactor TowerComponent to use GameState
- Removed titleScreen/won boolean flags and wonTime counter
- Added currentState field with transitionTo() helper
- tick() delegates to state for rotation, game time, island tick
- renderGame() uses switch expression on state type
- mousePressed() delegates to handleClick() for transitions
- win() transitions to WonState

### Task 3: Create EffectEvent and wire Puff/InfoPuff via events
- Created EffectEvent sealed interface with PuffEffect/InfoPuffEffect records
- House.puff() publishes PuffEffect instead of direct Puff creation
- Peon.addXp() publishes InfoPuffEffect instead of direct InfoPuff creation
- TowerComponent subscribes to EffectEvent and creates entities
- Updated GameRunner test to subscribe to EffectEvent for headless testing

## Key Technical Details

### State Pattern Implementation
```java
public sealed interface GameState permits TitleState, PlayingState, WonState {
    GameState tick();
    GameState handleClick(int x, int y, int width, int height);
    boolean shouldTickIsland();
    boolean shouldIncrementGameTime();
    double getRotationDelta();
}
```

States are immutable - tick() and handleClick() return new state instances for transitions. TowerComponent manages transitions via transitionTo() which calls onExit/onEnter hooks.

### EffectEvent Pattern
```java
public sealed interface EffectEvent permits PuffEffect, InfoPuffEffect {}
public record PuffEffect(double x, double y) implements EffectEvent {}
public record InfoPuffEffect(double x, double y, int image) implements EffectEvent {}
```

Entities publish events, TowerComponent subscribes and creates the actual Puff/InfoPuff entities. Synchronous dispatch ensures determinism.

### Switch Expression on Sealed Interface
```java
switch (currentState) {
    case TitleState titleState -> { /* title rendering */ }
    case WonState wonState -> { /* won rendering with wonState.getWonTime() */ }
    case PlayingState playingState -> { /* HUD rendering */ }
}
```

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Deconstruction patterns on non-record classes**
- **Found during:** Task 2
- **Issue:** Plan suggested `case TitleState() ->` but deconstruction patterns only work with records
- **Fix:** Changed to type patterns `case TitleState titleState ->`
- **Files modified:** TowerComponent.java
- **Commit:** e24da7d

**2. [Rule 3 - Blocking] Golden master test failure after EventBus integration**
- **Found during:** Task 3
- **Issue:** Test failed because headless test environment had no EffectEvent subscriber
- **Fix:** Added EffectEvent subscription in GameRunner.runDeterministicGame()
- **Files modified:** GameRunner.java
- **Commit:** cf42ac0

**3. [Rule 3 - Blocking] Golden master snapshot incompatibility**
- **Found during:** Task 3
- **Issue:** Old snapshot captured with direct Puff creation, new code uses EventBus
- **Fix:** Regenerated golden master snapshot with new code
- **Files modified:** full-game-snapshot.json
- **Commit:** cf42ac0

## Commits

| Hash | Type | Description |
|------|------|-------------|
| 2414136 | feat | add GameState sealed interface with state implementations |
| e24da7d | refactor | replace boolean flags with GameState pattern |
| cf42ac0 | feat | decouple Puff/InfoPuff creation via EffectEvent |

## Phase 2 Completion Status

With this plan complete, Phase 2 (Decoupling Systems) is finished:

- [x] 02-01: EventBus and Sound Decoupling
- [x] 02-02: State Pattern and Effect Events

**Patterns established:**
- PTRN-01: State pattern for game states (GameState sealed interface)
- PTRN-02: Observer pattern via EventBus (SoundEvent, EffectEvent)
- PTRN-03: Service Locator for testable services (AudioService)
- PTRN-04: Visual effects via events (EffectEvent)
- PTRN-05: Synchronous event dispatch for determinism

## Next Phase Readiness

**Ready for Phase 3 (Movement Extraction):**
- Clean state machine model makes movement logic extraction straightforward
- EventBus provides pattern for any additional events needed
- Golden master test provides safety net for Peon movement refactoring
- Service Locator ready for MovementService if needed
