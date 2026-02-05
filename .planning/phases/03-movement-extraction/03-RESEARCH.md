# Phase 3: Movement Extraction - Research

**Researched:** 2026-02-05
**Domain:** Movement system extraction, behavior/movement decoupling for Java game
**Confidence:** HIGH

## Summary

This phase extracts movement execution from entity behavior logic in the Breaking the Tower codebase. Currently, movement is tightly coupled with behavior in both `Peon.tick()` (lines 114-163) and `Monster.tick()` (lines 64-97), making it impossible to swap in pathfinding without rewriting behavior code.

The codebase does NOT require full ECS architecture. Instead, a **lightweight MovementSystem service** that follows the existing patterns (ServiceLocator, EventBus) provides the minimal decoupling needed. The key insight is that movement consists of: (1) calculating a target direction, (2) computing velocity, (3) checking collision, and (4) updating position. Steps 3-4 should live in MovementSystem; steps 1-2 remain in entity/job behavior.

The recommended approach uses a MovementRequest record pattern where Jobs request movement through MovementSystem rather than directly modifying entity x/y coordinates. This creates a single integration point for future pathfinding without disrupting the existing behavior logic.

**Primary recommendation:** Extract movement execution (collision check + position update) into MovementSystem while keeping movement intent (direction, speed) in entity behavior. Use MovementRequest records to decouple the two.

## Standard Stack

The established libraries/tools for this domain:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Java 21 records | 21 LTS | MovementRequest, MovementResult data | Built-in, immutable, value semantics |
| ServiceLocator (existing) | - | MovementSystem registration | Already established in Phase 2 |
| Java sealed interface | 21 LTS | Exhaustive movement result handling | Compile-time safety |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Vec record (existing) | - | Position/direction representation | Already refactored in Phase 1 |
| Island.isFree() (existing) | - | Collision detection | Existing API, don't duplicate |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| MovementSystem service | Full ECS | ECS overkill for 2 moving entity types |
| Service via ServiceLocator | EventBus MovementEvent | Events add latency; movement needs immediate result |
| MovementRequest record | Direct method params | Records provide clear API contract |

**Installation:**
```bash
# No installation needed - all Java 21 built-in features + existing infrastructure
```

## Architecture Patterns

### Recommended Project Structure
```
src/main/java/com/mojang/tower/
├── movement/                    # NEW: Movement system
│   ├── MovementSystem.java           # Movement execution service
│   ├── MovementRequest.java          # Request record
│   └── MovementResult.java           # Result sealed interface
├── service/
│   └── ServiceLocator.java           # Add movement() accessor
└── [existing entity files]           # Modify to use MovementSystem
```

### Pattern 1: MovementSystem as Service
**What:** Single class responsible for all movement execution. Entities request movement; system executes and returns result.
**When to use:** When multiple entity types share movement mechanics (collision, position update) but have different movement intents.
**Example:**
```java
// Source: Adaptation of Service pattern from Phase 2
public final class MovementSystem {
    private Island island;

    public void setIsland(Island island) {
        this.island = island;
    }

    /**
     * Execute a movement request with collision detection.
     * Returns the actual position after movement (may differ from target if blocked).
     */
    public MovementResult move(MovementRequest request) {
        Entity entity = request.entity();
        double targetX = request.targetX();
        double targetY = request.targetY();

        if (island.isFree(targetX, targetY, entity.r, entity)) {
            entity.x = targetX;
            entity.y = targetY;
            return new MovementResult.Moved(targetX, targetY);
        } else {
            Entity blocker = island.getEntityAt(targetX, targetY, entity.r, null, entity);
            return new MovementResult.Blocked(blocker);
        }
    }
}
```

### Pattern 2: MovementRequest Record
**What:** Immutable data object capturing movement intent. Contains entity reference, target position, and optional metadata.
**When to use:** When movement needs to be requested from behavior code but executed by movement system.
**Example:**
```java
// Source: Java records pattern from Phase 1-2
public record MovementRequest(
    Entity entity,
    double targetX,
    double targetY
) {
    /**
     * Factory: create request from entity moving in direction at speed.
     */
    public static MovementRequest fromDirection(Entity entity, double direction, double speed) {
        double targetX = entity.x + Math.cos(direction) * speed;
        double targetY = entity.y + Math.sin(direction) * speed;
        return new MovementRequest(entity, targetX, targetY);
    }
}
```

### Pattern 3: MovementResult Sealed Interface
**What:** Sealed interface with two permitted results: Moved (success) and Blocked (collision). Enables exhaustive switch handling.
**When to use:** When movement can succeed or fail, and caller needs to react differently to each outcome.
**Example:**
```java
// Source: Sealed interface pattern from Phase 2 (GameState, SoundEvent)
public sealed interface MovementResult permits MovementResult.Moved, MovementResult.Blocked {
    record Moved(double x, double y) implements MovementResult {}
    record Blocked(Entity blocker) implements MovementResult {
        /**
         * blocker may be null if blocked by terrain/boundary
         */
    }
}
```

### Pattern 4: Incremental Extraction via Delegation
**What:** Extract movement in stages: first wrap existing logic in MovementSystem call, then gradually simplify entity code.
**When to use:** When refactoring risky code (like Peon.tick()) and golden master must pass at each step.
**Process:**
1. Create MovementSystem with method matching current inline logic
2. Replace inline movement with MovementSystem call (behavior preserved)
3. Verify golden master passes
4. Simplify/consolidate movement logic in MovementSystem
5. Verify golden master passes again

### Anti-Patterns to Avoid
- **Over-extraction:** Don't extract rotation calculation, wander timing, or speed modifiers to MovementSystem. Keep movement *intent* in entity; only movement *execution* in system.
- **Breaking determinism:** MovementSystem must not introduce new Random calls or reorder operations. Golden master tests catch this.
- **Full ECS rewrite:** This codebase has 2 moving entity types. ECS adds complexity without benefit.
- **Event-based movement:** Using EventBus for movement requests adds latency and breaks the immediate collision-response pattern.

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Collision detection | Custom spatial check | Island.isFree() | Already handles entity exclusion, ground check |
| Position storage | MovementSystem fields | Entity.x/y | Single source of truth in entity |
| Service access | Singleton | ServiceLocator | Testable, swappable (established pattern) |
| Result handling | Exception throwing | Sealed MovementResult | Type-safe, exhaustive handling |

**Key insight:** The existing Island class already provides the collision API (isFree, getEntityAt). MovementSystem is a thin orchestration layer, NOT a replacement for Island's spatial logic.

## Common Pitfalls

### Pitfall 1: Extracting Too Much (50-Line Rule Violation)
**What goes wrong:** Attempting to extract all of Peon.tick() into MovementSystem, breaking behavior logic.
**Why it happens:** Movement and behavior are interleaved in the original code. It's tempting to "clean it all up."
**How to avoid:** Only extract position update (x=, y=) and collision check (isFree, getEntityAt). Leave rotation, speed calculation, wanderTime in entity.
**Warning signs:** Entity.tick() becomes a single MovementSystem call. This means you extracted too much.

### Pitfall 2: Breaking Movement-Collision-Reaction Loop
**What goes wrong:** Separating the collision check from the reaction (rotation change, job.cantReach()) breaks behavior.
**Why it happens:** In current code, collision handling is inline with position update.
**How to avoid:** MovementResult.Blocked provides blocker reference. Entity code handles reaction based on result.
**Warning signs:** Peons get stuck, monsters stop hunting after collision.

### Pitfall 3: Changing Operation Order
**What goes wrong:** Golden master fails because position update happens at different point in tick.
**Why it happens:** Extracting to system might change when position update occurs relative to other tick logic.
**How to avoid:** Call MovementSystem.move() at EXACT same point where inline position update occurred.
**Warning signs:** Golden master diverges early (tick < 100) with position differences.

### Pitfall 4: Speed/Direction Calculation in MovementSystem
**What goes wrong:** Different entities have different speed modifiers (Peon has level bonus, wander penalty). Putting this in MovementSystem creates entity-specific branches.
**Why it happens:** Movement "feels like" it includes speed calculation.
**How to avoid:** Entity calculates target position (direction * speed). MovementSystem just executes the move.
**Warning signs:** MovementSystem has `if (entity instanceof Peon)` checks.

### Pitfall 5: Forgetting MovementSystem Needs Island Reference
**What goes wrong:** NullPointerException when MovementSystem.move() calls island.isFree().
**Why it happens:** MovementSystem created before Island, or Island not passed to system.
**How to avoid:** Initialize MovementSystem in TowerComponent.init() after Island creation. Use ServiceLocator pattern.
**Warning signs:** NPE on first game tick.

### Pitfall 6: Not Handling Both Collision Cases
**What goes wrong:** Monster.tick() handles collision differently than Peon.tick() (different rotation change, no job callbacks).
**Why it happens:** Assuming one-size-fits-all collision handling.
**How to avoid:** MovementResult provides information; entity handles its own collision response.
**Warning signs:** Monster behavior changes after extraction.

## Code Examples

Verified patterns based on codebase analysis:

### Current Movement Logic (Before)
```java
// Peon.tick() lines 137-161 - movement interleaved with collision handling
double xt = x + Math.cos(rot) * 0.4 * speed;
double yt = y + Math.sin(rot) * 0.4 * speed;
if (island.isFree(xt, yt, r, this))
{
    x = xt;
    y = yt;
}
else
{
    if (job != null)
    {
        Entity collided = island.getEntityAt(xt, yt, r, null, this);
        if (collided != null)
        {
            job.collide(collided);
        }
        else
        {
            job.cantReach();
        }
    }
    rot = (random.nextDouble())*Math.PI*2;
    wanderTime = random.nextInt(30)+3;
}
```

### Extracted Movement Logic (After)
```java
// In Peon.tick() - same location, same behavior
double targetX = x + Math.cos(rot) * 0.4 * speed;
double targetY = y + Math.sin(rot) * 0.4 * speed;
MovementResult result = ServiceLocator.movement().move(
    new MovementRequest(this, targetX, targetY)
);
switch (result) {
    case MovementResult.Moved(var newX, var newY) -> {
        // Position already updated by MovementSystem
    }
    case MovementResult.Blocked(var blocker) -> {
        if (job != null) {
            if (blocker != null) {
                job.collide(blocker);
            } else {
                job.cantReach();
            }
        }
        rot = random.nextDouble() * Math.PI * 2;
        wanderTime = random.nextInt(30) + 3;
    }
}
```

### Monster Movement (Simpler Case)
```java
// Monster.tick() - similar pattern, different collision response
double targetX = x + Math.cos(rot) * 0.3 * speed;
double targetY = y + Math.sin(rot) * 0.3 * speed;
MovementResult result = ServiceLocator.movement().move(
    new MovementRequest(this, targetX, targetY)
);
if (result instanceof MovementResult.Blocked) {
    rot += random.nextInt(2) * 2 - 1 * Math.PI / 2 + (random.nextDouble() - 0.5);
    wanderTime = random.nextInt(30);
}
```

### MovementSystem Integration
```java
// In TowerComponent.init() - after Island creation
public void init() {
    // ... existing initialization ...
    island = new Island(this, bitmaps.island);

    // Initialize MovementSystem
    MovementSystem movementSystem = new MovementSystem();
    movementSystem.setIsland(island);
    ServiceLocator.provide(movementSystem);
}
```

### ServiceLocator Extension
```java
// In ServiceLocator.java - add movement service
public final class ServiceLocator {
    private static AudioService audioService;
    private static MovementSystem movementSystem;  // NEW

    public static void provide(MovementSystem service) {
        movementSystem = service;
    }

    public static MovementSystem movement() {
        if (movementSystem == null) {
            throw new IllegalStateException("MovementSystem not initialized");
        }
        return movementSystem;
    }

    public static void reset() {
        audioService = null;
        movementSystem = null;  // NEW
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Movement inline in tick() | Movement via system service | ECS popularization (2010s) | Decouples behavior from execution |
| Direct position mutation | Request/result pattern | Modern gamedev practice | Enables pathfinding integration |
| Inheritance for movement variants | Composition via systems | "Composition over inheritance" | Avoids diamond hierarchy |
| Immediate collision response | Deferred via result type | Sealed interfaces (Java 17+) | Type-safe handling |

**Deprecated/outdated:**
- Full ECS for small games: Adds complexity without benefit for < 5 entity types
- Movement events via message bus: Too slow for frame-by-frame movement

## Open Questions

Things that couldn't be fully resolved:

1. **Job movement requests**
   - What we know: Jobs currently set xTarget/yTarget, Peon reads them in tick()
   - What's unclear: Should Jobs call MovementSystem directly, or keep the current indirection?
   - Recommendation: Keep current pattern (Jobs set target, Peon executes). Phase 3 focuses on execution extraction, not request flow.

2. **moveTick animation counter**
   - What we know: moveTick += speed is used for animation frame calculation
   - What's unclear: Should MovementSystem update this, or keep in entity?
   - Recommendation: Keep in entity - it's rendering state, not movement state.

3. **Future pathfinding integration point**
   - What we know: MovementSystem will be the pathfinding hook
   - What's unclear: Whether pathfinding replaces move() or wraps it
   - Recommendation: Design MovementSystem.move() as low-level; pathfinding will call it with calculated waypoints.

## Sources

### Primary (HIGH confidence)
- [Game Programming Patterns - Update Method](https://gameprogrammingpatterns.com/update-method.html) - Pattern for entity tick separation
- [Game Programming Patterns - Component](https://gameprogrammingpatterns.com/component.html) - When to extract functionality
- Codebase analysis: Peon.java, Monster.java, Island.java - Current movement implementation

### Secondary (MEDIUM confidence)
- [Entity-Component-System Design Pattern](https://www.umlboard.com/design-patterns/entity-component-system.html) - System extraction principles
- [Behavioral ECS](https://shawwrites.medium.com/behavioral-ecs-is-my-favorite-new-thing-40be97335e05) - Separating behavior from execution
- [SOLID in Game Development](https://medium.com/@Code_With_K/understanding-the-single-responsibility-principle-srp-a-cornerstone-of-solid-principles-in-game-d28c3d553e58) - SRP for movement extraction

### Tertiary (LOW confidence)
- N/A - All architectural recommendations verified against codebase analysis

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Uses existing patterns (ServiceLocator, records, sealed) from Phase 1-2
- Architecture patterns: HIGH - Minimal extraction approach matched to codebase size
- Pitfalls: HIGH - Derived from detailed Peon.tick() and Monster.tick() analysis

**Research date:** 2026-02-05
**Valid until:** 90 days (architectural patterns, no external dependencies)
