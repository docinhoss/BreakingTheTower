# Phase 6: Unreachable Handling - Context

**Gathered:** 2026-02-06
**Status:** Ready for planning

<domain>
## Phase Boundary

Peons detect unreachable targets quickly and fail gracefully. Replace random 10% abandonment with deterministic unreachable detection. Peons surrounded by obstacles die and respawn. Dynamic recalculation when world changes is Phase 7.

</domain>

<decisions>
## Implementation Decisions

### Failure Behavior
- When path not found: abandon job entirely, become idle
- Emit event on abandonment (enables future sound/effect)
- Distinguish "target blocked" from "peon trapped":
  - Target blocked: abandon job, emit event, become idle
  - Peon trapped: immediate death, respawn at Tower
- Trapped death uses existing death logic (no distinct event type)
- No resource cost or penalty for trapped-peon death

### Search Limits
- Node limit: 1024 nodes (moderate, configurable via PathfindingService)
- Result is simply "no path found" — no distinction between "definitely unreachable" and "gave up searching"
- No early-exit optimization for surrounded start position (let A* handle naturally)

### Partial Paths
- No partial paths — either full path exists or abandon immediately
- No "move closer and reassess" behavior

### Retry Policy
- After abandoning: no retry, move on to new assignment
- Per-peon blacklist of failed targets (60 ticks duration)
- Prevents immediate re-assignment thrashing
- Blacklist cleared by time, not by world changes (that's Phase 7)

</decisions>

<specifics>
## Specific Ideas

- Trapped peon death should feel like a consequence of bad player placement, not a bug
- Blacklist duration (60 ticks = ~2 seconds) keeps game feeling responsive while avoiding thrashing

</specifics>

<deferred>
## Deferred Ideas

- Clearing blacklist when world changes — belongs in Phase 7 (Dynamic Recalculation)

</deferred>

---

*Phase: 06-unreachable-handling*
*Context gathered: 2026-02-06*
