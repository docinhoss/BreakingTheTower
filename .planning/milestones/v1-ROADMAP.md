# Milestone v1: Breaking the Tower Modernization

**Status:** ✅ SHIPPED 2026-02-05
**Phases:** 1-4
**Total Plans:** 11

## Overview

This milestone transformed Breaking the Tower from Java 1.6 to Java 21 through incremental refactoring that preserved exact gameplay behavior. The journey started with golden master testing (safety net), progressed through event-driven decoupling, extracted movement as a dedicated system (pathfinding integration point), and concluded with sealed hierarchies for type-safe entity handling.

## Phases

### Phase 1: Foundation & Language Modernization

**Goal:** Establish safety infrastructure (golden master tests) and modernize Java syntax without changing architecture
**Depends on:** Nothing (first phase)
**Requirements:** FOUN-01, FOUN-02, FOUN-03, LANG-01, LANG-02, LANG-05, LANG-06, LANG-07
**Success Criteria:**
  1. Project compiles and runs on Java 21 with identical gameplay to original
  2. Golden master test suite captures tick-by-tick state (positions, resources, RNG) and passes
  3. Vec is a record with value semantics (immutable, auto equals/hashCode)
  4. Pattern matching replaces instanceof+cast chains throughout codebase
  5. Modern syntax (var, switch expressions) used where it improves readability

Plans:
- [x] 01-01-PLAN.md — Maven setup + Java 21 compilation (Wave 1)
- [x] 01-02-PLAN.md — Golden master test infrastructure (Wave 2)
- [x] 01-03-PLAN.md — Vec record + syntax modernization (Wave 3)

### Phase 2: Decoupling Systems

**Goal:** Break tight coupling between entities and global services (sounds, effects) using event-driven patterns
**Depends on:** Phase 1
**Requirements:** PTRN-01, PTRN-02, PTRN-03, PTRN-04, PTRN-05
**Success Criteria:**
  1. Game states (title/playing/won) managed via State pattern with explicit transitions
  2. EventBus exists and handles all sound/effect notifications
  3. No direct Sounds.play() calls remain in entity or job classes
  4. Service Locator provides access to Sounds (testable, swappable)
  5. Golden master tests still pass (behavior preserved)

Plans:
- [x] 02-01-PLAN.md — EventBus + ServiceLocator + sound event refactoring (Wave 1)
- [x] 02-02-PLAN.md — State pattern + effect event refactoring (Wave 2)

### Phase 3: Movement Extraction

**Goal:** Separate movement execution from behavior logic, creating the integration point for future pathfinding
**Depends on:** Phase 2
**Requirements:** ARCH-01, ARCH-02, ARCH-03, ARCH-04
**Success Criteria:**
  1. MovementSystem exists as single source of truth for entity movement
  2. Peon movement logic lives in MovementSystem, not in Peon.tick()
  3. Monster movement logic lives in MovementSystem, not in Monster.tick()
  4. Jobs request movement via MovementSystem instead of directly updating positions
  5. Golden master tests still pass (behavior preserved)

Plans:
- [x] 03-01-PLAN.md — MovementSystem infrastructure (Wave 1)
- [x] 03-02-PLAN.md — Peon movement extraction (Wave 2)
- [x] 03-03-PLAN.md — Monster movement extraction (Wave 3)

### Phase 4: Navigation & Sealed Hierarchies

**Goal:** Complete architecture with queryable world representation and type-safe entity hierarchies
**Depends on:** Phase 3
**Requirements:** LANG-03, LANG-04, ARCH-05, ARCH-06
**Success Criteria:**
  1. NavigationGrid interface exists for walkability/collision queries
  2. Island implements NavigationGrid (pathfinding can query the world)
  3. Entity hierarchy is sealed with explicit permits clause
  4. Job class is sealed with explicit permitted implementations
  5. Golden master tests still pass (behavior preserved)

Plans:
- [x] 04-01-PLAN.md — NavigationGrid interface + Island implements it + MovementSystem uses interface (Wave 1)
- [x] 04-02-PLAN.md — Sealed Entity hierarchy with 9 final subclasses (Wave 2)
- [x] 04-03-PLAN.md — Sealed Job class with 6 final nested subclasses (Wave 2)

---

## Milestone Summary

**Key Decisions:**

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Keep AWT/Java2D | Works fine for this game, no benefit from LibGDX migration | ✓ Good |
| Records for Vec/Cost | Immutable, value semantics, perfect for pathfinding later | ✓ Good |
| Sealed hierarchies | Exhaustive pattern matching, clear extension points | ✓ Good |
| Event system for decoupling | Sounds, effects, UI updates shouldn't be hardcoded in entities | ✓ Good |
| Synchronous EventBus | Game determinism requires immediate processing within tick | ✓ Good |
| MovementSystem as service | Clean integration point for future pathfinding | ✓ Good |
| NavigationGrid interface | Dependency inversion for pathfinding queries | ✓ Good |

**Issues Resolved:**
- Java 1.6 syntax modernized to Java 21 (records, sealed classes, pattern matching)
- Tight coupling between entities and Sounds singleton eliminated
- Boolean flags replaced with State pattern
- Movement logic extracted from entities into dedicated service
- World queries abstracted behind NavigationGrid interface

**Issues Deferred:**
- A* pathfinding implementation (v2 milestone)
- Spatial partitioning for entity queries (v2 milestone)
- Object pooling for Puffs (v2 milestone)

**Technical Debt:**
- Vec record created but unused (preparatory for future pathfinding)

---

*Archived: 2026-02-05 as part of v1 milestone completion*
*For current project status, see .planning/ROADMAP.md*
