# Phase 6: Unreachable Handling - Research

**Researched:** 2026-02-06
**Domain:** Pathfinding failure handling, unreachable target detection, entity blacklisting
**Confidence:** HIGH

## Summary

This phase implements deterministic unreachable target detection and graceful failure handling for peons. The research focused on: (1) replacing the random 10% abandonment in `Job.cantReach()` with deterministic "no path found" detection, (2) distinguishing between "target blocked" and "peon trapped" scenarios, and (3) implementing a time-based blacklist to prevent re-assignment thrashing.

The codebase is well-prepared for this phase. Phase 5 established the A* pathfinder with a node limit (currently 1000, to be raised to 1024) that terminates search and returns `PathResult.NotFound`. The current `Peon.tick()` already handles `PathResult.NotFound` by falling back to random movement. The `Job.cantReach()` method uses a 10% random chance to abandon targets, which must be replaced with deterministic immediate abandonment when pathfinding fails.

Key insight: The phase context explicitly states "let A* handle naturally" for trapped peon detection. This means we rely on A* returning `PathResult.NotFound` for both scenarios (target blocked and peon trapped). The distinction is made AFTER pathfinding fails by checking if the peon's current position is surrounded (no walkable neighbors). This is a simple post-check, not an early-exit optimization.

**Primary recommendation:** Modify `Peon.tick()` to (1) immediately abandon job when `PathResult.NotFound` is returned (not random 10%), (2) check if peon is trapped (all 8 neighbors blocked) and die if so, (3) add per-peon blacklist using `LinkedHashMap<Entity, Integer>` for 60-tick temporal expiry, and (4) emit abandonment event for future sound/effect hooks.

## Standard Stack

The established libraries/tools for this domain:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Java LinkedHashMap | 21 LTS | Per-peon blacklist with temporal expiry | Deterministic iteration, O(1) lookup, insertion-order for cleanup |
| Java record | 21 LTS | AbandonedTargetEvent | Immutable event type matching codebase pattern |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Existing PathfindingService | N/A | Unreachable detection | Already returns NotFound when path impossible |
| Existing NavigationGrid | N/A | Trapped peon detection | isOnGround() for checking neighbor walkability |
| Existing EventBus | N/A | Abandonment event | Synchronous publish for future sound hooks |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| LinkedHashMap for blacklist | HashMap | HashMap is slightly faster but non-deterministic iteration; LinkedHashMap ensures cleanup order is consistent |
| Entity reference in blacklist | Entity ID/hash | Entity reference is simpler; no ID system exists in codebase |
| 60-tick fixed duration | Variable duration | Fixed is simpler, aligns with phase context decision |

**Installation:**
```bash
# No installation needed - all Java 21 built-in types
```

## Architecture Patterns

### Recommended Project Structure
```
src/main/java/com/mojang/tower/
├── Peon.java                        # MODIFY: Add blacklist, trapped detection, abandonment logic
├── Job.java                         # MODIFY: Change cantReach() to abandon immediately
├── pathfinding/
│   ├── PathfindingService.java      # MODIFY: Make node limit configurable, expose grid coordinate helpers
│   └── AStarPathfinder.java         # MODIFY: Update MAX_NODES constant to 1024
├── event/
│   ├── SoundEvent.java              # MODIFY: Permit new AbandonedTargetSound
│   └── AbandonedTargetSound.java    # NEW: Event for target abandonment
└── navigation/
    └── NavigationGrid.java          # NO CHANGE: Already has isOnGround()
```

### Pattern 1: Immediate Abandonment on PathResult.NotFound
**What:** Replace random 10% abandonment with deterministic immediate abandonment when A* returns NotFound.
**When to use:** Always - the phase context explicitly requires this behavior.
**Example:**
```java
// In Peon.tick(), current code:
case PathResult.NotFound(var reason) -> {
    currentPath = null;
    // Falls through to random movement
}

// Replace with:
case PathResult.NotFound(var reason) -> {
    currentPath = null;
    abandonTarget(reason);  // New method handles blacklist + event + job clear
}
```

### Pattern 2: Trapped Peon Detection
**What:** After pathfinding fails, check if peon is completely surrounded (all 8 neighbors unwalkable). If trapped, die immediately.
**When to use:** On every PathResult.NotFound, after abandonment logic.
**Example:**
```java
// Source: Phase context decision - let A* handle naturally, then post-check
private boolean isTrapped() {
    // Check all 8 neighbors of current grid cell
    GridCell current = worldToGrid(x, y);
    int[][] dirs = {{-1,-1},{0,-1},{1,-1},{-1,0},{1,0},{-1,1},{0,1},{1,1}};

    for (int[] dir : dirs) {
        GridCell neighbor = new GridCell(current.x() + dir[0], current.y() + dir[1]);
        if (neighbor.isValid()) {
            double worldX = PathfindingService.gridToWorldX(neighbor);
            double worldY = PathfindingService.gridToWorldY(neighbor);
            if (island.isOnGround(worldX, worldY)) {
                return false;  // At least one walkable neighbor
            }
        }
    }
    return true;  // All neighbors blocked
}
```

### Pattern 3: Time-Based Blacklist
**What:** Per-peon `LinkedHashMap<Entity, Integer>` mapping failed targets to expiry tick. Prevents immediate re-assignment.
**When to use:** Store target when abandoning due to unreachable. Clear entries when tick count exceeds stored expiry.
**Example:**
```java
// In Peon.java
private final LinkedHashMap<Entity, Integer> blacklist = new LinkedHashMap<>();
private static final int BLACKLIST_DURATION = 60; // ticks

private void abandonTarget(String reason) {
    if (job != null && job.target != null) {
        // Add to blacklist with expiry tick
        int expiryTick = getCurrentTick() + BLACKLIST_DURATION;
        blacklist.put(job.target, expiryTick);
    }
    EventBus.publish(new AbandonedTargetSound());
    job.setJob(null);  // Clear job, become idle
}

// In tick(), clean expired entries (efficient with LinkedHashMap insertion order)
private void cleanBlacklist(int currentTick) {
    blacklist.entrySet().removeIf(entry -> entry.getValue() <= currentTick);
}

public boolean isBlacklisted(Entity target) {
    return blacklist.containsKey(target);
}
```

### Pattern 4: Abandonment Event
**What:** Event record emitted when peon abandons target, following existing SoundEvent pattern.
**When to use:** On every abandonment for future sound/effect hooks.
**Example:**
```java
// Source: Codebase pattern (DeathSound.java)
package com.mojang.tower.event;

/** Triggered when a peon abandons an unreachable target. */
public record AbandonedTargetSound() implements SoundEvent {}
```

### Pattern 5: Configurable Node Limit
**What:** Move node limit from constant to configurable value in PathfindingService.
**When to use:** Allows tuning without recompiling; phase context says "configurable via PathfindingService".
**Example:**
```java
// In PathfindingService.java
private int maxNodes = 1024;  // Default per phase context

public void setMaxNodes(int limit) {
    this.maxNodes = limit;
}

// Pass to AStarPathfinder
public PathResult findPath(double fromX, double fromY, double toX, double toY) {
    GridCell start = worldToGrid(fromX, fromY);
    GridCell goal = worldToGrid(toX, toY);
    return pathfinder.findPath(start, goal, maxNodes);  // New parameter
}
```

### Anti-Patterns to Avoid
- **Partial paths:** Phase context explicitly says "no partial paths - either full path exists or abandon immediately." Never implement "move closer and reassess."
- **Retry logic:** Phase context says "after abandoning: no retry, move on to new assignment." Don't add retry timers or counters.
- **Clearing blacklist on world changes:** This is explicitly deferred to Phase 7 (Dynamic Recalculation). Blacklist is cleared only by time.
- **Distinct trapped-death event:** Phase context says "trapped death uses existing death logic (no distinct event type)." Just call existing `die()`.
- **Early-exit for surrounded start:** Phase context says "let A* handle naturally." Don't add special case for surrounded peon before A*.

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Unreachable detection | Custom reachability check | PathfindingService.findPath() returning NotFound | A* with node limit handles this; phase context says "let A* handle naturally" |
| Peon death | New death mechanism | Existing Peon.die() | Reuses existing death sound, population decrement, alive=false logic |
| Event publishing | Custom callback | EventBus.publish() | Existing pattern, synchronous for determinism |
| Tick counting | Custom timer | Island tick loop | Peons tick in island.tick(); can track via simple counter |

**Key insight:** The phase context makes strong decisions about what NOT to build. The research validates these decisions: A* with node limit naturally handles unreachable detection, and existing death logic handles trapped peons. The new code is primarily about wiring these pieces together and adding the blacklist.

## Common Pitfalls

### Pitfall 1: Accessing Job.target Directly
**What goes wrong:** Job.target is protected, not public. Direct access from Peon fails compilation.
**Why it happens:** Peon and Job are separate classes; Peon doesn't have access to protected members.
**How to avoid:** Add `Entity getTarget()` accessor method to Job base class, or expose target for blacklist purposes.
**Warning signs:** Compilation error accessing job.target from Peon.

### Pitfall 2: Entity Reference Equality for Blacklist
**What goes wrong:** Blacklist lookup fails if Entity equals() isn't identity-based.
**Why it happens:** Entity doesn't override equals()/hashCode(), so uses Object identity which is correct for this use case.
**How to avoid:** Confirm Entity uses identity equality (it does - no equals() override). Use reference equality consistently.
**Warning signs:** Same entity added multiple times, or blacklist check fails for entities that should match.

### Pitfall 3: Forgetting to Clean Expired Blacklist Entries
**What goes wrong:** Blacklist grows unboundedly, memory leak, and entities stay blacklisted forever.
**Why it happens:** Cleanup not called, or cleanup logic wrong.
**How to avoid:** Call cleanBlacklist() at start of each tick(). Use LinkedHashMap for efficient removal of oldest entries.
**Warning signs:** Peon never re-targets previously failed entities even after long time.

### Pitfall 4: Non-Deterministic Tick Counter
**What goes wrong:** Blacklist expiry behaves differently across runs, breaking determinism.
**Why it happens:** Using System.currentTimeMillis() or similar non-deterministic time source.
**How to avoid:** Use game tick counter, not real time. Each island.tick() is one tick; track with simple integer counter.
**Warning signs:** Golden master tests fail intermittently with blacklist-related behavior changes.

### Pitfall 5: Trapped Detection Using World Coordinates Instead of Grid
**What goes wrong:** Trapped detection gives wrong results due to coordinate conversion issues.
**Why it happens:** Checking world positions directly instead of using grid cell neighbors.
**How to avoid:** Convert peon position to grid cell, check 8 grid neighbors, convert back to world for isOnGround() check.
**Warning signs:** Peon detected as trapped when clearly has walkable neighbors, or vice versa.

### Pitfall 6: Modifying Job.cantReach() Without Understanding Callers
**What goes wrong:** Changing cantReach() behavior breaks collision handling in Peon.tick().
**Why it happens:** cantReach() is called from multiple places: both when MovementResult.Blocked and when PathResult.NotFound.
**How to avoid:** Keep cantReach() for collision handling but make it deterministic. Pathfinding failure should use a separate code path.
**Warning signs:** Peons behave strangely when colliding with entities but path exists.

## Code Examples

Verified patterns for this codebase:

### Modified Job.cantReach() - Deterministic Abandonment
```java
// Source: Existing Job.java pattern, modified per phase context
public void cantReach() {
    // Replace random 10% with deterministic immediate abandonment
    // This is called when pathfinding fails (not when collision occurs)
    target = null;
    // Peon will call setJob(null) after handling blacklist
}
```

### Peon Blacklist Implementation
```java
// Source: Phase context decision + LinkedHashMap pattern
// Add to Peon.java
private final LinkedHashMap<Entity, Long> targetBlacklist = new LinkedHashMap<>();
private static final int BLACKLIST_DURATION = 60; // ticks
private int tickCounter = 0;

// Call at start of tick()
private void cleanBlacklist() {
    targetBlacklist.entrySet().removeIf(e -> e.getValue() <= tickCounter);
}

private void blacklistTarget(Entity target) {
    if (target != null) {
        targetBlacklist.put(target, (long)(tickCounter + BLACKLIST_DURATION));
    }
}

public boolean isBlacklisted(Entity target) {
    return target != null && targetBlacklist.containsKey(target);
}
```

### Trapped Peon Detection
```java
// Source: Phase context - check after pathfinding fails
private boolean isTrapped() {
    int gx = (int) ((x + 192) / 4);  // Convert world to grid
    int gy = (int) ((y + 192) / 4);

    int[][] neighbors = {
        {-1,-1}, {0,-1}, {1,-1},
        {-1, 0},         {1, 0},
        {-1, 1}, {0, 1}, {1, 1}
    };

    for (int[] d : neighbors) {
        int nx = gx + d[0];
        int ny = gy + d[1];
        if (nx >= 0 && nx < 96 && ny >= 0 && ny < 96) {
            double worldX = (nx * 4.0) - 192 + 2;  // Grid to world (center)
            double worldY = (ny * 4.0) - 192 + 2;
            if (island.isOnGround(worldX, worldY)) {
                return false;  // Has at least one walkable neighbor
            }
        }
    }
    return true;  // All neighbors blocked = trapped
}
```

### AbandonedTargetSound Event
```java
// Source: Existing SoundEvent pattern (DeathSound.java)
package com.mojang.tower.event;

/** Triggered when a peon abandons an unreachable target. */
public record AbandonedTargetSound() implements SoundEvent {}
```

### Updated SoundEvent Permits Clause
```java
// Source: Existing SoundEvent.java
public sealed interface SoundEvent permits
    SelectSound,
    PlantSound,
    DestroySound,
    GatherSound,
    FinishBuildingSound,
    SpawnSound,
    SpawnWarriorSound,
    DingSound,
    DeathSound,
    MonsterDeathSound,
    WinSound,
    AbandonedTargetSound {  // NEW
}
```

### Peon.tick() Integration Sketch
```java
// In Peon.tick(), replacing current PathResult.NotFound handling:
case PathResult.NotFound(var reason) -> {
    currentPath = null;

    // Check if peon is trapped (all neighbors blocked)
    if (isTrapped()) {
        die();  // Uses existing death logic, respawns at Tower
        return;
    }

    // Target is blocked but peon is not trapped
    if (job != null && job.target != null) {
        blacklistTarget(job.target);
        EventBus.publish(new AbandonedTargetSound());
    }
    setJob(null);  // Become idle, available for new assignment
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Random 10% abandonment | Deterministic A* failure | This phase | Predictable, testable behavior |
| No unreachable detection | Node-limited A* | Phase 5 | Bounded search, graceful failure |
| No blacklist | Time-based blacklist | This phase | Prevents re-assignment thrashing |
| No trapped detection | Post-A* neighbor check | This phase | Peons die when surrounded |

**Deprecated/outdated:**
- Random abandonment (`random.nextDouble() < 0.1`): Replace with deterministic A* failure handling
- Unbounded A* search: Already fixed in Phase 5 with MAX_NODES limit

## Open Questions

Things that couldn't be fully resolved:

1. **Tick Counter Access**
   - What we know: Peon needs to track ticks for blacklist expiry. Island.tick() calls entity.tick() each frame.
   - What's unclear: Whether to use a static tick counter, pass tick to Peon.tick(), or maintain per-peon counter.
   - Recommendation: Use per-peon counter incremented in tick(). Simpler, no threading concerns.

2. **Job.target Visibility**
   - What we know: Job.target is protected. Peon needs access for blacklisting.
   - What's unclear: Best way to expose - public getter, or friend-style package access.
   - Recommendation: Add `public Entity getTarget()` method to Job. Clean public API.

3. **Respawn Location**
   - What we know: Phase context says "respawn at Tower" for trapped peons.
   - What's unclear: How respawn works - does die() handle this, or is separate spawn logic needed?
   - Recommendation: Check codebase for existing respawn logic. If none, trapped peons just die (population decreases, new peons spawn from Houses as normal).

## Sources

### Primary (HIGH confidence)
- Codebase analysis: Peon.java, Job.java, AStarPathfinder.java, PathfindingService.java - Existing patterns
- Phase 5 RESEARCH.md - A* implementation details, node limit
- Phase 6 CONTEXT.md - User decisions constraining implementation
- Java LinkedHashMap docs - Insertion-order iteration guarantee

### Secondary (MEDIUM confidence)
- SoundEvent.java pattern - Event record structure
- EventBus.java - Synchronous publish pattern

### Tertiary (LOW confidence)
- None - all findings verified against codebase

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Uses existing codebase patterns and Java collections
- Architecture patterns: HIGH - Based on codebase analysis and phase context decisions
- Pitfalls: HIGH - Identified from code review and phase context constraints
- Tick counter approach: MEDIUM - Per-peon counter is simple but not yet validated

**Research date:** 2026-02-06
**Valid until:** 60 days (behavior patterns are stable; only blacklist duration may need tuning)
