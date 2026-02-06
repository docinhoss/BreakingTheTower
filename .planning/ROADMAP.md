# Roadmap: Breaking the Tower

## Milestones

- v1.0 Modernization - Phases 1-4 (shipped 2026-02-05)
- v2 Pathfinding - Phases 5-7 (in progress)

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

### v2 Pathfinding (In Progress)

**Milestone Goal:** Peons navigate intelligently around obstacles instead of bumping repeatedly.

- [x] **Phase 5: Core A* and Integration** - Deterministic pathfinding with MovementSystem integration
- [ ] **Phase 6: Unreachable Handling** - Graceful handling of blocked targets
- [ ] **Phase 7: Dynamic Recalculation** - Path updates when world changes

## Phase Details

### Phase 5: Core A* and Integration
**Goal**: Peons find and follow walkable paths around obstacles
**Depends on**: Phase 4 (NavigationGrid interface)
**Requirements**: PATH-01, PATH-02, PATH-03, PATH-04, INT-01, INT-02, INT-03
**Success Criteria** (what must be TRUE):
  1. Peon walks around a rock to reach a target on the other side
  2. Peon moves diagonally when that is the shortest path
  3. Running the same scenario twice produces identical peon movement
  4. Game maintains 30 tps with 20 peons pathfinding simultaneously
  5. Golden master test still passes (determinism preserved)
**Plans**: 2 plans

Plans:
- [x] 05-01-PLAN.md - Implement A* algorithm with deterministic types and tests
- [x] 05-02-PLAN.md - Integrate PathfindingService with Peon path following

### Phase 6: Unreachable Handling
**Goal**: Peons detect and abandon unreachable targets quickly
**Depends on**: Phase 5
**Requirements**: REACH-01, REACH-02, REACH-03
**Success Criteria** (what must be TRUE):
  1. Peon surrounded by obstacles gives up immediately (not random 10% abandon)
  2. Peon targeting entity completely walled off abandons within 1 tick
  3. Pathfinding search terminates within node limit (no unbounded exploration)
**Plans**: 2 plans

Plans:
- [ ] 06-01-PLAN.md - Infrastructure: AbandonedTargetSound event, configurable node limit, Job.getTarget() accessor
- [ ] 06-02-PLAN.md - Peon behavior: blacklist, trapped detection, immediate abandonment

### Phase 7: Dynamic Recalculation
**Goal**: Peons reroute when obstacles appear or disappear mid-journey
**Depends on**: Phase 6
**Requirements**: DYN-01, DYN-02, DYN-03
**Success Criteria** (what must be TRUE):
  1. Peon blocked mid-path finds alternative route without user intervention
  2. Peon reroutes when house is built in its path
  3. Path recalculation does not cause visible frame drops (lazy invalidation)
**Plans**: TBD

Plans:
- [ ] 07-01: TBD

## Progress

**Execution Order:** 5 -> 6 -> 7

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1. Foundation | v1.0 | 3/3 | Complete | 2026-02-05 |
| 2. Decoupling | v1.0 | 2/2 | Complete | 2026-02-05 |
| 3. Movement | v1.0 | 3/3 | Complete | 2026-02-05 |
| 4. Navigation | v1.0 | 3/3 | Complete | 2026-02-05 |
| 5. Core A* and Integration | v2 | 2/2 | Complete | 2026-02-05 |
| 6. Unreachable Handling | v2 | 0/2 | Not started | - |
| 7. Dynamic Recalculation | v2 | 0/? | Not started | - |

---
*Roadmap created: 2026-02-05*
*Last updated: 2026-02-06 - Phase 6 plans created*
