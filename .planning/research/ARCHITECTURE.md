# Architecture Patterns

**Domain:** 2D RTS/god game modernization
**Researched:** 2026-02-05
**Overall Confidence:** HIGH (patterns well-established, codebase fully analyzed)

## Executive Summary

Breaking the Tower has a working but monolithic architecture from the Java applet era. The current design tightly couples movement logic, behavior decisions, and rendering within individual entity classes. To support pathfinding and future extensibility, the architecture needs clean separation between these concerns without abandoning the working codebase.

The recommended approach is **incremental component extraction** rather than a full ECS migration. This preserves the existing entity hierarchy while introducing clean interfaces between movement, behavior, and rendering subsystems.

## Current Architecture Analysis

### Component Map

| Class | Responsibilities | Problems |
|-------|------------------|----------|
| `Entity` | Position, collision, rendering stub, world access | Good base, but subclasses override too much |
| `Peon` | Movement execution, job delegation, combat, rendering, health, leveling | Too many concerns; movement intertwined with behavior |
| `Monster` | Target finding, movement, combat, rendering, health | Same movement code duplicated from Peon |
| `Job` | Target selection, movement steering, arrival behavior, resource tracking | Mixes "what to do" with "how to move" |
| `Island` | Entity storage, collision queries, world state, coordinate transform | Good world container, but O(n) entity queries |
| `TowerComponent` | Game loop, input, rendering orchestration, game state, UI | Classic "god class" for main loop |
| `House` | Building behavior, job assignment, spawning, rendering | Behavior logic embedded in entity |

### Current Data Flow

```
TowerComponent.tick()
    |
    v
Island.tick() --> for each Entity: entity.tick()
                        |
                        v
                  Peon.tick()
                    - job.tick()          [behavior timing]
                    - job.hasTarget()     [target selection]
                    - calculate velocity  [movement MIXED with behavior]
                    - island.isFree()     [collision check]
                    - update position     [movement execution]
```

**Critical Issue:** Movement calculation happens inside `Peon.tick()` lines 109-156, where:
- Line 110-121: Job provides target (behavior concern)
- Line 125: Random wander rotation (movement concern)
- Line 132-156: Velocity calculation and collision (movement concern)

These are interleaved, making it impossible to swap in pathfinding without rewriting Peon.

### Coupling Analysis

```
Job <---> Peon         (bidirectional: job modifies peon, peon calls job)
Job ---> Island        (queries world state)
Peon --> Island        (collision checks, entity queries)
House --> Peon         (assigns jobs directly)
House --> Job          (creates job instances)
Entity --> Bitmaps     (rendering resources)
Entity --> Island      (world reference)
```

**Problem areas:**
1. Job classes directly manipulate Peon state (`peon.setJob(null)`, `peon.rot += Math.PI`)
2. Movement code duplicated between Peon (lines 132-156) and Monster (lines 81-92)
3. Sound calls scattered: `Sounds.play()` called from Peon, House, Monster, Tower, etc.
4. House directly creates and assigns Job instances to Peons

## Recommended Architecture

### Target Component Boundaries

```
+-----------------------------------------------------------------------------+
|                              TowerComponent                                  |
|  (game loop, input, orchestration)                                          |
+-----------------------------------------------------------------------------+
         |                    |                    |
         v                    v                    v
+----------------+   +----------------+   +-------------------+
| BehaviorSystem |   | MovementSystem |   | RenderingSystem   |
| (what to do)   |   | (how to move)  |   | (how to display)  |
+----------------+   +----------------+   +-------------------+
         |                    |                    |
         v                    v                    v
+-----------------------------------------------------------------------------+
|                              Entity Layer                                    |
|  Entity, Peon, Monster, House, Tree, etc.                                   |
|  (data holders with minimal behavior)                                       |
+-----------------------------------------------------------------------------+
         |
         v
+-----------------------------------------------------------------------------+
|                              World Layer                                     |
|  Island (spatial queries), NavigationGrid (walkability)                     |
+-----------------------------------------------------------------------------+
```

### Component Responsibilities

| Component | Responsibility | Communicates With |
|-----------|---------------|-------------------|
| **BehaviorSystem** | Decides what entities want to do; manages Jobs | MovementSystem (via movement requests), World Layer |
| **MovementSystem** | Executes movement; pathfinding integration point | World Layer (collision/navigation queries) |
| **RenderingSystem** | Transforms and draws entities | Entity Layer (read-only), Bitmaps |
| **World Layer** | Spatial queries, collision, navigation data | (queries only, no outbound calls) |
| **Entity Layer** | State storage (position, health, job reference) | Accessed by all systems |
| **EventBus** | Decoupled notifications (sounds, effects, UI) | All systems can publish/subscribe |

### Data Flow After Refactoring

```
TowerComponent.tick()
    |
    +---> BehaviorSystem.update()
    |         - For each entity with behavior:
    |         - Job decides target/intent
    |         - Emits MovementRequest (target position, urgency)
    |
    +---> MovementSystem.update()
    |         - For each MovementRequest:
    |         - Calculate path (PATHFINDING INTEGRATION POINT)
    |         - Apply velocity respecting collision
    |         - Update entity positions
    |
    +---> RenderingSystem.render()
              - Sort entities by Y
              - Draw each entity

Events (sounds, effects) flow through EventBus, not direct calls.
```

## Pathfinding Integration Points

### Where Pathfinding Plugs In

The **MovementSystem** becomes the single integration point:

```java
// Current: Direct movement in Peon.tick()
double xt = x + Math.cos(rot) * 0.4 * speed;
if (island.isFree(xt, yt, r, this)) { x = xt; y = yt; }

// After: MovementSystem handles this
interface MovementStrategy {
    Vec2 getNextPosition(Entity entity, Vec2 target, NavigationGrid nav);
}

class DirectMovement implements MovementStrategy { /* current behavior */ }
class PathfindingMovement implements MovementStrategy { /* A* integration */ }
```

### Required Supporting Infrastructure

1. **NavigationGrid**: Queryable grid for walkability
   - Wraps Island.isOnGround() and Island.isFree()
   - Cacheable obstacle positions (rocks, trees, houses)
   - Grid resolution matches pathfinding needs

2. **Vec2 Record**: Immutable position type
   - Replace raw `double x, y` pairs
   - Value semantics for pathfinding data structures
   - Methods: `add()`, `sub()`, `distance()`, `normalize()`

3. **MovementRequest**: Intent from behavior to movement
   - Target position
   - Arrival radius (how close is "arrived")
   - Priority/urgency
   - Callback for arrival notification

## Patterns to Apply

### Pattern 1: Component Pattern for Entities

**What:** Extract rendering and movement into separate component objects within entities.

**When:** Peon and Monster have nearly identical movement code; this is the signal.

**Implementation:**

```java
// Instead of Peon containing all logic:
class Peon extends Entity {
    private MovementComponent movement;  // handles position updates
    private BehaviorComponent behavior;  // handles job/targeting
    private RenderComponent render;      // handles drawing

    void tick() {
        behavior.update(this);  // decides intent
        // movement handled by MovementSystem
    }
}
```

### Pattern 2: Event Bus for Decoupling

**What:** Replace direct `Sounds.play()` calls with event publishing.

**When:** Sound calls are scattered across 10+ locations; effects trigger from entity code.

**Implementation:**

```java
// Current (coupled):
public void die() {
    Sounds.play(new Sound.Death());  // Direct call
    alive = false;
}

// After (decoupled):
public void die() {
    EventBus.publish(new EntityDiedEvent(this));
    alive = false;
}

// SoundSystem subscribes to events
class SoundSystem {
    void onEntityDied(EntityDiedEvent e) {
        if (e.entity instanceof Peon) Sounds.play(new Sound.Death());
        if (e.entity instanceof Monster) Sounds.play(new Sound.MonsterDeath());
    }
}
```

### Pattern 3: Strategy Pattern for Movement

**What:** Make movement algorithm swappable per-entity.

**When:** Preparing for pathfinding without breaking existing behavior.

**Implementation:**

```java
interface MovementStrategy {
    void move(Entity entity, MovementIntent intent, World world);
}

class WanderMovement implements MovementStrategy { /* current random walk */ }
class DirectMovement implements MovementStrategy { /* beeline to target */ }
class PathfindMovement implements MovementStrategy { /* A* to target */ }
```

## Anti-Patterns to Avoid

### Anti-Pattern 1: Big Bang ECS Migration

**What:** Completely replacing the entity hierarchy with a pure ECS system.

**Why bad:**
- Existing code works; this is a refactor not a rewrite
- ECS is overkill for ~20 entity types and hundreds of instances
- Would require rewriting every entity, every test, every behavior

**Instead:** Extract components incrementally. Keep Entity base class, add component references.

### Anti-Pattern 2: Movement Logic in Jobs

**What:** Current Job classes contain movement steering (lines 261-278 in Job.java).

**Why bad:**
- Jobs should express *intent* (go to X), not *how* to get there
- Movement steering duplicated if new jobs added
- Can't swap pathfinding without modifying every Job subclass

**Instead:** Jobs emit MovementRequests; MovementSystem handles execution.

### Anti-Pattern 3: Callback Hell for Arrival

**What:** Jobs call `arrived()` from within movement code.

**Why bad:**
- Couples movement completion to behavior in same call stack
- Complicates pathfinding which may arrive over multiple ticks

**Instead:** MovementSystem notifies BehaviorSystem via callback or event when arrival occurs.

### Anti-Pattern 4: God Class GameLoop

**What:** TowerComponent handles input, rendering, game state, UI, mouse tracking.

**Why bad:**
- Any change risks breaking unrelated functionality
- Hard to test individual concerns

**Instead:** Extract GameState (State pattern), InputHandler, UIRenderer as separate concerns.

## Refactoring Order

Dependencies must be respected. Here is the recommended sequence:

### Phase 1: Foundation (No Behavior Change)

| Step | What | Why First | Depends On |
|------|------|-----------|------------|
| 1.1 | Create `Vec2` record | Used everywhere; no behavior change | Nothing |
| 1.2 | Create `EventBus` | Enables decoupling without changing behavior | Nothing |
| 1.3 | Extract `GameState` (State pattern) | Removes flags from TowerComponent | Nothing |

### Phase 2: World Layer (Enables Pathfinding)

| Step | What | Why Here | Depends On |
|------|------|----------|------------|
| 2.1 | Create `NavigationGrid` interface | Abstracts walkability queries | Vec2 |
| 2.2 | Implement `IslandNavigationGrid` | Wraps current Island methods | NavigationGrid |
| 2.3 | Extract `SpatialIndex` from Island | Faster entity queries (optional) | Nothing |

### Phase 3: Movement Extraction (Critical Path)

| Step | What | Why Here | Depends On |
|------|------|----------|------------|
| 3.1 | Define `MovementStrategy` interface | Abstraction before implementation | Vec2 |
| 3.2 | Extract `DirectMovement` from Peon | Current behavior as strategy | MovementStrategy |
| 3.3 | Create `MovementComponent` | Holds strategy + movement state | DirectMovement |
| 3.4 | Refactor Peon to use MovementComponent | Critical coupling break | MovementComponent |
| 3.5 | Refactor Monster to use same component | Removes duplication | MovementComponent |

### Phase 4: Behavior Extraction

| Step | What | Why Here | Depends On |
|------|------|----------|------------|
| 4.1 | Define `MovementRequest` type | Intent from behavior to movement | Vec2 |
| 4.2 | Refactor Jobs to emit MovementRequests | Decouples job from movement execution | MovementRequest |
| 4.3 | Create `BehaviorComponent` | Holds job reference + state | MovementRequest |
| 4.4 | Wire arrival callbacks | Movement notifies behavior | BehaviorComponent, MovementComponent |

### Phase 5: Rendering Extraction (Low Priority)

| Step | What | Why Last | Depends On |
|------|------|----------|------------|
| 5.1 | Define `Renderable` interface | Standardize rendering contract | Nothing |
| 5.2 | Extract render methods to components | Cleaner separation | Renderable |

### Phase 6: Event Integration

| Step | What | Why Here | Depends On |
|------|------|----------|------------|
| 6.1 | Replace Sounds.play() calls with events | Full decoupling | EventBus |
| 6.2 | Create SoundSystem subscriber | Centralizes sound logic | EventBus |
| 6.3 | Replace effect spawning with events | Consistent pattern | EventBus |

## Dependency Diagram

```
Vec2 (foundation)
  |
  +---> NavigationGrid
  |         |
  |         +---> MovementStrategy
  |                   |
  |                   +---> MovementComponent
  |                             |
  +---> MovementRequest         |
            |                   |
            +---> BehaviorComponent
                      |
                      +---> [Pathfinding can now be added]

EventBus (parallel track)
  |
  +---> SoundSystem
  +---> EffectSystem
```

## Pathfinding Readiness Checklist

After completing Phases 1-4, pathfinding can be added by:

- [ ] Implementing `PathfindingMovement` strategy
- [ ] Implementing A* or similar using NavigationGrid
- [ ] Configuring which entities use pathfinding vs direct movement
- [ ] No changes needed to Job classes, House logic, or combat

This is the target architectural state.

## Java 21 Features to Apply

| Feature | Where | Benefit |
|---------|-------|---------|
| **Records** | Vec2, MovementRequest, events | Immutable value types with equals/hashCode |
| **Sealed classes** | Entity hierarchy, Job types | Exhaustive pattern matching |
| **Pattern matching** | Event handling, entity type checks | Cleaner instanceof handling |
| **Switch expressions** | State transitions, type dispatch | Concise, exhaustive |

## Sources

- [Game Programming Patterns - Decoupling Patterns](https://gameprogrammingpatterns.com/decoupling-patterns.html) (HIGH confidence)
- [Game Programming Patterns - Component Pattern](https://gameprogrammingpatterns.com/component.html) (HIGH confidence)
- [Entity Component System - Wikipedia](https://en.wikipedia.org/wiki/Entity_component_system) (MEDIUM confidence)
- [ECS FAQ - GitHub](https://github.com/SanderMertens/ecs-faq) (MEDIUM confidence)
- [Java Design Patterns - Component](https://java-design-patterns.com/patterns/component/) (MEDIUM confidence)
- Direct codebase analysis of Breaking the Tower (HIGH confidence)

---
*Research produced for roadmap phase planning. Movement extraction (Phase 3) is the critical path for pathfinding enablement.*
