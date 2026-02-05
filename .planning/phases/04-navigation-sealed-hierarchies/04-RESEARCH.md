# Phase 4: Navigation & Sealed Hierarchies - Research

**Researched:** 2026-02-05
**Domain:** Java sealed classes/interfaces, navigation grid abstraction
**Confidence:** HIGH

## Summary

This phase completes the architecture modernization by introducing two related improvements: (1) sealing the Entity hierarchy and Job interface to enable exhaustive pattern matching and prevent unauthorized extensions, and (2) creating a NavigationGrid interface that abstracts world queries for pathfinding integration.

The codebase is well-prepared for sealing. The Entity class has exactly 9 subclasses, all in the same package (`com.mojang.tower`), making sealed hierarchy straightforward. The Job class has 6 nested static subclasses (Goto, GotoAndConvert, Hunt, Build, Plant, Gather), all in the same file. An existing sealed interface (GameState) demonstrates the pattern already established in Phase 2.

For NavigationGrid, the Island class already provides the core methods needed (`isFree`, `isOnGround`, `getEntityAt`). Creating a NavigationGrid interface and having Island implement it creates a clean abstraction that MovementSystem can depend on instead of the concrete Island class.

**Primary recommendation:** Seal Entity with explicit `permits` clause listing all 9 subclasses (each marked `final` or `non-sealed`), seal Job as an interface extracted from the existing class, and create NavigationGrid interface with methods matching Island's current API for walkability/collision queries.

## Standard Stack

The established libraries/tools for this domain:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Java sealed classes | 21 LTS | Restrict Entity/Job hierarchies | Compile-time exhaustiveness, better switch expressions |
| Java interfaces | 21 LTS | NavigationGrid abstraction | Dependency inversion for pathfinding |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Pattern matching (existing) | 21 LTS | Handle sealed types | Already used with MovementResult, GameState |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Sealed Entity class | Keep unsealed | Loses exhaustiveness guarantees; allows accidental extensions |
| NavigationGrid interface | Pass Island directly | Tight coupling; harder to test MovementSystem; blocks pathfinding |
| Job as sealed interface | Keep as class | Interface enables cleaner sealed hierarchy with nested records |

**Installation:**
```bash
# No installation needed - all Java 21 built-in features
```

## Architecture Patterns

### Recommended Project Structure
```
src/main/java/com/mojang/tower/
├── Entity.java                # sealed class permits...
├── [9 entity subclasses]      # each marked final or non-sealed
├── Job.java                   # sealed interface permits...
├── navigation/                # NEW: Navigation abstraction
│   └── NavigationGrid.java         # Interface for world queries
├── movement/
│   └── MovementSystem.java          # Updated to use NavigationGrid
└── Island.java                # implements NavigationGrid
```

### Pattern 1: Sealed Entity Hierarchy
**What:** Seal Entity class with explicit permits listing all 9 subclasses. Each subclass marked `final` (cannot extend further) or `non-sealed` (rare, if extension needed).
**When to use:** When you want exhaustive pattern matching and to prevent new subclasses.
**Example:**
```java
// Source: JEP 409 - Sealed Classes, validated against codebase analysis
public sealed class Entity implements Comparable<Entity>
    permits FarmPlot, House, InfoPuff, Monster, Peon, Puff, Rock, Tower, Tree {
    // Existing implementation unchanged
}

// Each subclass must declare sealing status
public final class Monster extends Entity { /* ... */ }
public final class Peon extends Entity { /* ... */ }
public final class House extends Entity { /* ... */ }
// etc.
```

### Pattern 2: Sealed Job Interface (Extracted from Class)
**What:** Convert Job from class with nested subclasses to a sealed interface. Move shared fields to record parameters or default method state.
**When to use:** When nested classes form a clear type hierarchy suitable for pattern matching.
**Example:**
```java
// Source: Codebase analysis + JEP 409
// BEFORE: Job is a class with nested static subclasses
// AFTER: Job is a sealed interface

public sealed interface Job
    permits Job.Goto, Job.GotoAndConvert, Job.Hunt, Job.Build, Job.Plant, Job.Gather {

    // Common operations defined as interface methods
    void init(Island island, Peon peon);
    void tick();
    boolean isValidTarget(Entity e);
    boolean hasTarget();
    void arrived();
    void cantReach();
    void collide(Entity e);
    int getCarried();
    void setTarget(Entity e);

    // Each implementation is a final record or class
    final class Goto implements Job { /* ... */ }
    final class Hunt implements Job { /* ... */ }
    // etc.
}
```

**Note:** Job refactoring is complex due to shared mutable state (`peon`, `island`, `target`, `boreTime`). Consider keeping Job as a sealed class if interface extraction adds too much complexity. The key requirement (LANG-04) is "sealed with explicit permitted implementations" - this can be achieved with either class or interface.

### Pattern 3: NavigationGrid Interface
**What:** Interface abstracting walkability and collision queries. Island implements it; MovementSystem depends on it.
**When to use:** When multiple systems need world queries and you want to decouple from concrete Island.
**Example:**
```java
// Source: Research synthesis + codebase Island.java analysis
public interface NavigationGrid {
    /**
     * Check if a position is valid ground (within island bounds, on solid terrain).
     */
    boolean isOnGround(double x, double y);

    /**
     * Check if a circular area is free (on ground and no entity collision).
     * @param x center x
     * @param y center y
     * @param radius collision radius
     * @param exclude entity to exclude from collision check (typically the moving entity)
     * @return true if position is free
     */
    boolean isFree(double x, double y, double radius, Entity exclude);

    /**
     * Get entity at position matching filter.
     * @param x center x
     * @param y center y
     * @param radius search radius
     * @param filter entity filter (nullable for all entities)
     * @param exclude entity to exclude from search
     * @return closest matching entity, or null if none
     */
    Entity getEntityAt(double x, double y, double radius, TargetFilter filter, Entity exclude);
}
```

### Pattern 4: Island implements NavigationGrid
**What:** Island class implements the NavigationGrid interface. Existing method signatures already match.
**When to use:** When Island has the methods but lacks the interface abstraction.
**Example:**
```java
// Source: Codebase Island.java
public class Island implements NavigationGrid {
    // Existing methods already match NavigationGrid signatures:
    // - isOnGround(double x, double y): boolean
    // - isFree(double x, double y, double r, Entity source): boolean
    // - getEntityAt(double x, double y, double r, TargetFilter filter, Entity exception): Entity

    // No implementation changes needed - just add "implements NavigationGrid"
}
```

### Pattern 5: MovementSystem Uses NavigationGrid
**What:** Change MovementSystem from depending on Island to depending on NavigationGrid.
**When to use:** For better abstraction and testability.
**Example:**
```java
// Source: Codebase MovementSystem.java + research
public final class MovementSystem {
    private NavigationGrid grid;  // Was: Island island

    public void setNavigationGrid(NavigationGrid grid) {  // Was: setIsland
        this.grid = grid;
    }

    public MovementResult move(MovementRequest request) {
        // Use grid.isFree() and grid.getEntityAt() instead of island.*
        if (grid.isFree(targetX, targetY, entity.r, entity)) {
            // ...
        }
    }
}
```

### Anti-Patterns to Avoid
- **Sealing with missing subclass:** Compiler error if permits list doesn't include all direct subclasses in same package.
- **Using `sealed` without `permits` when subclasses in different files:** When subclasses are in different files (not nested), you MUST use explicit `permits` clause.
- **Non-sealed unless necessary:** Only use `non-sealed` if you genuinely need the subclass to be extensible. Prefer `final`.
- **Breaking Job's shared state:** Job subclasses rely on shared fields (`peon`, `island`, `target`). Extracting to interface requires careful state management.
- **Over-abstracting NavigationGrid:** Keep interface focused on what MovementSystem needs. Don't add methods "for future use."

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Hierarchy restriction | Access modifiers only | sealed/permits | Compile-time enforcement |
| Exhaustive switch | Default cases | Pattern matching on sealed | Compiler ensures all cases handled |
| Collision queries | Custom interface from scratch | Extract from Island's existing API | Already tested, golden master validates |

**Key insight:** NavigationGrid is NOT a new implementation - it's extracting an interface from Island's existing, tested methods. Zero behavior change.

## Common Pitfalls

### Pitfall 1: Missing Subclass in Permits
**What goes wrong:** Compiler error "class X is not allowed to extend sealed class Y"
**Why it happens:** All direct subclasses must be in permits clause; easy to forget one.
**How to avoid:** Use IDE to find all "extends Entity" or "extends Job"; list them all in permits.
**Warning signs:** Compilation fails immediately when adding `sealed`.

### Pitfall 2: Subclass Not Marked final/sealed/non-sealed
**What goes wrong:** Compiler error "sealed, non-sealed or final modifiers expected"
**Why it happens:** Java requires explicit declaration of how each subclass continues the sealing.
**How to avoid:** Add `final` to each permitted subclass (default choice for leaf classes).
**Warning signs:** Compilation error on each subclass after sealing parent.

### Pitfall 3: Subclasses in Different Packages
**What goes wrong:** Compiler error "permitted class must be in same package"
**Why it happens:** Sealed classes require permitted subclasses in same package (or same module for module systems).
**How to avoid:** All 9 Entity subclasses and Job nested classes are already in `com.mojang.tower` - no issue expected.
**Warning signs:** Would only occur if someone moves files; current codebase is safe.

### Pitfall 4: Job Interface Extraction Breaks Shared State
**What goes wrong:** Job implementations can't access shared fields like `peon`, `island`, `target`.
**Why it happens:** Interface can't have instance fields; each implementation would need its own.
**How to avoid:** Two options:
1. Keep Job as a sealed CLASS (simpler, less refactoring)
2. Extract interface but have each implementation hold its own `peon`, `island`, `target` references
**Recommendation:** Keep Job as sealed class for Phase 4; consider interface extraction for v2.
**Warning signs:** Compilation errors about field access; increased code duplication.

### Pitfall 5: MovementSystem null NavigationGrid
**What goes wrong:** NullPointerException when MovementSystem.move() called before setNavigationGrid().
**Why it happens:** Same issue as before with setIsland() - MovementSystem used during Island construction.
**How to avoid:** Keep existing pattern: MovementSystem allows null during construction, checks `if (grid == null)` in move().
**Warning signs:** Already handled in current code; don't remove the null check.

### Pitfall 6: NavigationGrid Exposes Too Much
**What goes wrong:** Interface becomes Island's entire public API, defeating abstraction purpose.
**Why it happens:** Temptation to add all Island methods "while we're at it."
**How to avoid:** Only include methods MovementSystem (and future pathfinding) actually needs:
- `isOnGround(double x, double y)` - terrain check
- `isFree(double x, double y, double r, Entity exclude)` - walkability check
- `getEntityAt(double x, double y, double r, TargetFilter filter, Entity exclude)` - collision query
**Warning signs:** Interface has methods like `addEntity`, `tick`, `placeHouse` - these don't belong in navigation.

## Code Examples

Verified patterns based on codebase analysis:

### Sealed Entity Declaration
```java
// Source: Codebase Entity.java + JEP 409
package com.mojang.tower;

public sealed class Entity implements Comparable<Entity>
    permits FarmPlot, House, InfoPuff, Monster, Peon, Puff, Rock, Tower, Tree {

    // ALL existing code unchanged
    // ...
}
```

### Subclass Final Modifier
```java
// Source: Codebase Monster.java - add final keyword
package com.mojang.tower;

public final class Monster extends Entity {  // Added: final
    // ALL existing code unchanged
}
```

### Sealed Job Class (Simpler Approach)
```java
// Source: Codebase Job.java + JEP 409
package com.mojang.tower;

public sealed class Job
    permits Job.Goto, Job.GotoAndConvert, Job.Hunt, Job.Build, Job.Plant, Job.Gather {

    // Nested classes already defined here - add final to each
    public static final class Goto extends Job { /* existing */ }
    public static final class GotoAndConvert extends Job { /* existing */ }
    public static final class Hunt extends Job { /* existing */ }
    public static final class Build extends Job { /* existing */ }
    public static final class Plant extends Job { /* existing */ }
    public static final class Gather extends Job { /* existing */ }

    // Rest of Job class unchanged
}
```

### NavigationGrid Interface
```java
// Source: Research synthesis from Island.java method signatures
package com.mojang.tower.navigation;

import com.mojang.tower.Entity;
import com.mojang.tower.TargetFilter;

/**
 * Interface for world navigation queries.
 * Abstracts walkability and collision detection for movement and pathfinding systems.
 */
public interface NavigationGrid {
    /**
     * Check if a position is on valid ground (within bounds, on solid terrain).
     * @param x world x coordinate
     * @param y world y coordinate
     * @return true if position is on walkable ground
     */
    boolean isOnGround(double x, double y);

    /**
     * Check if a circular area is free for movement.
     * @param x center x coordinate
     * @param y center y coordinate
     * @param radius collision radius to check
     * @param exclude entity to exclude from collision check (null to check all)
     * @return true if the area is on ground and has no colliding entities
     */
    boolean isFree(double x, double y, double radius, Entity exclude);

    /**
     * Find entity at a position matching an optional filter.
     * @param x center x coordinate
     * @param y center y coordinate
     * @param radius search radius
     * @param filter entity filter (null accepts all entities)
     * @param exclude entity to exclude from search (null to search all)
     * @return closest matching entity, or null if none found
     */
    Entity getEntityAt(double x, double y, double radius, TargetFilter filter, Entity exclude);
}
```

### Island Implements NavigationGrid
```java
// Source: Codebase Island.java
package com.mojang.tower;

import com.mojang.tower.navigation.NavigationGrid;

public class Island implements NavigationGrid {
    // Existing methods already have correct signatures:
    // - public boolean isOnGround(double x, double y)
    // - public boolean isFree(double x, double y, double r, Entity source)
    // - public Entity getEntityAt(double x, double y, double r, TargetFilter filter, Entity exception)

    // No method implementations change - just add "implements NavigationGrid"

    // Overloaded convenience methods (not in interface):
    public boolean isFree(double x, double y, double r) {
        return isFree(x, y, r, null);
    }

    public Entity getEntityAt(double x, double y, double r, TargetFilter filter) {
        return getEntityAt(x, y, r, filter, null);
    }
}
```

### Updated MovementSystem
```java
// Source: Codebase MovementSystem.java
package com.mojang.tower.movement;

import com.mojang.tower.Entity;
import com.mojang.tower.navigation.NavigationGrid;

public final class MovementSystem {
    private NavigationGrid grid;  // Changed from Island

    public void setNavigationGrid(NavigationGrid grid) {  // Changed from setIsland
        this.grid = grid;
    }

    public MovementResult move(MovementRequest request) {
        Entity entity = request.entity();
        double targetX = request.targetX();
        double targetY = request.targetY();

        // During Island construction, grid not yet set.
        if (grid == null) {
            entity.x = targetX;
            entity.y = targetY;
            return new MovementResult.Moved(targetX, targetY);
        }

        if (grid.isFree(targetX, targetY, entity.r, entity)) {
            entity.x = targetX;
            entity.y = targetY;
            return new MovementResult.Moved(targetX, targetY);
        } else {
            Entity blocker = grid.getEntityAt(targetX, targetY, entity.r, null, entity);
            return new MovementResult.Blocked(blocker);
        }
    }
}
```

### TowerComponent Wiring Update
```java
// Source: Codebase TowerComponent.java init() method
private void init() {
    // ... existing code ...

    // Initialize MovementSystem BEFORE Island
    var movementSystem = new MovementSystem();
    ServiceLocator.provide(movementSystem);

    island = new Island(this, bitmaps.island);

    // Now that Island exists, inject it into MovementSystem as NavigationGrid
    movementSystem.setNavigationGrid(island);  // Changed from setIsland(island)
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Open class hierarchies | Sealed hierarchies | Java 17 (finalized) | Exhaustive pattern matching, controlled extension |
| Concrete dependencies | Interface abstractions | Timeless pattern | Testability, flexibility |
| `default` switch cases | Exhaustive sealed switches | Java 21 patterns | Compiler catches missing cases |

**Deprecated/outdated:**
- Reflection to find subclasses: Use `getPermittedSubclasses()` on sealed classes instead
- Marker interfaces for type hierarchies: Sealed types provide stronger guarantees

## Open Questions

Things that couldn't be fully resolved:

1. **Job as sealed class vs sealed interface**
   - What we know: Job has significant shared mutable state (`peon`, `island`, `target`, `boreTime`, `bonusRadius`)
   - What's unclear: Whether the architectural benefit of interface outweighs refactoring cost
   - Recommendation: Keep as sealed CLASS for Phase 4. The requirement is "sealed with explicit permitted implementations" - achievable with either. Interface extraction can be v2 cleanup.

2. **NavigationGrid method granularity**
   - What we know: MovementSystem needs `isFree` and `getEntityAt`; future pathfinding needs `isOnGround`
   - What's unclear: Whether pathfinding will need additional methods (e.g., `getNeighbors` for A*)
   - Recommendation: Start minimal with 3 methods. Pathfinding (v2) can extend interface or use Island directly.

3. **Entity sealed class vs sealed interface**
   - What we know: Entity is a class with fields, not an interface
   - What's unclear: Whether to convert Entity to interface for cleaner hierarchy
   - Recommendation: Keep Entity as sealed CLASS. Converting to interface would require major refactoring since Entity has state (`x`, `y`, `r`, `alive`, etc.) and concrete methods.

## Sources

### Primary (HIGH confidence)
- [JEP 409: Sealed Classes](https://openjdk.org/jeps/409) - Official specification for sealed classes
- [Oracle Java 17 Sealed Classes Documentation](https://docs.oracle.com/en/java/javase/17/language/sealed-classes-and-interfaces.html) - Syntax and rules
- [Baeldung - Sealed Classes and Interfaces in Java](https://www.baeldung.com/java-sealed-classes-interfaces) - Practical examples
- Codebase analysis: Entity.java, Job.java, Island.java, MovementSystem.java - Current implementation

### Secondary (MEDIUM confidence)
- [Game Programming Patterns](http://www-cs-students.stanford.edu/~amitp/gameprog.html) - Navigation grid concepts
- [Java Code Geeks - Deep Dive into Sealed Classes](https://www.javacodegeeks.com/2024/08/a-deep-dive-into-sealed-classes-and-interfaces.html) - Best practices

### Tertiary (LOW confidence)
- N/A - All recommendations verified against codebase or official documentation

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Java 21 sealed classes are well-documented, existing GameState/MovementResult show pattern
- Architecture patterns: HIGH - NavigationGrid is direct interface extraction from Island's existing API
- Pitfalls: HIGH - Based on Java language specification and codebase analysis

**Research date:** 2026-02-05
**Valid until:** 90 days (stable language features, no external dependencies)
