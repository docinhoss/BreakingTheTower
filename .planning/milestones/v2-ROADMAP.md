# Milestone v2: Pathfinding

**Status:** SHIPPED 2026-02-06
**Phases:** 5-6
**Total Plans:** 4

## Overview

Intelligent pathfinding so peons navigate around obstacles instead of walking in straight lines. Includes A* algorithm implementation, unreachable target handling, and deterministic behavior preservation.

## Phases

### Phase 5: Core A* and Integration

**Goal**: Peons find and follow walkable paths around obstacles
**Depends on**: Phase 4 (NavigationGrid interface)
**Requirements**: PATH-01, PATH-02, PATH-03, PATH-04, INT-01, INT-02, INT-03
**Success Criteria**:
  1. Peon walks around a rock to reach a target on the other side
  2. Peon moves diagonally when that is the shortest path
  3. Running the same scenario twice produces identical peon movement
  4. Game maintains 30 tps with 20 peons pathfinding simultaneously
  5. Golden master test still passes (determinism preserved)

Plans:
- [x] 05-01-PLAN.md - Implement A* algorithm with deterministic types and tests
- [x] 05-02-PLAN.md - Integrate PathfindingService with Peon path following

**Details:**
- GridCell record for 96x96 grid (4 world units per cell)
- PathNode with g/h/f scores for A* algorithm state
- PathResult sealed interface (Found/NotFound) for exhaustive handling
- AStarPathfinder with LinkedHashMap for deterministic iteration
- Integer costs (10 cardinal, 14 diagonal) avoiding floating-point
- PathfindingService facade with world coordinate conversion
- Peon path following with waypoint navigation
- Path invalidation when target moves >= 4 world units

### Phase 6: Unreachable Handling

**Goal**: Peons detect and abandon unreachable targets quickly
**Depends on**: Phase 5
**Requirements**: REACH-01, REACH-02, REACH-03
**Success Criteria**:
  1. Peon surrounded by obstacles gives up immediately (not random 10% abandon)
  2. Peon targeting entity completely walled off abandons within 1 tick
  3. Pathfinding search terminates within node limit (no unbounded exploration)

Plans:
- [x] 06-01-PLAN.md - Infrastructure: AbandonedTargetSound event, configurable node limit, Job.getTarget() accessor
- [x] 06-02-PLAN.md - Peon behavior: blacklist, trapped detection, immediate abandonment

**Details:**
- AbandonedTargetSound event for abandonment notifications
- Configurable node limit (default 1024) via PathfindingService
- Job.getTarget() accessor for blacklist management
- LinkedHashMap blacklist with 60-tick expiry
- isTrapped() checks all 8 neighbors via isOnGround()
- Immediate abandonment on PathResult.NotFound
- Deterministic Job.cantReach() (no random 10%)

### Phase 7: Dynamic Recalculation (DEFERRED)

**Goal**: Peons reroute when obstacles appear or disappear mid-journey
**Depends on**: Phase 6
**Requirements**: DYN-01, DYN-02, DYN-03
**Status**: Not started — deferred to future milestone

Plans:
- [ ] 07-01: TBD

---

## Milestone Summary

**Key Decisions:**
- Integer costs (10/14) for determinism — avoids floating-point comparison issues
- LinkedHashMap for closed set and blacklist — consistent iteration order for golden master
- PathfindingService as public facade — hides AStarPathfinder implementation
- Path invalidation threshold of 4 world units (1 grid cell)
- Node limit default 1024 (configurable via PathfindingService)
- Blacklist duration 60 ticks — prevents thrashing without permanent blocking

**Issues Resolved:**
- Peons no longer walk through obstacles
- Unreachable targets abandoned immediately (not random 10%)
- Trapped peons die and respawn (not stuck forever)
- Golden master preserved through deterministic implementation

**Issues Deferred:**
- Phase 7 (Dynamic Recalculation) — proactive path invalidation when world changes
- DYN-01/02/03 requirements pending

**Technical Debt Incurred:**
- Collision-based path invalidation is reactive (bump then reroute) rather than proactive
- AbandonedTargetSound returns null (no sound asset yet)

---

*For current project status, see .planning/ROADMAP.md*
