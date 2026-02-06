# Breaking the Tower - Modernization

## What This Is

A modernized version of "Breaking the Tower," Notch's Java-based RTS/god game where players command peons to gather resources, build structures, and ultimately destroy a central tower. The codebase has been updated from Java 1.6 to Java 21 with a clean, event-driven architecture and intelligent A* pathfinding.

## Core Value

A clean, extensible architecture that makes adding new systems straightforward without fighting the existing code.

## Current State

**Version:** v2 Pathfinding (Shipped 2026-02-06)
**Codebase:** 5,336 lines of Java (src/main + src/test)
**Tech Stack:** Java 21, Maven, AWT/Java2D, JUnit 5

**Architecture Highlights:**
- EventBus for decoupled sound/effect handling
- ServiceLocator for testable service access
- MovementSystem as single source of truth for entity movement
- NavigationGrid interface for pathfinding-ready world queries
- PathfindingService with A* algorithm for intelligent navigation
- Sealed hierarchies for Entity (9 types), Job (6 types), PathResult
- Golden master test capturing 5000 ticks of deterministic gameplay

## Requirements

### Validated

- ✓ Entity system with polymorphic hierarchy (Tower, Peon, Monster, House, Rock, Tree, etc.) — existing
- ✓ Job-based behavior system for peon AI (Goto, Hunt, Build, Gather, Plant) — existing
- ✓ Resource economy with three types (wood, rock, food) — existing
- ✓ Building construction with resource costs — existing
- ✓ Combat between warriors and monsters — existing
- ✓ Isometric rendering with rotation — existing
- ✓ Tick-based game loop (30 tps) — existing
- ✓ Win condition: destroy the tower — existing
- ✓ Sound effect system — existing
- ✓ Title screen and win screen UI — existing
- ✓ Java 21 syntax and language features — v1
- ✓ State pattern for game state management (title/playing/won) — v1
- ✓ Observer pattern for game events (sounds, effects via EventBus) — v1
- ✓ Records for immutable data (Vec, Cost, MovementRequest/Result) — v1
- ✓ Sealed classes for entity and job hierarchies — v1
- ✓ Movement/navigation decoupled from behavior logic — v1
- ✓ Queryable world representation (NavigationGrid interface) — v1
- ✓ Service Locator for testable service access — v1
- ✓ Golden master test for behavior preservation — v1
- ✓ A* pathfinding with 8-directional movement — v2
- ✓ Deterministic pathfinding (golden master compatible) — v2
- ✓ Unreachable target detection and immediate abandonment — v2
- ✓ PathfindingService integrated with MovementSystem — v2

### Active

None — planning next milestone

### Out of Scope

- Full ECS architecture — overkill for ~20 entity types; incremental extraction sufficient
- Virtual threads — CPU-bound game loop doesn't benefit; adds complexity
- New gameplay features — focus is architecture improvements
- Graphics/asset changes — keeping existing sprites and visuals
- Multiplayer/networking — not in scope
- Dynamic path recalculation — deferred from v2 (collision-based invalidation works)

## Context

This is Notch's "Breaking the Tower" - a Ludum Dare style game from the Java applet era.

**Before v1 (original state):**
- Java 1.6 patterns (no generics, verbose syntax)
- Tight coupling (Sounds.play() calls embedded in entities)
- Game state managed via boolean flags
- Movement and behavior intertwined in Job classes

**After v2 (current state):**
- Modern Java 21 (records, sealed classes, pattern matching, switch expressions)
- Loose coupling via EventBus and ServiceLocator
- Explicit State pattern for game states
- MovementSystem separated from behavior logic
- A* pathfinding with deterministic tie-breaking
- Peons navigate around obstacles intelligently
- Unreachable targets abandoned immediately (not random 10%)

## Constraints

- **Compatibility**: Must maintain identical gameplay behavior — verified by golden master test
- **Tech stack**: Pure Java 21, no external frameworks (keeping self-contained nature)
- **Build**: Maven producing runnable JAR with resource structure

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Keep AWT/Java2D | Works fine for this game, no benefit from LibGDX migration | ✓ Good |
| Records for Vec/Cost | Immutable, value semantics, perfect for pathfinding | ✓ Good |
| Sealed hierarchies | Exhaustive pattern matching, clear extension points | ✓ Good |
| EventBus for decoupling | Sounds, effects shouldn't be hardcoded in entities | ✓ Good |
| Synchronous EventBus | Game determinism requires immediate processing within tick | ✓ Good |
| MovementSystem service | Clean integration point for future pathfinding | ✓ Good |
| NavigationGrid interface | Dependency inversion for pathfinding queries | ✓ Good |
| Golden master testing | 5000-tick snapshot validates behavior preservation | ✓ Good |
| Integer A* costs (10/14) | Avoids floating-point comparison issues for determinism | ✓ Good |
| LinkedHashMap for A* | Consistent iteration order preserves golden master | ✓ Good |
| PathfindingService facade | Hides A* implementation, provides world coordinate API | ✓ Good |
| 60-tick blacklist duration | Prevents thrashing without permanent blocking | ✓ Good |
| Collision-based invalidation | Reactive approach simpler than event-driven, matches original | ✓ Good |

---
*Last updated: 2026-02-06 after v2 milestone*
