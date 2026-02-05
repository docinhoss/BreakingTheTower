# Breaking the Tower - Modernization

## What This Is

A modernization of "Breaking the Tower," a Java-based RTS/god game where players command peons to gather resources, build structures, and ultimately destroy a central tower. This project updates the codebase from Java 1.6 to Java 21 and refactors the architecture with design patterns to support future feature development.

## Core Value

A clean, extensible architecture that makes adding new systems (starting with pathfinding) straightforward without fighting the existing code.

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

### Active

- [ ] Update to Java 21 syntax and language features
- [ ] Apply State pattern to game state management (title/playing/won)
- [ ] Apply Strategy pattern to Job/behavior system
- [ ] Apply Observer pattern for game events (decoupling sounds, effects)
- [ ] Use Records for immutable data (Vec, positions, paths)
- [ ] Use Sealed classes for entity and job hierarchies
- [ ] Decouple movement/navigation from behavior logic
- [ ] Create queryable world representation for future pathfinding
- [ ] Apply Factory pattern for entity creation

### Out of Scope

- Implement actual pathfinding algorithm — future milestone after architecture is ready
- New gameplay features — focus is architecture, not new content
- Graphics/asset changes — keeping existing sprites and visuals
- Multiplayer/networking — not in scope for this modernization

## Context

This is Notch's "Breaking the Tower" - a Ludum Dare style game from the Java applet era. The codebase is functional but shows its age:

- Uses Java 1.6 patterns (no generics in places, verbose syntax)
- Tight coupling between systems (sound calls embedded in entity logic)
- Game state managed via boolean flags
- Movement and behavior intertwined in Job classes
- Entity creation scattered across codebase

The immediate future goal is implementing proper pathfinding (A* or similar), which requires:
- Clean separation of navigation from behavior
- World representation that can be queried for obstacles/walkability
- Immutable position/vector types with value semantics

Codebase already mapped in `.planning/codebase/`.

## Constraints

- **Compatibility**: Must maintain identical gameplay behavior — this is a refactor, not a redesign
- **Tech stack**: Pure Java 21, no external frameworks (keeping the self-contained nature)
- **Build**: Must still produce runnable JAR with same resource structure

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Keep AWT/Java2D | Works fine for this game, no benefit from LibGDX migration | — Pending |
| Records for Vec/positions | Immutable, value semantics, perfect for pathfinding later | — Pending |
| Sealed hierarchies | Exhaustive pattern matching, clear extension points | — Pending |
| Event system for decoupling | Sounds, effects, UI updates shouldn't be hardcoded in entities | — Pending |

---
*Last updated: 2026-02-05 after initialization*
