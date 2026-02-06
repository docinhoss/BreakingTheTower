---
milestone: v2 Pathfinding
audited: 2026-02-06T11:00:00Z
status: tech_debt
scores:
  requirements: 10/13
  phases: 2/3
  integration: 12/12
  flows: 3/3
gaps:
  requirements:
    - DYN-01 (pending - Phase 7)
    - DYN-02 (pending - Phase 7)
    - DYN-03 (pending - Phase 7)
  integration: []
  flows: []
tech_debt:
  - phase: 07-dynamic-recalculation
    items:
      - "Phase skipped - DYN-01/02/03 requirements deferred"
      - "Current behavior: reactive collision-based path invalidation"
      - "Missing: proactive detection of obstacles in current path"
---

# v2 Pathfinding Milestone Audit

**Milestone Goal:** Peons navigate intelligently around obstacles instead of bumping repeatedly
**Audited:** 2026-02-06T11:00:00Z
**Status:** Tech Debt (Phase 7 deferred)

## Executive Summary

| Category | Score | Status |
|----------|-------|--------|
| Requirements | 10/13 (77%) | Phase 7 pending |
| Phases | 2/3 | Phase 7 skipped |
| Integration | 12/12 | All wired |
| E2E Flows | 3/3 | Complete |

Core pathfinding (Phase 5) and unreachable handling (Phase 6) are complete and verified. Dynamic recalculation (Phase 7) is deferred.

## Requirements Coverage

### Satisfied (10/13)

| Requirement | Description | Phase | Evidence |
|-------------|-------------|-------|----------|
| PATH-01 | Peon finds walkable route around obstacles | 5 | AStarPathfinder.findPath() with NavigationGrid |
| PATH-02 | 8-directional movement (diagonals) | 5 | DIRECTIONS array, diagonal cost 14 |
| PATH-03 | Deterministic data structures | 5 | LinkedHashMap, integer costs, 4-level comparator |
| PATH-04 | Completes within tick budget | 5 | 20 peons < 8ms performance test |
| INT-01 | Integrates with MovementSystem | 5 | Peon path following uses MovementRequest |
| INT-02 | Uses NavigationGrid for walkability | 5 | AStarPathfinder calls grid.isOnGround() |
| INT-03 | Accessible via ServiceLocator | 5 | ServiceLocator.pathfinding() accessor |
| REACH-01 | Detects completely blocked targets | 6 | PathResult.NotFound for unwalkable goals |
| REACH-02 | Abandons unreachable quickly (not 10% random) | 6 | Immediate abandonment + blacklist |
| REACH-03 | Node limit prevents unbounded search | 6 | Default 1024, configurable via setMaxNodes() |

### Pending (3/13)

| Requirement | Description | Phase | Status |
|-------------|-------------|-------|--------|
| DYN-01 | Recalculates path when blocked mid-route | 7 | Deferred |
| DYN-02 | Reroutes when obstacle appears in path | 7 | Deferred |
| DYN-03 | Path invalidation without frame spikes | 7 | Deferred |

## Phase Verification

### Phase 5: Core A* and Integration

**Status:** PASSED (11/11 truths verified)
**Verified:** 2026-02-05T23:15:00Z

Key deliverables:
- AStarPathfinder with 8-directional movement
- PathfindingService facade with coordinate conversion
- PathResult sealed interface (Found/NotFound)
- Peon path following with waypoint navigation
- Golden master test passing

### Phase 6: Unreachable Handling

**Status:** PASSED (5/5 truths verified)
**Verified:** 2026-02-06T10:56:00Z

Key deliverables:
- AbandonedTargetSound event
- Peon blacklist infrastructure (60-tick duration)
- Peon.isTrapped() detection (8-neighbor check)
- Configurable node limit (default 1024)
- Deterministic Job.cantReach() (no random 10%)

### Phase 7: Dynamic Recalculation

**Status:** NOT STARTED (skipped by user)

Deferred requirements:
- DYN-01: Path recalculation on blockage
- DYN-02: Reroute when obstacle appears
- DYN-03: Lazy invalidation (no frame spikes)

## Cross-Phase Integration

**Status:** All 12 exports properly wired

### Phase 5 → Phase 6 Connections

| Export | Consumer | Status |
|--------|----------|--------|
| PathfindingService | ServiceLocator, Peon.tick() | CONNECTED |
| PathResult.Found | Peon.java:222 (pattern match) | CONNECTED |
| PathResult.NotFound | Peon.java:228 (triggers abandonment) | CONNECTED |
| GridCell | Peon waypoint following | CONNECTED |
| ServiceLocator.pathfinding() | Peon, TowerComponent | CONNECTED |

### Phase 6 Internal Wiring

| Export | Consumer | Status |
|--------|----------|--------|
| AbandonedTargetSound | EventBus, TowerComponent | CONNECTED |
| Job.getTarget() | Peon.java:239 (blacklist target) | CONNECTED |
| Peon.isTrapped() | Peon.java:232 (NotFound handling) | CONNECTED |
| PathfindingService.setMaxNodes() | Tests, public API | CONNECTED |

## E2E Flow Verification

### Flow 1: Pathfinding Around Obstacle

**Status:** COMPLETE

Trace: Peon → PathfindingService → AStarPathfinder → PathResult.Found → waypoint following

### Flow 2: Unreachable Target Abandonment

**Status:** COMPLETE

Trace: PathResult.NotFound → isTrapped() → blacklist → AbandonedTargetSound → setJob(null)

### Flow 3: Collision-Based Path Invalidation

**Status:** COMPLETE

Trace: MovementResult.Blocked → currentPath = null → random direction → path recomputed next tick

## Test Results

| Test Suite | Tests | Status |
|------------|-------|--------|
| AStarPathfinderTest | 8 | PASS |
| PathfindingServiceTest | 6 | PASS |
| UnreachableHandlingTest | 6 | PASS |
| GameRunnerTest | 3 | PASS |
| GoldenMasterTest | 1 | PASS |
| **Total** | **24** | **BUILD SUCCESS** |

## Tech Debt Assessment

### Phase 7 Deferral Impact

**Current Behavior (Phases 5-6):**
- Peons pathfind around static obstacles correctly
- Peons abandon unreachable targets immediately
- Collision-based path invalidation works (reactive)

**What Phase 7 Would Add:**
- Proactive detection of obstacles appearing in current path
- Rerouting before collision occurs
- Event-driven invalidation when world changes

**User Experience Impact:**
- Peons may bump into newly-placed buildings before rerouting
- This matches original game behavior (no regression)
- Core pathfinding value delivered (around obstacles)

### Recommendation

The milestone delivers its core value: *"Peons navigate intelligently around obstacles instead of bumping repeatedly."*

Phase 7 is an enhancement for handling dynamic world changes. The current collision-based approach is functional and matches original behavior.

**Options:**
1. **Complete milestone** — Accept Phase 7 as future work
2. **Plan Phase 7** — Add dynamic recalculation before completing

---

*Audit completed: 2026-02-06T11:00:00Z*
*Auditor: Claude (gsd-audit-milestone)*
