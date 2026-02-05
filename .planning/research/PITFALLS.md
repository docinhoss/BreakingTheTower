# Domain Pitfalls: Legacy Java Game Modernization

**Domain:** Java 1.6 to Java 21 game refactoring (Breaking the Tower)
**Researched:** 2026-02-05
**Confidence:** HIGH (verified through codebase analysis and multiple sources)

---

## Critical Pitfalls

Mistakes that cause rewrites, gameplay behavior changes, or major issues.

---

### Pitfall 1: Breaking Random Number Sequence Determinism

**What goes wrong:** Refactoring changes the order of random number calls, causing different gameplay outcomes even with the same seed. The game's entity behavior, spawn locations, and AI decisions diverge from original behavior.

**Why it happens:** The codebase uses `Random` instances throughout (`Peon`, `Entity`, `Island`, `Job` classes). Each entity has its own `Random` instance. Changing iteration order, extraction of methods, or reordering logic changes the random call sequence.

**Consequences:**
- Players experience different gameplay than original
- Regression testing fails to detect issues (different paths taken)
- "It works but plays differently" - the hardest bug to find
- Deterministic replay/testing becomes impossible

**Prevention:**
- **Before refactoring:** Record a "golden master" of game state at tick intervals
- **Preserve call order:** Document every `random.nextX()` call location
- **Use separate Random instances:** Create per-system randomizers that can be compared
- **Add determinism tests:** Capture tick-by-tick state hashes with fixed seeds

**Detection (warning signs):**
- "The game feels different" feedback without code errors
- Tests pass but manual playtesting reveals behavioral drift
- Any refactoring that changes loop order or method call sequence

**Phase:** Address in Phase 1 (Foundation) - establish golden master testing before ANY refactoring

**Sources:**
- [Unexpected Gotchas in Making a Game Deterministic](https://www.jfgeyelin.com/2025/05/unexpected-gotchas-in-making-game.html)
- [Why Your Puzzle Game Should Be Deterministic](https://medium.com/@dev.ios.android/why-your-puzzle-game-should-be-deterministic-99a0ad4a5890)

---

### Pitfall 2: Game Loop Timing Alterations

**What goes wrong:** Refactoring the game loop changes timing behavior, causing physics to run faster/slower, animations to desync, or game speed to vary by frame rate.

**Why it happens:** The current `TowerComponent.run()` uses a fixed timestep accumulator pattern (30 ticks/second). Any change to:
- How `lastTime` is tracked
- The `MAX_TICKS_PER_FRAME` catchup limit
- Sleep duration or Thread behavior
...changes fundamental game behavior.

**Consequences:**
- Game runs at different speeds on different machines
- "Spiral of death" - simulation falls behind and never catches up
- Entity movement distances change per-tick
- Combat balance breaks (damage per second changes)

**Prevention:**
- **Do not refactor the game loop first** - it's the riskiest code
- **Measure before changing:** Log actual ticks/second over 5-minute sessions
- **Preserve the accumulator pattern:** Keep `msPerTick`, `MAX_TICKS_PER_FRAME`, catchup logic
- **Test on slow hardware:** Simulate frame drops to verify catchup behavior

**Detection (warning signs):**
- Game speed varies between runs
- Higher CPU usage without more entities
- Animation "stutters" or "jumps" that didn't exist before

**Phase:** Address in Phase 3 (Architecture) - only after comprehensive behavior tests exist

**Sources:**
- [Fix Your Timestep! | Gaffer On Games](https://gafferongames.com/post/fix_your_timestep/)
- [Taming Time in Game Engines](https://andreleite.com/posts/2025/game-loop/fixed-timestep-game-loop/)

---

### Pitfall 3: Entity Collection Modification During Iteration

**What goes wrong:** Refactoring introduces `ConcurrentModificationException` or skips entities when modifying the `entities` list during `tick()` or `render()` loops.

**Why it happens:** The codebase modifies `island.entities` during iteration:
```java
// In Island.tick()
for (int i = 0; i < entities.size(); i++) {
    Entity entity = entities.get(i);
    entity.tick();
    if (!entity.isAlive()) entities.remove(i--);  // Removal during iteration!
}
```
The current code "works" via manual index adjustment (`i--`). Modern patterns (streams, enhanced for-loops) will break this.

**Consequences:**
- `ConcurrentModificationException` crashes
- Entities skipped or processed twice
- New entities not ticked on spawn frame (or ticked twice)
- Subtle bugs: "sometimes monsters don't attack"

**Prevention:**
- **Preserve the iteration pattern initially** - don't "modernize" to for-each
- **Use deferred removal:** Collect dead entities, remove after loop
- **Create entity lifecycle phases:** spawn queue, active list, removal queue
- **Test with rapid spawn/death cycles:** Monsters spawning while dying

**Detection (warning signs):**
- Crashes only during intense gameplay (many spawns/deaths)
- "Entity acted twice" or "entity never acted" symptoms
- Population counters drift from actual entity count

**Phase:** Address in Phase 2 (Decoupling) - when extracting entity management

**Sources:**
- [Avoiding the ConcurrentModificationException in Java | Baeldung](https://www.baeldung.com/java-concurrentmodificationexception)
- [MSC06-J. Do not modify the underlying collection when an iteration is in progress](https://wiki.sei.cmu.edu/confluence/display/java/MSC06-J.+Do+not+modify+the+underlying+collection+when+an+iteration+is+in+progress)

---

### Pitfall 4: Attempting Full Rewrite Instead of Incremental Refactoring

**What goes wrong:** Developers try to redesign entire systems at once rather than making small, tested changes. The refactored code "should" behave the same but doesn't.

**Why it happens:** The code looks "messy" - tight coupling, mixed responsibilities, hardcoded calls. The temptation is to "fix it properly" with a clean design. But without complete knowledge of edge cases and implicit behaviors, new code introduces regressions.

**Consequences:**
- Weeks of work discarded when subtle bugs emerge
- Loss of implicit behavior that was actually important
- "New code, new bugs" - trading known issues for unknown ones
- Team loses confidence in refactoring

**Prevention:**
- **Small changes, frequent verification:** Extract one method, test, commit
- **Preserve behavior first, improve structure second**
- **Use the Strangler Fig pattern:** New code wraps old code, gradually taking over
- **50-line rule:** Never extract/refactor more than 50 lines without testing

**Detection (warning signs):**
- PRs with hundreds of changed lines
- "This needs a rewrite" statements without test coverage
- Refactoring multiple systems in one change

**Phase:** This is a meta-pitfall - applies to ALL phases

**Sources:**
- [How to Refactor Legacy Java Code Without Breaking Everything](https://medium.com/javarevisited/how-to-refactor-legacy-java-code-without-breaking-everything-f50004e706cf)
- [Best Practices for Modernizing Legacy Java Code - Diffblue](https://www.diffblue.com/resources/best-practices-for-modernizing-legacy-java-code/)

---

## Moderate Pitfalls

Mistakes that cause delays, technical debt, or subtle bugs.

---

### Pitfall 5: Premature State Machine Extraction from Boolean Flags

**What goes wrong:** Replacing boolean flags (`titleScreen`, `won`, `scrolling`, `paused`) with state machines changes implicit state transition rules.

**Why it happens:** The current code has interacting flags:
```java
private boolean titleScreen = true, won = false;
private boolean scrolling = false;
private boolean paused;
```
These have implicit precedence: `paused` overrides everything, `titleScreen` and `won` are mutually exclusive states that interact with `scrolling`. A state machine must capture ALL these relationships.

**Consequences:**
- State transitions that "should" be impossible become possible
- Edge cases break: "What if won becomes true while paused?"
- Original behavior relied on specific flag-check ordering
- New states accidentally introduced (e.g., simultaneous title+won)

**Prevention:**
- **Document current state transitions before refactoring**
- **Create state transition diagrams from the existing code**
- **Test impossible state combinations:** Write tests that verify they stay impossible
- **Extract incrementally:** One flag at a time, not all at once

**Detection (warning signs):**
- "The game got stuck in a weird state"
- Menu/game state flickering
- `if (flag1 && !flag2 && flag3)` conditions after "simplification"

**Phase:** Address in Phase 2 (Decoupling) - when creating GameState system

**Sources:**
- [State - Game Programming Patterns](https://gameprogrammingpatterns.com/state.html)
- [State Machines: The Key to Cleaner, Smarter GameDev Code](https://howtomakeanrpg.com/r/a/state-machines.html)

---

### Pitfall 6: Breaking Tight Coupling Without Preserving Call Timing

**What goes wrong:** Introducing event systems or observers changes WHEN code executes, not just how it's organized. Events might fire synchronously vs. deferred, changing behavior.

**Why it happens:** Current code has direct calls:
```java
// In Island.win()
tower.win();

// In Peon.die()
island.population--;
island.warriorPopulation--;
alive = false;
```
Replacing `island.population--` with `eventBus.emit(PEON_DIED)` may defer the decrement, breaking code that checks `population` immediately after death.

**Consequences:**
- Population counts temporarily wrong, affecting spawn logic
- Victory conditions checked before death processing completes
- "Event storms" - cascading events in unexpected order
- Race conditions in what was deterministic code

**Prevention:**
- **Start with synchronous events:** Maintain exact call timing initially
- **Document call order dependencies:** "X must happen before Y"
- **Use immediate handlers first:** No deferred/queued events until verified
- **Test counts immediately after events:** Verify state consistency

**Detection (warning signs):**
- "Off by one" errors in population/resource counts
- Victory/defeat triggers at wrong times
- Events causing events causing events (cascade)

**Phase:** Address in Phase 2 (Decoupling) - core event system design

**Sources:**
- [Observer - Game Programming Patterns](https://gameprogrammingpatterns.com/observer.html)
- [Event-Driven Architecture in Monoliths: Incremental Refactoring for Java Apps](https://www.javacodegeeks.com/2025/10/event-driven-architecture-in-monoliths-incremental-refactoring-for-java-apps.html)

---

### Pitfall 7: Overusing Java 21 Features Where Simplicity Suffices

**What goes wrong:** Converting working code to lambdas, streams, records, and pattern matching makes it harder to understand without improving behavior, and may subtly change execution.

**Why it happens:** Java 21 has powerful features that feel like "the right way" to write modern Java. But:
- Streams may execute in different order than indexed loops
- Lambdas capture variables differently than explicit iteration
- Records are immutable - converting mutable classes changes semantics

**Consequences:**
- Code becomes harder for future maintainers unfamiliar with functional style
- Stream operations may parallelize unexpectedly
- Lambda variable capture causes subtle bugs
- Performance regression in hot paths (object creation overhead)

**Prevention:**
- **Don't modernize hot paths:** Game loops, per-tick code stays simple
- **Use new features for NEW code, not rewrites**
- **Measure performance:** Streams aren't always faster
- **Keep behavioral code imperative:** AI, physics, game logic

**Detection (warning signs):**
- Frame rate drops after "equivalent" refactoring
- "Clever" one-liners replacing clear 5-line loops
- `.parallelStream()` anywhere in game logic

**Phase:** Address in Phase 4 (Polish) - only after behavior is locked down

**Sources:**
- [Refactoring Legacy Code with Java 8 Lambda Expressions](https://javadzone.com/refactoring-legacy-code-with-java-8-lambda-expressions-a-detailed-guide-with-examples/)
- [From Java 8 to Java 21: A Step-by-Step Migration Guide](https://medium.com/@Games24x7Tech/from-java-8-to-java-21-a-step-by-step-migration-guide-24ec6b41f3ac)

---

### Pitfall 8: God Class Decomposition Without Behavior Tests

**What goes wrong:** Splitting `TowerComponent` (570 lines, handles rendering, input, game state, timing) into smaller classes without comprehensive tests introduces behavioral drift.

**Why it happens:** `TowerComponent` is a "God Class" mixing:
- Game loop management
- Mouse/keyboard input handling
- Rendering coordination
- UI state management
- Win condition checking

Each responsibility seems separable, but they interact in subtle ways (e.g., `synchronized(this)` blocks that prevent input during rendering).

**Consequences:**
- Thread safety breaks (input and rendering desync)
- Initialization order changes, causing null references
- Circular dependencies between extracted classes
- "Works in tests, breaks in game" scenarios

**Prevention:**
- **Write characterization tests FIRST:** Capture current behavior
- **Extract one responsibility at a time:** Input first, then rendering, then state
- **Preserve synchronization:** Keep `synchronized` blocks until proven unnecessary
- **Use dependency injection carefully:** Don't break initialization order

**Detection (warning signs):**
- Null pointer exceptions at startup
- Input lag or missed clicks
- Visual glitches during state transitions
- Deadlocks or race conditions

**Phase:** Address in Phase 3 (Architecture) - major structural work

**Sources:**
- [The God Class Intervention: Avoiding the All-Knowing Anti-Pattern](https://www.wayline.io/blog/god-class-intervention-avoiding-anti-pattern)
- [How to Refactor the God Object Antipattern](https://www.theserverside.com/tip/How-to-refactor-the-God-object-antipattern)

---

## Minor Pitfalls

Mistakes that cause annoyance but are fixable.

---

### Pitfall 9: AWT/Swing Threading Rule Violations

**What goes wrong:** Accessing UI components from non-EDT (Event Dispatch Thread) threads causes intermittent visual glitches or crashes.

**Why it happens:** The current code creates its own game thread and renders directly to a Canvas. This "works" because Canvas is heavyweight and more forgiving, but any Swing components added during modernization require EDT compliance.

**Prevention:**
- **Keep rendering on the game thread** for Canvas-based rendering
- **Use SwingUtilities.invokeLater()** for any Swing additions
- **Don't mix Swing and AWT components** without understanding threading

**Detection:** Visual corruption, intermittent `NullPointerException` in rendering

**Phase:** Phase 4 (Polish) - if adding modern UI elements

---

### Pitfall 10: Resource Loading Path Changes

**What goes wrong:** Modernizing resource loading (images, sounds) breaks paths that worked in the JAR/applet context.

**Why it happens:** The game uses `Bitmaps` and `Sounds` classes that load from relative paths. The hot-reload feature added filesystem-based loading. Java modules (Java 9+) and changed classloader behavior can break both.

**Prevention:**
- **Test in JAR form:** Don't just test from IDE
- **Abstract resource loading early:** Create a ResourceLoader that handles both cases
- **Preserve existing paths:** Don't "clean up" path strings without testing

**Detection:** `IOException` on startup, missing sprites, silent audio failures

**Phase:** Phase 1 (Foundation) - ensure resources load before other changes

---

### Pitfall 11: Instanceof Pattern Matching Side Effects

**What goes wrong:** Java 21's pattern matching for instanceof (`if (e instanceof Monster m)`) changes when the cast variable is in scope, potentially causing compilation errors in else branches or accidental variable shadowing.

**Prevention:**
- **Don't bulk-convert instanceof checks** - do one at a time
- **Watch for variable name conflicts** with existing variables
- **Test both branches** of instanceof checks

**Detection:** Compilation errors, wrong variable referenced in else branch

**Phase:** Phase 4 (Polish) - minor modernization

---

## Phase-Specific Warnings

| Phase | Likely Pitfall | Mitigation |
|-------|---------------|------------|
| Foundation | #1 Random determinism, #10 Resource paths | Establish golden master tests before ANY code changes |
| Decoupling | #3 Collection modification, #5 State machine extraction, #6 Event timing | Create entity lifecycle tests, document state transitions |
| Architecture | #2 Game loop timing, #4 Full rewrite, #8 God class decomposition | Measure timing baselines, extract one system at a time |
| Polish | #7 Overusing Java 21, #9 AWT threading, #11 Pattern matching | Only modernize syntax after behavior is verified |

---

## Behavior Preservation Strategy

Given the constraint of **identical gameplay behavior**, implement this testing approach:

### Golden Master Testing

1. **Record baseline:** Run game with fixed seed, capture tick-by-tick state:
   - Entity positions (x, y) at each tick
   - Population counts
   - Resource values
   - Random number generator states

2. **Create determinism tests:**
   ```java
   // Conceptual - capture state hash at regular intervals
   String stateHash = computeStateHash(island.entities, island.resources);
   assertEquals(expectedHashes[tick], stateHash);
   ```

3. **Run after every refactoring:** Any hash mismatch = behavioral change

### Characterization Tests

1. **Identify implicit behaviors:**
   - What happens when a Peon dies while carrying a resource? (Resource lost)
   - What happens when two monsters target the same Peon? (Both damage)
   - What happens when Tower is destroyed mid-monster-spawn? (Game state)

2. **Write tests that capture current behavior** - even if it seems "wrong"

3. **Only change behavior intentionally** with explicit documentation

### Integration Test Scenarios

- [ ] Full game playthrough with fixed seed produces identical score
- [ ] Entity spawning/death cycles maintain correct population counts
- [ ] Resource gathering completes correctly under combat stress
- [ ] Win condition triggers at exactly the right moment
- [ ] Game loop maintains 30 ticks/second under load

---

## Sources

### Refactoring & Legacy Code
- [How to Refactor Legacy Java Code Without Breaking Everything](https://medium.com/javarevisited/how-to-refactor-legacy-java-code-without-breaking-everything-f50004e706cf)
- [Best Practices for Modernizing Legacy Java Code - Diffblue](https://www.diffblue.com/resources/best-practices-for-modernizing-legacy-java-code/)
- [Taming the Legacy Beast: A Game Developer's Guide to Refactoring](https://www.wayline.io/blog/taming-legacy-beast-game-dev-refactoring)

### Java Migration
- [Modernizing Legacy Java: A Practical Guide to Migrating to Java 17/21+](https://medium.com/@alxkm/modernizing-legacy-java-a-practical-guide-to-migrating-to-java-17-21-b3ab6a215f1f)
- [From Java 8 to Java 21: A Step-by-Step Migration Guide](https://medium.com/@Games24x7Tech/from-java-8-to-java-21-a-step-by-step-migration-guide-24ec6b41f3ac)

### Game Programming Patterns
- [State - Game Programming Patterns](https://gameprogrammingpatterns.com/state.html)
- [Observer - Game Programming Patterns](https://gameprogrammingpatterns.com/observer.html)
- [Fix Your Timestep! | Gaffer On Games](https://gafferongames.com/post/fix_your_timestep/)

### Testing Legacy Code
- [Refactoring Legacy Code: Part 1 - The Golden Master](https://code.tutsplus.com/refactoring-legacy-code-part-1-the-golden-master--cms-20331t)
- [Characterization testing - refactoring legacy code with confidence](https://cloudamite.com/characterization-testing/)
- [Surviving Legacy Code with Golden Master and Sampling](https://blog.thecodewhisperer.com/permalink/surviving-legacy-code-with-golden-master-and-sampling)

### Collections & Concurrency
- [Avoiding the ConcurrentModificationException in Java | Baeldung](https://www.baeldung.com/java-concurrentmodificationexception)

### Determinism
- [Unexpected Gotchas in Making a Game Deterministic](https://www.jfgeyelin.com/2025/05/unexpected-gotchas-in-making-game.html)
