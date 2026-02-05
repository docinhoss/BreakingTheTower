# Project Research Summary

**Project:** Breaking the Tower - Java 1.6 to Java 21 Migration
**Domain:** RTS/God Game Modernization
**Researched:** 2026-02-05
**Confidence:** HIGH

## Executive Summary

Breaking the Tower is a working Java 1.6 RTS/god game with entity hierarchies, job-based AI, and tick-based game loop. The modernization path to Java 21 combines language feature adoption with architectural refactoring to enable future pathfinding integration. The recommended approach is **incremental component extraction** rather than full rewrite - preserving behavior while introducing modern patterns.

The highest-value modernizations are records (Vec class becomes 20 lines instead of 76), sealed entity hierarchies (enables exhaustive pattern matching), and pattern matching for instanceof (eliminates casts). The current architecture tightly couples movement execution, behavior decisions, and rendering within entity classes. Movement extraction into a dedicated system is the critical path to enabling pathfinding without rewriting the entire codebase.

The primary risk is **behavioral drift** - refactoring changes the sequence of random number calls, game loop timing, or entity lifecycle processing, causing gameplay to "feel different" even when tests pass. Mitigation requires golden master testing (capture tick-by-tick state hashes) before any refactoring begins, and incremental extraction with frequent verification. The 570-line TowerComponent god class and the interleaved Peon.tick() movement logic are high-risk refactoring targets that must be approached with comprehensive behavior tests.

## Key Findings

### Recommended Stack

Java 21 offers powerful features, but not all are equally valuable for game modernization. Records, sealed classes, and pattern matching deliver the highest value for cleaning up entity hierarchies and job systems. Virtual threads provide minimal benefit for CPU-bound game loops.

**Core technologies:**
- **Java 21 (from 1.6)**: Records, sealed classes, pattern matching — eliminate boilerplate, enable exhaustive type checking
- **Records**: Vec class, Cost types, MovementRequest — immutable value objects with auto-generated equals/hashCode
- **Sealed Classes**: Entity hierarchy, Job types — document valid subtypes, enable exhaustive pattern matching
- **Pattern Matching**: instanceof chains, switch expressions — cleaner type checks without casts
- **var**: Local type inference — reduce redundancy where type is obvious
- **Switch Expressions**: Replace if-else chains in HouseType, Resource handling — concise and exhaustive

**Version requirements:**
- Java 21 LTS (not 17) — sequenced collections, pattern matching for switch finalized
- No module system needed — game runs as monolithic JAR
- No virtual threads — CPU-bound game loop doesn't benefit from I/O concurrency

**Low-priority features:**
- Virtual Threads: LOW value (game loop is CPU-bound, not I/O-bound)
- String Templates: SKIP (removed in JDK 23, pending redesign)
- Full ECS: Defer (current scale doesn't justify complexity)

### Expected Features

Game architecture patterns that separate behavior, movement, and rendering concerns. The existing codebase lacks these separations, making pathfinding integration difficult.

**Must have (table stakes):**
- **State Pattern** — Peon/Monster use implicit boolean states (wanderTime, hasResource); need explicit state classes
- **Observer Pattern** — Sounds.play() called directly from entities; need event-driven decoupling
- **Command Pattern** — Job system partially implements this but couples to specific Peon instances
- **Component Pattern** — Movement, rendering, AI mixed in entity classes; need separation

**Should have (competitive):**
- **Behavior Trees** — Current job system is linear; trees enable priority-based action selection and fallback logic
- **Spatial Partition** — Island.isFree() is O(n); quadtree reduces to O(log n), essential for pathfinding
- **Event Queue** — Decouple when events are processed from when they're raised
- **Service Locator** — Replace raw Singleton pattern (Sounds.instance) with abstraction

**Defer (v2+):**
- **Full ECS Architecture** — High complexity without clear benefit at current scale (~20 entity types)
- **Object Pooling** — GC pressure not yet a problem; add if profiling shows need
- **Dirty Flag Pattern** — Premature optimization until rendering becomes bottleneck

**Anti-features (avoid):**
- **God Classes** — TowerComponent is 570 lines mixing input, rendering, game state, timing
- **Deep Inheritance** — Current Entity hierarchy works but resist adding more depth
- **Hardcoded Sound Calls** — Already problematic; entities directly call Sounds.play()
- **Boolean State Flags** — Peon.wanderTime, Job.hasResource create implicit states
- **Polling for Events** — Checking conditions every frame instead of reacting to changes

### Architecture Approach

The current architecture has working but monolithic structure from the Java applet era. Movement calculation happens inside Peon.tick() where behavior logic (job target selection) and movement logic (velocity, collision) are interleaved. The recommended refactoring is incremental component extraction while preserving behavior.

**Target component boundaries:**
1. **BehaviorSystem** — Decides what entities want to do; manages Jobs; emits MovementRequests
2. **MovementSystem** — Executes movement; pathfinding integration point; handles collision
3. **RenderingSystem** — Transforms and draws entities; separates visual from logic
4. **World Layer** — Spatial queries (Island), navigation data; queryable interface
5. **EventBus** — Decouples notifications for sounds, effects, UI updates

**Major refactoring targets:**
- **Peon.tick() lines 109-156** — Movement interleaved with behavior; extract to MovementSystem
- **TowerComponent (570 lines)** — God class; extract GameState, InputHandler, UIRenderer
- **Job classes** — Convert to sealed interface with record implementations for stateless jobs
- **Entity hierarchy** — Add sealed permits clause for exhaustive pattern matching
- **Vec class (76 lines)** — Convert to record (reduces to ~20 lines)

**Critical integration point:**
The MovementSystem becomes the single place where pathfinding plugs in. Current direct movement (beeline to target) and future A* pathfinding both implement MovementStrategy interface. This enables pathfinding without touching Job classes, House logic, or combat.

**Supporting infrastructure needed:**
- Vec2 record (immutable position type)
- NavigationGrid (queryable walkability for pathfinding)
- MovementRequest (intent from behavior to movement)
- MovementStrategy interface (swappable algorithms)

### Critical Pitfalls

The research identified 11 pitfalls across critical, moderate, and minor severity. The top five that directly impact the refactoring roadmap:

1. **Breaking Random Number Sequence Determinism** — Refactoring changes order of random.nextX() calls, causing different gameplay outcomes. Even identical seed produces different behavior. Prevention: Establish golden master testing (capture tick-by-tick state hashes) before ANY refactoring. Record baseline gameplay with fixed seed.

2. **Game Loop Timing Alterations** — TowerComponent.run() uses fixed timestep accumulator (30 ticks/second). Changes to timing logic cause physics speed changes, animation desync, spiral of death. Prevention: Do NOT refactor game loop until comprehensive behavior tests exist. Measure actual ticks/second before changes. Preserve accumulator pattern.

3. **Entity Collection Modification During Iteration** — Island.tick() removes dead entities during iteration using manual index adjustment (i--). Modern patterns (streams, for-each) break this. Prevention: Preserve iteration pattern initially; use deferred removal queues; test rapid spawn/death cycles.

4. **Attempting Full Rewrite Instead of Incremental Refactoring** — Temptation to "fix it properly" with clean design leads to weeks of work discarded when subtle bugs emerge. Prevention: 50-line rule (never extract more than 50 lines without testing); Strangler Fig pattern (new code wraps old, gradually takes over); preserve behavior first, improve structure second.

5. **Breaking Tight Coupling Without Preserving Call Timing** — Replacing direct calls (island.population--) with events may defer the decrement, breaking code that checks population immediately. Prevention: Start with synchronous events; document call order dependencies; test state consistency immediately after events.

**Additional notable pitfalls:**
- Premature state machine extraction from boolean flags changes implicit transition rules
- Overusing Java 21 features where simplicity suffices (streams may change execution order)
- God class decomposition without behavior tests (TowerComponent's synchronized blocks prevent race conditions)

## Implications for Roadmap

Based on combined research, the migration requires four major phases with specific ordering constraints. Movement extraction is the critical path to pathfinding enablement.

### Phase 1: Foundation & Quick Wins
**Rationale:** Establish safety infrastructure and gain low-risk modernization wins before touching core architecture. Golden master testing must exist before any refactoring to detect behavioral drift.

**Delivers:**
- Golden master test harness (tick-by-tick state verification)
- Java 21 syntax modernization (var, switch expressions, pattern matching instanceof)
- Vec record conversion
- Resource loading abstraction

**Addresses (from FEATURES.md):**
- No architectural patterns yet; pure language modernization

**Avoids (from PITFALLS.md):**
- #1 Random determinism (golden master detects sequence changes)
- #10 Resource loading path changes (test in JAR form)

**Stack elements (from STACK.md):**
- Records (Vec class: 76 lines to ~20 lines)
- var adoption (immediate readability)
- Pattern matching instanceof (remove casts)
- Switch expressions (HouseType.getDescription())

**Critical actions:**
- Record baseline gameplay with fixed seed (1000 ticks minimum)
- Capture state hashes every 10 ticks (entity positions, population, resources, RNG state)
- Create determinism test suite that compares current vs baseline hashes
- DO NOT proceed to Phase 2 without passing golden master tests

### Phase 2: Decoupling Systems
**Rationale:** Break tight coupling between entities and services (sound, effects) without changing movement architecture. Establishes event-driven communication patterns needed for later phases.

**Delivers:**
- EventBus implementation (synchronous initially)
- Sound system decoupling (all Sounds.play() calls become events)
- Service Locator pattern for global services
- GameState extraction from TowerComponent (State pattern for boolean flags)

**Addresses (from FEATURES.md):**
- Observer Pattern (decouple event producers from consumers)
- Service Locator (replace Singleton anti-pattern)
- State Pattern (eliminate titleScreen, won, paused, scrolling boolean flags)

**Avoids (from PITFALLS.md):**
- #5 Premature state machine extraction (document current state transitions first)
- #6 Breaking event timing (start with synchronous events, preserve call order)

**Architecture (from ARCHITECTURE.md):**
- EventBus for sounds, effects, UI notifications
- Extract GameState, InputHandler from TowerComponent
- Preserve TowerComponent game loop (too risky to refactor yet)

**Research flag:** Standard patterns (Observer, Service Locator well-documented); skip deep research.

### Phase 3: Movement & Behavior Extraction
**Rationale:** This is the critical path to pathfinding. Movement logic must be separated from Peon/Monster classes before pathfinding can be integrated. Highest risk phase due to tight coupling in Peon.tick() lines 109-156.

**Delivers:**
- MovementSystem (single integration point for pathfinding)
- BehaviorSystem (Job management separated from movement execution)
- MovementStrategy interface (DirectMovement, WanderMovement)
- NavigationGrid abstraction (wraps Island collision queries)
- MovementRequest type (intent from behavior to movement)
- Component-based entity structure (movement, behavior, render components)

**Addresses (from FEATURES.md):**
- Component Pattern (extract movement, behavior into components)
- Command Pattern (Jobs emit MovementRequests instead of direct movement)
- Spatial Partition preparation (NavigationGrid enables future optimization)

**Avoids (from PITFALLS.md):**
- #2 God class decomposition without tests (MovementSystem extracted incrementally)
- #3 Collection modification during iteration (deferred entity lifecycle processing)
- #4 Full rewrite temptation (extract one concern at a time)

**Architecture (from ARCHITECTURE.md):**
- BehaviorSystem decides intent, emits MovementRequests
- MovementSystem calculates path (PATHFINDING INTEGRATION POINT), applies velocity
- Jobs no longer contain movement steering logic
- Peon/Monster share MovementComponent (eliminates duplication)

**Critical integration points:**
- MovementStrategy.getNextPosition() — where pathfinding plugs in
- NavigationGrid.isWalkable() — abstracts Island.isFree() for pathfinding
- MovementRequest callbacks — notify behavior when arrival occurs

**Research flag:** NEEDS DEEPER RESEARCH during planning. Component extraction from tightly coupled code is complex. May need specific refactoring pattern research (Strangler Fig, Branch by Abstraction).

### Phase 4: Entity Hierarchy Modernization
**Rationale:** With movement/behavior separated, the entity hierarchy can be modernized using sealed classes and pattern matching. Enables exhaustive type checking and cleaner dispatching.

**Delivers:**
- Sealed Entity hierarchy with permits clause
- Sealed Job interface with record implementations (for stateless jobs)
- Pattern matching for switch in entity type dispatch
- Intermediate sealed interfaces (MobileEntity, StaticEntity, EffectEntity)

**Addresses (from FEATURES.md):**
- Table stakes patterns already complete; this is polish

**Stack elements (from STACK.md):**
- Sealed classes (Entity permits Peon, Monster, House, Tree, Rock, FarmPlot, Puff, InfoPuff)
- Pattern matching for switch (exhaustive entity type handling)
- Records for Job implementations (Job.Goto, Job.Hunt as records)

**Avoids (from PITFALLS.md):**
- #7 Overusing Java 21 features (only apply where clear benefit exists)
- #11 Instanceof pattern matching side effects (convert one at a time, test both branches)

**Research flag:** Standard patterns (sealed classes well-documented); skip deep research.

### Phase 5: Game Loop & Rendering Refactoring
**Rationale:** This is the highest-risk remaining refactoring. Only attempt after all behavior tests pass and movement/behavior systems are stable. TowerComponent.run() timing logic must be preserved.

**Delivers:**
- RenderingSystem extraction from entity classes
- Preserved game loop timing (30 ticks/second accumulator)
- Thread safety for input/rendering (preserve synchronized blocks)

**Avoids (from PITFALLS.md):**
- #2 Game loop timing alterations (measure ticks/second before/after; preserve accumulator)
- #8 God class decomposition without tests (extract one responsibility at a time)
- #9 AWT/Swing threading violations (keep rendering on game thread)

**Research flag:** Standard patterns, but HIGH RISK. Needs careful characterization testing. Consider skipping if time-constrained.

### Phase Ordering Rationale

**Why this order:**
1. **Foundation first** — Golden master testing detects behavioral drift; must exist before ANY structural changes
2. **Decoupling before extraction** — EventBus and Service Locator establish patterns used by later phases
3. **Movement before hierarchy** — Pathfinding integration is higher priority than sealed class polish
4. **Game loop last** — Highest risk; only attempt when everything else is stable

**Dependencies discovered:**
- MovementSystem requires NavigationGrid (Vec2 record foundation)
- BehaviorSystem requires MovementRequest type
- Sealed classes require Component extraction (otherwise hierarchy is still monolithic)
- Rendering extraction requires stable game loop (can't refactor both simultaneously)

**Pitfall avoidance strategy:**
- Phase 1 addresses #1, #10 (determinism and resource loading)
- Phase 2 addresses #5, #6 (state machines and event timing)
- Phase 3 addresses #2, #3, #4 (the hardest pitfalls; incremental extraction critical)
- Phase 4 addresses #7, #11 (polish pitfalls only after behavior locked down)
- Phase 5 addresses #2, #8, #9 (game loop is most fragile)

### Research Flags

Phases likely needing deeper research during planning:

- **Phase 3 (Movement & Behavior Extraction):** Complex refactoring of tightly coupled code. May need research on:
  - Strangler Fig pattern for incremental extraction
  - Branch by Abstraction for parallel old/new implementations
  - Game-specific component patterns (libGDX, gdx-ai)
  - Collision query optimization patterns

- **Phase 5 (Game Loop & Rendering):** High-risk refactoring with subtle timing issues. May need research on:
  - Fixed timestep game loop patterns (already cited Gaffer on Games)
  - AWT/Swing threading best practices for games
  - Rendering pipeline patterns for 2D games

Phases with standard patterns (skip research-phase):

- **Phase 1:** Language feature usage well-documented in official JEPs
- **Phase 2:** Observer, Service Locator, State patterns covered in Game Programming Patterns
- **Phase 4:** Sealed classes and pattern matching covered in official Java docs

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | All recommendations based on official JEPs, Oracle docs, verified sources |
| Features | HIGH | Patterns from authoritative Game Programming Patterns book (Nystrom) |
| Architecture | HIGH | Direct codebase analysis combined with established patterns |
| Pitfalls | HIGH | Multiple verified sources (Gaffer on Games, official Java docs, community consensus) |

**Overall confidence:** HIGH

All research areas converge on consistent recommendations. The language features (records, sealed classes, pattern matching) directly support the architectural patterns (Component, Observer, State). The pitfalls align with known risks in legacy refactoring and game development.

### Gaps to Address

The following areas need validation or deeper investigation during planning:

- **Pathfinding integration specifics:** Research identified the integration point (MovementSystem) and requirements (NavigationGrid), but actual pathfinding algorithm selection (A*, JPS, flow fields) needs evaluation during Phase 3 planning. Consider `/gsd:research-phase "Pathfinding algorithms for tile-based RTS"` when Phase 3 begins.

- **Component pattern implementation details:** ARCHITECTURE.md provides the conceptual separation, but actual component lifecycle management (creation, updates, inter-component communication) needs design during Phase 3. The choice between traditional component pattern vs. lightweight ECS should be evaluated based on performance profiling.

- **Testing strategy for behavioral preservation:** PITFALLS.md recommends golden master testing, but implementation details (what state to capture, how often, how to compare) need definition during Phase 1. Consider characterization testing libraries or custom harness.

- **Performance impact of Java 21 features:** Records and pattern matching are generally zero-cost, but sealed class dispatch and switch expressions may have performance implications in hot paths (entity tick loops). Profile during Phase 4 before converting critical code.

- **Build system modernization:** Current codebase compiles to JAR but no build configuration analyzed. Maven/Gradle setup for Java 21 needs evaluation. Ensure hot-reload feature (filesystem-based resource loading) works with new build system.

## Sources

### Primary (HIGH confidence)

**Java 21 Language Features:**
- [Oracle Java 21 Virtual Threads](https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html)
- [JEP 441: Pattern Matching for switch](https://openjdk.org/jeps/441)
- [JEP 395: Records](https://openjdk.org/jeps/395)
- [JEP 409: Sealed Classes](https://openjdk.org/jeps/409)
- [JEP 286: Local Variable Type Inference](https://openjdk.org/jeps/286)
- [JEP 378: Text Blocks](https://openjdk.org/jeps/378)
- [JEP 431: Sequenced Collections](https://openjdk.org/jeps/431)
- [dev.java/learn/records](https://dev.java/learn/records/)

**Game Programming Patterns:**
- [Game Programming Patterns - Full Book](https://gameprogrammingpatterns.com/contents.html) — Robert Nystrom
- [Game Programming Patterns - State](https://gameprogrammingpatterns.com/state.html)
- [Game Programming Patterns - Observer](https://gameprogrammingpatterns.com/observer.html)
- [Game Programming Patterns - Command](https://gameprogrammingpatterns.com/command.html)
- [Game Programming Patterns - Component](https://gameprogrammingpatterns.com/component.html)
- [Game Programming Patterns - Event Queue](https://gameprogrammingpatterns.com/event-queue.html)
- [Game Programming Patterns - Service Locator](https://gameprogrammingpatterns.com/service-locator.html)
- [Game Programming Patterns - Spatial Partition](https://gameprogrammingpatterns.com/spatial-partition.html)

**Game Loop & Timing:**
- [Fix Your Timestep! | Gaffer On Games](https://gafferongames.com/post/fix_your_timestep/)

**Direct Codebase Analysis:**
- Breaking the Tower source code (full analysis performed)

### Secondary (MEDIUM confidence)

**Behavior Trees & AI:**
- [Gamedeveloper - Behavior Trees for AI](https://www.gamedeveloper.com/programming/behavior-trees-for-ai-how-they-work)
- [gdx-ai Core Architecture](https://deepwiki.com/libgdx/gdx-ai/1.2-core-architecture)

**Design Patterns:**
- [Software Patterns Lexicon: Sealed Classes](https://softwarepatternslexicon.com/java/modern-java-features-and-their-impact-on-design/records-and-sealed-classes/understanding-sealed-classes/)
- [Java Design Patterns - Component](https://java-design-patterns.com/patterns/component/)
- [Entity Component System - Wikipedia](https://en.wikipedia.org/wiki/Entity_component_system)
- [ECS FAQ - GitHub](https://github.com/SanderMertens/ecs-faq)

**Legacy Code Refactoring:**
- [How to Refactor Legacy Java Code Without Breaking Everything](https://medium.com/javarevisited/how-to-refactor-legacy-java-code-without-breaking-everything-f50004e706cf)
- [Best Practices for Modernizing Legacy Java Code - Diffblue](https://www.diffblue.com/resources/best-practices-for-modernizing-legacy-java-code/)
- [Modernizing Legacy Java: Practical Guide to Java 17/21+](https://medium.com/@alxkm/modernizing-legacy-java-a-practical-guide-to-migrating-to-java-17-21-b3ab6a215f1f)
- [From Java 8 to Java 21: Step-by-Step Migration Guide](https://medium.com/@Games24x7Tech/from-java-8-to-java-21-a-step-by-step-migration-guide-24ec6b41f3ac)

**Testing Legacy Code:**
- [Refactoring Legacy Code: Part 1 - The Golden Master](https://code.tutsplus.com/refactoring-legacy-code-part-1-the-golden-master--cms-20331t)
- [Characterization Testing - Refactoring with Confidence](https://cloudamite.com/characterization-testing/)
- [Surviving Legacy Code with Golden Master and Sampling](https://blog.thecodewhisperer.com/permalink/surviving-legacy-code-with-golden-master-and-sampling)

**Determinism & Game Development:**
- [Unexpected Gotchas in Making a Game Deterministic](https://www.jfgeyelin.com/2025/05/unexpected-gotchas-in-making-game.html)
- [Why Your Puzzle Game Should Be Deterministic](https://medium.com/@dev.ios.android/why-your-puzzle-game-should-be-deterministic-99a0ad4a5890)
- [Taming Time in Game Engines - Fixed Timestep](https://andreleite.com/posts/2025/game-loop/fixed-timestep-game-loop/)

### Tertiary (LOW confidence - webdev sources)

- [The God Class Intervention](https://www.wayline.io/blog/god-class-intervention-avoiding-anti-pattern)
- [Event Handling Strategies in Game Development](https://www.numberanalytics.com/blog/event-handling-strategies-game-development)
- [Beyond If-Else Hell - State Machines](https://dev.to/niraj_gaming/beyond-if-else-hell-elegant-state-machines-pattern-in-game-development-2i7g)
- [Event-Driven Architecture in Monoliths](https://www.javacodegeeks.com/2025/10/event-driven-architecture-in-monoliths-incremental-refactoring-for-java-apps.html)

---
**Research completed:** 2026-02-05
**Ready for roadmap:** Yes
**Recommended starting phase:** Phase 1 (Foundation & Quick Wins) — establish golden master testing before ANY refactoring
