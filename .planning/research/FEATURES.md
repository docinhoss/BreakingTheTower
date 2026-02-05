# Feature Landscape: Game Architecture Design Patterns

**Domain:** Game Development Design Patterns for Entity Systems, AI Behaviors, and Event Handling
**Researched:** 2026-02-05
**Overall Confidence:** HIGH (sourced from authoritative game development resources)

## Executive Summary

This research examines design patterns applicable to refactoring "Breaking the Tower" - an RTS/god game with entity hierarchies, job-based AI, and a tick-based game loop. The current codebase exhibits common game development anti-patterns: tight coupling between systems (entities call `Sounds.play()` directly), inheritance-based entity hierarchies, and boolean state flags for AI behavior.

The patterns identified here focus on three key areas:
1. **Entity Architecture** - Moving from inheritance to composition
2. **AI Behavior Systems** - Evolving from simple jobs to flexible behavior systems
3. **Event-Driven Communication** - Decoupling game systems through messaging

---

## Table Stakes Patterns

Features/patterns that are expected for clean, maintainable game architecture. Missing these creates technical debt.

| Pattern | Why Expected | Complexity | Current Codebase Gap |
|---------|--------------|------------|---------------------|
| **State Pattern** | Eliminates boolean flag proliferation and invalid state combinations | Low | `Peon.wanderTime`, `Job.hasResource`, implicit states in tick() methods |
| **Observer Pattern** | Decouples event producers from consumers; essential for sound/UI/achievements | Low | `Sounds.play()` called directly from `Peon.die()`, `Monster.die()`, `Job.arrived()` |
| **Command Pattern** | Encapsulates actions for AI/input decoupling and undo capability | Low | Jobs partially implement this but tightly couple to Peon instances |
| **Update Method** | Standardizes per-frame entity processing | Low | Already implemented via `Entity.tick()` - this is correct |
| **Component Pattern** | Separates concerns (physics, rendering, AI) into composable modules | Medium | Currently all in Entity subclasses mixing render/tick/collision |
| **Service Locator** | Provides global access to services without Singleton coupling | Low | `Sounds.instance` is a raw Singleton; needs abstraction layer |

### Pattern Details

#### State Pattern (Must Have)
**Source:** [Game Programming Patterns - State](https://gameprogrammingpatterns.com/state.html)

**Problem it solves:** The current codebase uses implicit states through boolean flags and conditional checks:
```java
// Current: scattered state checks in Peon.tick()
if (wanderTime == 0 && job != null && job.hasTarget()) { ... }
else { ... }
```

**Implementation approach:**
- Define explicit state classes (Idle, Moving, Working, Fighting)
- Each state handles its own input/transitions
- Eliminates invalid state combinations that booleans allow

**Confidence:** HIGH - Nystrom explicitly recommends this for any entity with behavior that "changes based on some internal state."

#### Observer Pattern (Must Have)
**Source:** [Game Programming Patterns - Observer](https://gameprogrammingpatterns.com/observer.html)

**Problem it solves:** Current tight coupling:
```java
// Current: Entity death directly calls sound system
public void die() {
    Sounds.play(new Sound.Death());  // Tight coupling!
    island.population--;
    alive = false;
}
```

**Implementation approach:**
- Create `GameEvent` types (EntityDied, ResourceGathered, BuildingComplete)
- Systems subscribe to events they care about
- Entities fire events without knowing who listens

**Confidence:** HIGH - Pattern is synchronous and efficient ("just walking a list and calling methods").

**Pitfall:** The "lapsed listener problem" - unregistered observers cause memory leaks and ghost processing.

#### Command Pattern (Must Have)
**Source:** [Game Programming Patterns - Command](https://gameprogrammingpatterns.com/command.html)

**Problem it solves:** The existing `Job` system is close but couples jobs to specific peon instances and doesn't support actor-independence.

**Implementation approach:**
- Commands become first-class objects passed to actors
- Same command class can control player units or AI units
- Enables command queuing, replay, and potential undo

**Confidence:** HIGH - The Job system already hints at this; full adoption enables AI modularity.

---

## Differentiators

Patterns that enable future extensibility and set the codebase apart. Not strictly required but highly valuable.

| Pattern | Value Proposition | Complexity | Applicability to Project |
|---------|-------------------|------------|-------------------------|
| **Behavior Trees** | Flexible, modular AI that scales better than state machines | Medium | Hunt/Gather/Build jobs would become tree leaves |
| **Event Queue** | Temporal decoupling for cross-system communication | Medium | Sound system, UI updates, achievement triggers |
| **Component-Based Architecture** | Full composition over inheritance; maximum flexibility | High | Would replace Entity hierarchy with data + systems |
| **Spatial Partition** | O(log n) collision/proximity queries vs current O(n) | Medium | `Island.isFree()` and `getEntityAt()` iterate all entities |
| **Object Pool** | Eliminates GC pressure from frequent entity creation | Low | Monster spawning, projectiles, particle effects |
| **Dirty Flag** | Avoids redundant recalculations | Low | Position updates, render sorting |

### Pattern Details

#### Behavior Trees (High Value)
**Source:** [Gamedeveloper - Behavior Trees](https://www.gamedeveloper.com/programming/behavior-trees-for-ai-how-they-work)

**Value:** Current job system is linear - a peon has one job and follows it. Behavior trees enable:
- Priority-based action selection (fight if threatened, else gather)
- Fallback behaviors when primary action fails
- Reusable behavior subtrees across entity types
- Actions that span multiple ticks with Running status

**Node types:**
- **Sequence:** Execute children in order until one fails
- **Selector:** Try children until one succeeds (fallback logic)
- **Decorator:** Modify child behavior (repeat, invert, timeout)
- **Leaf:** Actual game actions (Move, Attack, Gather)

**Migration path from Jobs:**
```
Current Job.Hunt -> BT Sequence[FindTarget, MoveToTarget, Attack]
Current Job.Gather -> BT Sequence[FindResource, MoveToResource, Harvest, ReturnToBase, Deposit]
```

**Confidence:** HIGH - gdx-ai library provides production-ready implementation for Java games.

#### Event Queue (High Value)
**Source:** [Game Programming Patterns - Event Queue](https://gameprogrammingpatterns.com/event-queue.html)

**Value over Observer:** Decouples not just *who* handles events but *when* they handle them.

**Use cases in this codebase:**
- Sound effects (current: immediate, blocks game logic; improved: queued, processed on audio thread)
- UI updates (resource changes, population changes)
- Achievement/statistics tracking

**Implementation consideration:** Ring buffer for constant-time enqueue/dequeue.

**Pitfall:** Events capture state at enqueue time. The document warns: "you have to be careful not to assume the current state of the world reflects how the world was when the event was raised."

**Confidence:** MEDIUM - Adds complexity; evaluate whether simple Observer suffices first.

#### Spatial Partition (High Value for Scale)
**Source:** [Game Programming Patterns - Spatial Partition](https://gameprogrammingpatterns.com/spatial-partition.html)

**Problem:** Current `Island.isFree()` is O(n):
```java
for (int i = 0; i < entities.size(); i++) {
    Entity e = entities.get(i);
    if (e.collides(x, y, r)) return false;
}
```

**Value:** Quadtree or grid-based partition reduces collision queries to O(log n) or O(1).

**When needed:** Essential when pathfinding is added - A* with O(n) neighbor collision checks becomes O(n^2).

**Confidence:** HIGH for pathfinding integration; current entity count may not require it yet.

---

## Anti-Features / Anti-Patterns

Features and patterns to explicitly avoid. Common mistakes in game development.

| Anti-Pattern | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| **God Class** | Monolithic classes become 5,000-line dumping grounds mixing physics, rendering, AI, audio | Use Component pattern to slice along domain boundaries |
| **Deep Inheritance Hierarchies** | Changes propagate unpredictably; "deadly diamond" problems | Prefer composition; entities are containers of components |
| **Singleton for Everything** | Hidden dependencies, global state, testing difficulty | Service Locator with explicit registration; dependency injection |
| **Hardcoded Sound/Effect Calls** | Tight coupling between game logic and presentation | Observer/Event pattern; entities fire events, systems react |
| **Boolean State Flags** | Invalid combinations possible; scattered conditionals | State pattern with explicit state classes |
| **Polling for Events** | Wastes cycles checking conditions every frame | Observer pattern; react when state changes |
| **Premature ECS Adoption** | Full Entity-Component-System adds complexity without guaranteed benefit | Start with Component pattern; evolve to ECS only if justified |

### Anti-Pattern Details

#### God Class Anti-Pattern
**Source:** [The God Class Intervention](https://www.wayline.io/blog/god-class-intervention-avoiding-anti-pattern)

**Current risk:** `Peon` class at 218 lines handles movement, combat, rendering, AI decision-making, leveling, and sound. Growth trajectory leads to unmaintainability.

**Signs of trouble:**
- Methods that "need to know" about multiple domains
- Difficulty assigning single responsibility
- Changes for one feature break unrelated features

**Prevention:** Slice by domain: `MovementComponent`, `CombatComponent`, `RenderComponent`.

#### Hardcoded Sound Calls (Current Problem)
**Source:** [Event Handling Strategies](https://www.numberanalytics.com/blog/event-handling-strategies-game-development)

**Current code smell:**
```java
Sounds.play(new Sound.Death());       // in Peon.die()
Sounds.play(new Sound.MonsterDeath()); // in Monster.die()
Sounds.play(new Sound.Ding());         // in Peon.addXp()
Sounds.play(new Sound.Plant());        // in Island.placeHouse()
```

**Problems:**
- Every new sound effect requires modifying entity code
- Can't disable sounds without code changes
- Mocking for tests is difficult
- Audio logic scattered across codebase

**Solution:** Entities fire `EntityDied`, `LevelUp`, `BuildingPlaced` events. Audio system subscribes and plays appropriate sounds.

#### Boolean State Proliferation (Current Problem)
**Source:** [Beyond If-Else Hell](https://dev.to/niraj_gaming/beyond-if-else-hell-elegant-state-machines-pattern-in-game-development-2i7g)

**Current examples:**
```java
// Job.java
boolean hasResource = false;  // Gather job state
boolean hasSeed = false;      // Plant job state

// Peon.java
private int wanderTime = 0;   // Implicit "wandering" state when > 0
```

**Problems:**
- Adding new states requires new booleans
- Combinations of booleans create 2^n possible states
- Easy to create invalid combinations

**Solution:** Explicit state classes with defined transitions.

---

## Pattern Dependencies

Understanding which patterns depend on others for implementation.

```
Service Locator
    |
    v
Observer Pattern <---- State Pattern
    |                      |
    v                      v
Event Queue          Behavior Trees
    |                      |
    v                      v
[Audio System]       [AI System]

Component Pattern
    |
    +---> Spatial Partition (for collision components)
    +---> Object Pool (for component recycling)
```

**Recommended Implementation Order:**

1. **Service Locator** - Foundation for decoupled service access
2. **Observer Pattern** - Decouple event producers/consumers
3. **State Pattern** - Clean up boolean state flags in Peon/Monster
4. **Command Pattern** - Refactor Job system for actor-independence
5. **Component Pattern** - Extract rendering/physics/AI into components
6. **Behavior Trees** - Replace job system with flexible AI
7. **Spatial Partition** - Required before pathfinding integration
8. **Event Queue** - If Observer proves insufficient

---

## MVP Recommendation

For initial refactoring milestone, prioritize:

### Phase 1: Decouple Sound System (Low Risk, High Value)
1. Implement Service Locator for audio access
2. Add Observer pattern for game events
3. Move all `Sounds.play()` calls to event handlers

**Rationale:** Isolated change that demonstrates pattern value without touching core game logic.

### Phase 2: State Pattern for AI (Medium Risk, High Value)
1. Convert Peon implicit states to explicit State classes
2. Convert Monster targeting/wandering to states
3. Refactor Job to use State internally

**Rationale:** Prepares codebase for Behavior Tree adoption.

### Phase 3: Pathfinding Preparation (Required for Future)
1. Add Spatial Partition to Island
2. Refactor collision queries to use partition
3. Create pathfinding interface (can start with simple A*)

**Rationale:** Current O(n) queries will not scale with pathfinding.

### Defer to Post-MVP:
- **Full Component/ECS Architecture:** High complexity, benefits unclear at current scale
- **Behavior Trees:** Wait until State pattern proves limiting
- **Event Queue:** Start with Observer; upgrade only if async needed

---

## Sources

### Primary Sources (HIGH Confidence)
- [Game Programming Patterns - Full Book](https://gameprogrammingpatterns.com/contents.html) - Robert Nystrom
- [Game Programming Patterns - State](https://gameprogrammingpatterns.com/state.html)
- [Game Programming Patterns - Observer](https://gameprogrammingpatterns.com/observer.html)
- [Game Programming Patterns - Command](https://gameprogrammingpatterns.com/command.html)
- [Game Programming Patterns - Component](https://gameprogrammingpatterns.com/component.html)
- [Game Programming Patterns - Event Queue](https://gameprogrammingpatterns.com/event-queue.html)
- [Game Programming Patterns - Service Locator](https://gameprogrammingpatterns.com/service-locator.html)

### Secondary Sources (MEDIUM Confidence)
- [Gamedeveloper - Behavior Trees](https://www.gamedeveloper.com/programming/behavior-trees-for-ai-how-they-work)
- [gdx-ai Core Architecture](https://deepwiki.com/libgdx/gdx-ai/1.2-core-architecture) - LibGDX AI framework documentation
- [Entity Component System Wikipedia](https://en.wikipedia.org/wiki/Entity_component_system)

### Supporting Sources (LOW Confidence - WebSearch)
- [The God Class Intervention](https://www.wayline.io/blog/god-class-intervention-avoiding-anti-pattern)
- [Event Handling Strategies for Game Development](https://www.numberanalytics.com/blog/event-handling-strategies-game-development)
- [Beyond If-Else Hell - State Machines](https://dev.to/niraj_gaming/beyond-if-else-hell-elegant-state-machines-pattern-in-game-development-2i7g)
