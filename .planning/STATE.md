# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-02-05)

**Core value:** A clean, extensible architecture that makes adding pathfinding straightforward
**Current focus:** Phase 2 - Decoupling Systems (EventBus, State pattern, Service Locator)

## Current Position

Phase: 2 of 4 (Decoupling Systems)
Plan: 1 of 2 in current phase
Status: In progress
Last activity: 2026-02-05 - Completed 02-01-PLAN.md (EventBus and Sound Decoupling)

Progress: [████░░░░░░] 33% (4/12 plans)

## Phase 1 Completion

**Verified:** 2026-02-05
**Score:** 5/5 must-haves
**Report:** .planning/phases/01-foundation-language-modernization/01-VERIFICATION.md

## Performance Metrics

**Velocity:**
- Total plans completed: 4
- Average duration: 5 min
- Total execution time: 20 min

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01-foundation | 3 | 15 min | 5 min |
| 02-decoupling | 1 | 5 min | 5 min |

**Recent Trend:**
- Last 5 plans: 01-01 (2 min), 01-02 (5 min), 01-03 (8 min), 02-01 (5 min)
- Trend: Stable velocity

*Updated after each plan completion*

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- 01-01: Use Maven over Gradle (simpler setup, consistent with CONTEXT.md)
- 01-01: Preserve original res/ folder for hot-reload development feature
- 01-02: Use Java records for state capture (immutable, auto-equals/hashCode)
- 01-02: Static testSeedBase mechanism in Entity/Job for deterministic Random
- 01-02: HeadlessTowerComponent/Bitmaps stubs for test isolation from AWT
- 01-03: Vec and Cost as records for automatic equals/hashCode (value semantics)
- 01-03: Pattern matching only where cast follows instanceof (not pure type checks)
- 01-03: var only for local variables where type obvious from RHS (new expressions)
- 01-03: Switch expression with default case to match original silent-ignore behavior
- 02-01: Split SoundEvent records into separate public files (Java single public class per file requirement)
- 02-01: Synchronous EventBus dispatch (critical for game determinism)
- 02-01: ConcurrentHashMap + CopyOnWriteArrayList for thread-safe listener management

### Pending Todos

None.

### Blockers/Concerns

**From Research:**
- Phase 3 (Movement Extraction) flagged as highest risk - tightly coupled Peon.tick() lines 109-156
- Golden master testing MUST exist before any structural refactoring (critical safety net)
- Avoid full rewrite temptation - use 50-line extraction rule

**From Phase 1:**
- Golden master snapshot generated and committed (577MB, 5000 ticks)
- Test passes in 4.2s, providing safety net for future refactoring

**From Phase 2:**
- EventBus infrastructure complete, pattern established for sound events
- ServiceLocator ready for additional services (e.g., EffectsService for puffs)

## Session Continuity

Last session: 2026-02-05T19:24:54Z
Stopped at: Completed 02-01-PLAN.md
Resume file: None

---
*State initialized: 2026-02-05*
*Last updated: 2026-02-05*
