# Breaking the Tower - Modernization

## What This Is

A modernized version of "Breaking the Tower," Notch's Java-based RTS/god game where players command peons to gather resources, build structures, and ultimately destroy a central tower. The codebase has been updated from Java 1.6 to Java 21 with a clean, event-driven architecture ready for pathfinding integration.

## Core Value

A clean, extensible architecture that makes adding new systems (starting with pathfinding) straightforward without fighting the existing code.

## Current Milestone: v2 Pathfinding

**Goal:** Implement intelligent pathfinding so peons navigate around obstacles instead of walking in straight lines.

**Target features:**
- A* pathfinding algorithm implementation
- Path caching for frequently traveled routes
- Dynamic path recalculation when obstacles change

## v1 Foundation (Shipped: 2026-02-05)

**Codebase:** 4,284 lines of Java (src/main + src/test)
**Tech Stack:** Java 21, Maven, AWT/Java2D, JUnit 5

**Architecture Highlights:**
- EventBus for decoupled sound/effect handling
- ServiceLocator for testable service access
- MovementSystem as single source of truth for entity movement
- NavigationGrid interface for pathfinding-ready world queries
- Sealed hierarchies for Entity (9 types) and Job (6 types)
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

### Active

- [ ] A* pathfinding algorithm implementation
- [ ] Path caching for frequently traveled routes
- [ ] Dynamic path recalculation when obstacles change

### Out of Scope

- Full ECS architecture — overkill for ~20 entity types; incremental extraction sufficient
- Virtual threads — CPU-bound game loop doesn't benefit; adds complexity
- New gameplay features — focus is architecture improvements
- Graphics/asset changes — keeping existing sprites and visuals
- Multiplayer/networking — not in scope

## Context

This is Notch's "Breaking the Tower" - a Ludum Dare style game from the Java applet era.

**Before v1 (original state):**
- Java 1.6 patterns (no generics, verbose syntax)
- Tight coupling (Sounds.play() calls embedded in entities)
- Game state managed via boolean flags
- Movement and behavior intertwined in Job classes

**After v1 (current state):**
- Modern Java 21 (records, sealed classes, pattern matching, switch expressions)
- Loose coupling via EventBus and ServiceLocator
- Explicit State pattern for game states
- MovementSystem separated from behavior logic
- NavigationGrid interface ready for pathfinding

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

---
*Last updated: 2026-02-05 after v2 milestone start*
