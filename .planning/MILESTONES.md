# Project Milestones: Breaking the Tower

## v2 Pathfinding (Shipped: 2026-02-06)

**Delivered:** Intelligent A* pathfinding — peons navigate around obstacles and abandon unreachable targets immediately

**Phases completed:** 5-6 (4 plans total)

**Key accomplishments:**
- Implemented A* algorithm with 8-directional movement and deterministic tie-breaking
- Created PathfindingService facade with world-to-grid coordinate conversion
- Peons follow computed paths around obstacles via waypoint navigation
- Immediate abandonment of unreachable targets (replacing random 10% chance)
- Blacklist infrastructure prevents re-assignment thrashing (60-tick duration)
- Trapped peon detection (8-neighbor check) with immediate death/respawn

**Stats:**
- 15 files created/modified
- 887 lines added (5,336 total Java LOC)
- 2 phases, 4 plans
- 2 days (2026-02-05 → 2026-02-06)

**Git range:** `feat(05-01)` → `feat(06-02)`

**Requirements:** 10/13 satisfied (PATH-01-04, INT-01-03, REACH-01-03)

**Deferred to future:** Phase 7 (Dynamic Recalculation) — DYN-01/02/03

**What's next:** TBD — consider Phase 7 for proactive path invalidation, or new milestone

---

## v1 Modernization (Shipped: 2026-02-05)

**Delivered:** Java 21 modernization with clean architecture ready for pathfinding integration

**Phases completed:** 1-4 (11 plans total)

**Key accomplishments:**
- Established golden master test capturing 5000 ticks of deterministic gameplay
- Modernized to Java 21 with records (Vec, Cost), sealed classes, pattern matching
- Decoupled entities from services via EventBus and Service Locator patterns
- Extracted MovementSystem as single source of truth for entity movement
- Created NavigationGrid interface for pathfinding-ready world queries
- Sealed Entity hierarchy (9 final subclasses) and Job hierarchy (6 final nested subclasses)

**Stats:**
- 106 files created/modified
- 4,284 lines of Java
- 4 phases, 11 plans
- 53 commits in 1 day

**Git range:** `docs: initialize project` → `docs(04): complete Navigation & Sealed Hierarchies phase`

**What's next:** v2 Pathfinding — A* implementation using the clean MovementSystem/NavigationGrid architecture

---
