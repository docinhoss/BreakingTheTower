# Roadmap: Breaking the Tower

## Milestones

- v1.0 Modernization - Phases 1-4 (shipped 2026-02-05)
- v2 Pathfinding - Phases 5-6 (shipped 2026-02-06)

## Phases

<details>
<summary>v1.0 Modernization (Phases 1-4) - SHIPPED 2026-02-05</summary>

See `.planning/milestones/v1-ROADMAP.md` for archived v1 roadmap.

**Summary:** Java 21 modernization with clean architecture ready for pathfinding integration.
- Phase 1: Foundation (golden master, Java 21 syntax)
- Phase 2: Decoupling (EventBus, ServiceLocator)
- Phase 3: Movement (MovementSystem extraction)
- Phase 4: Navigation (NavigationGrid, sealed hierarchies)

</details>

<details>
<summary>v2 Pathfinding (Phases 5-6) - SHIPPED 2026-02-06</summary>

See `.planning/milestones/v2-ROADMAP.md` for archived v2 roadmap.

**Summary:** Intelligent A* pathfinding so peons navigate around obstacles.
- Phase 5: Core A* and Integration (2 plans)
- Phase 6: Unreachable Handling (2 plans)

**Deferred:** Phase 7 (Dynamic Recalculation) — moved to future work

</details>

### Future Work

- [ ] **Phase 7: Dynamic Recalculation** - Path updates when world changes (DYN-01, DYN-02, DYN-03)

## Progress

**Execution Order:** 1 -> 2 -> 3 -> 4 -> 5 -> 6

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1. Foundation | v1.0 | 3/3 | Complete | 2026-02-05 |
| 2. Decoupling | v1.0 | 2/2 | Complete | 2026-02-05 |
| 3. Movement | v1.0 | 3/3 | Complete | 2026-02-05 |
| 4. Navigation | v1.0 | 3/3 | Complete | 2026-02-05 |
| 5. Core A* and Integration | v2 | 2/2 | Complete | 2026-02-05 |
| 6. Unreachable Handling | v2 | 2/2 | Complete | 2026-02-06 |
| 7. Dynamic Recalculation | Future | 0/? | Deferred | - |

---
*Roadmap created: 2026-02-05*
*Last updated: 2026-02-06 - v2 milestone complete*
