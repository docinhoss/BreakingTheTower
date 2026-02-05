# Roadmap: Breaking the Tower Modernization

## Overview

This roadmap transforms Breaking the Tower from Java 1.6 to Java 21 through incremental refactoring that preserves exact gameplay behavior. The journey starts with golden master testing (safety net), progresses through event-driven decoupling, extracts movement as a dedicated system (pathfinding integration point), and concludes with sealed hierarchies for type-safe entity handling. Each phase builds on the previous, with movement extraction being the critical path to the ultimate goal: clean pathfinding integration.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3, 4): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

- [x] **Phase 1: Foundation & Language Modernization** - Golden master testing + Java 21 syntax adoption
- [x] **Phase 2: Decoupling Systems** - EventBus, State pattern, Service Locator
- [x] **Phase 3: Movement Extraction** - Separate movement from behavior (pathfinding integration point)
- [x] **Phase 4: Navigation & Sealed Hierarchies** - NavigationGrid + sealed classes for type safety

## Phase Details

### Phase 1: Foundation & Language Modernization
**Goal**: Establish safety infrastructure (golden master tests) and modernize Java syntax without changing architecture
**Depends on**: Nothing (first phase)
**Requirements**: FOUN-01, FOUN-02, FOUN-03, LANG-01, LANG-02, LANG-05, LANG-06, LANG-07
**Success Criteria** (what must be TRUE):
  1. Project compiles and runs on Java 21 with identical gameplay to original
  2. Golden master test suite captures tick-by-tick state (positions, resources, RNG) and passes
  3. Vec is a record with value semantics (immutable, auto equals/hashCode)
  4. Pattern matching replaces instanceof+cast chains throughout codebase
  5. Modern syntax (var, switch expressions) used where it improves readability
**Plans**: 3 plans in 3 waves

Plans:
- [x] 01-01-PLAN.md — Maven setup + Java 21 compilation (Wave 1)
- [x] 01-02-PLAN.md — Golden master test infrastructure (Wave 2)
- [x] 01-03-PLAN.md — Vec record + syntax modernization (Wave 3)

### Phase 2: Decoupling Systems
**Goal**: Break tight coupling between entities and global services (sounds, effects) using event-driven patterns
**Depends on**: Phase 1
**Requirements**: PTRN-01, PTRN-02, PTRN-03, PTRN-04, PTRN-05
**Success Criteria** (what must be TRUE):
  1. Game states (title/playing/won) managed via State pattern with explicit transitions
  2. EventBus exists and handles all sound/effect notifications
  3. No direct Sounds.play() calls remain in entity or job classes
  4. Service Locator provides access to Sounds (testable, swappable)
  5. Golden master tests still pass (behavior preserved)
**Plans**: 2 plans in 2 waves

Plans:
- [x] 02-01-PLAN.md — EventBus + ServiceLocator + sound event refactoring (Wave 1)
- [x] 02-02-PLAN.md — State pattern + effect event refactoring (Wave 2)

### Phase 3: Movement Extraction
**Goal**: Separate movement execution from behavior logic, creating the integration point for future pathfinding
**Depends on**: Phase 2
**Requirements**: ARCH-01, ARCH-02, ARCH-03, ARCH-04
**Success Criteria** (what must be TRUE):
  1. MovementSystem exists as single source of truth for entity movement
  2. Peon movement logic lives in MovementSystem, not in Peon.tick()
  3. Monster movement logic lives in MovementSystem, not in Monster.tick()
  4. Jobs request movement via MovementSystem instead of directly updating positions
  5. Golden master tests still pass (behavior preserved)
**Plans**: 3 plans in 3 waves

Plans:
- [x] 03-01-PLAN.md — MovementSystem infrastructure (Wave 1)
- [x] 03-02-PLAN.md — Peon movement extraction (Wave 2)
- [x] 03-03-PLAN.md — Monster movement extraction (Wave 3)

### Phase 4: Navigation & Sealed Hierarchies
**Goal**: Complete architecture with queryable world representation and type-safe entity hierarchies
**Depends on**: Phase 3
**Requirements**: LANG-03, LANG-04, ARCH-05, ARCH-06
**Success Criteria** (what must be TRUE):
  1. NavigationGrid interface exists for walkability/collision queries
  2. Island implements NavigationGrid (pathfinding can query the world)
  3. Entity hierarchy is sealed with explicit permits clause
  4. Job class is sealed with explicit permitted implementations
  5. Golden master tests still pass (behavior preserved)
**Plans**: 3 plans in 2 waves

Plans:
- [x] 04-01-PLAN.md — NavigationGrid interface + Island implements it + MovementSystem uses interface (Wave 1)
- [x] 04-02-PLAN.md — Sealed Entity hierarchy with 9 final subclasses (Wave 2)
- [x] 04-03-PLAN.md — Sealed Job class with 6 final nested subclasses (Wave 2)

## Progress

**Execution Order:**
Phases execute in numeric order: 1 -> 2 -> 3 -> 4

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Foundation & Language Modernization | 3/3 | ✓ Complete | 2026-02-05 |
| 2. Decoupling Systems | 2/2 | ✓ Complete | 2026-02-05 |
| 3. Movement Extraction | 3/3 | ✓ Complete | 2026-02-05 |
| 4. Navigation & Sealed Hierarchies | 3/3 | ✓ Complete | 2026-02-05 |

---
*Roadmap created: 2026-02-05*
*Depth: quick (3-5 phases)*
*Coverage: 19/19 v1 requirements mapped*
