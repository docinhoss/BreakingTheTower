# Technology Stack: Java 21 Modernization

**Project:** Breaking the Tower (Java 1.6 to Java 21 Migration)
**Researched:** 2026-02-05
**Overall Confidence:** HIGH (verified with Oracle docs, OpenJDK JEPs, official sources)

## Executive Summary

This document prioritizes Java 21 features specifically valuable for modernizing "Breaking the Tower" - a Java 1.6 RTS/god game with entity systems, job-based AI, and resource economy. The focus is on features that improve code clarity, enable better architecture for future pathfinding, and replace legacy patterns with modern idioms.

**Key insight:** Not all Java 21 features are equally valuable for this migration. Virtual threads, while headline-worthy, provide minimal benefit for CPU-bound game loops. Records, sealed classes, and pattern matching deliver the highest value for this game's architecture.

---

## Priority 1: High-Value Features for Game Architecture

### 1.1 Records - Replace Immutable Data Classes

**Value for This Project:** HIGH
**Effort:** LOW
**Confidence:** HIGH (Official: [dev.java/learn/records](https://dev.java/learn/records/))

Records eliminate boilerplate for immutable data carriers. The codebase has `Vec` class that is already immutable-by-design - a perfect record candidate.

#### Migration Pattern: Vec Class

**Before (Java 1.6):**
```java
public class Vec {
    public final double x, y, z;

    public Vec(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec(Vec v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
    }

    // 70+ more lines of boilerplate...
    public Vec add(Vec m) {
        return new Vec(x+m.x, y+m.y, z+m.z);
    }

    public Vec scale(double v) {
        return new Vec(x*v, y*v, z*v);
    }
}
```

**After (Java 21):**
```java
public record Vec(double x, double y, double z) {
    public Vec add(Vec m) {
        return new Vec(x + m.x, y + m.y, z + m.z);
    }

    public Vec scale(double v) {
        return new Vec(x * v, y * v, z * v);
    }

    public double distanceSqr(Vec v) {
        double xd = v.x - x;
        double yd = v.y - y;
        double zd = v.z - z;
        return xd*xd + yd*yd + zd*zd;
    }
}
```

**Benefits:**
- Auto-generated `equals()`, `hashCode()`, `toString()`
- Guaranteed immutability
- Compact syntax reduces 76-line class to ~20 lines
- Enables record patterns for deconstruction

#### Additional Record Candidates in Codebase

| Current Pattern | Record Candidate | Rationale |
|----------------|------------------|-----------|
| `Vec` class | `record Vec(double x, double y, double z)` | Already immutable |
| Resource costs in `HouseType` | `record Cost(int wood, int rock, int food)` | Immutable, passed around |
| Position parameters `(double x, double y, double r)` | `record Position(double x, double y, double radius)` | Used in Entity constructor |

---

### 1.2 Sealed Classes - Define Entity Hierarchy

**Value for This Project:** HIGH
**Effort:** MEDIUM
**Confidence:** HIGH (Official: [JEP 409](https://openjdk.org/jeps/409), [softwarepatternslexicon.com](https://softwarepatternslexicon.com/java/modern-java-features-and-their-impact-on-design/records-and-sealed-classes/understanding-sealed-classes/))

Sealed classes enable exhaustive type checking when combined with pattern matching. The game's Entity hierarchy is a textbook use case.

#### Migration Pattern: Entity Hierarchy

**Before (Java 1.6):**
```java
public class Entity implements Comparable<Entity> { ... }
public class Peon extends Entity { ... }
public class Monster extends Entity { ... }
public class House extends Entity { ... }
public class Tree extends Entity { ... }
public class Rock extends Entity { ... }
public class FarmPlot extends Entity { ... }
public class Puff extends Entity { ... }
public class InfoPuff extends Entity { ... }
```

**After (Java 21):**
```java
public sealed abstract class Entity implements Comparable<Entity>
    permits Peon, Monster, House, Tree, Rock, FarmPlot, Puff, InfoPuff {
    // ...
}

public final class Peon extends Entity { ... }
public final class Monster extends Entity { ... }
// etc.
```

**Benefits:**
- Compiler enforces exhaustive switch handling
- Documents valid subtypes explicitly
- Enables exhaustive pattern matching (no default case needed)
- Prevents unauthorized subclasses

#### Entity Category Hierarchy (Recommended)

For cleaner architecture, consider intermediate sealed interfaces:

```java
public sealed interface GameEntity permits MobileEntity, StaticEntity, EffectEntity {}

public sealed interface MobileEntity extends GameEntity permits Peon, Monster {}
public sealed interface StaticEntity extends GameEntity permits House, Tree, Rock, FarmPlot {}
public sealed interface EffectEntity extends GameEntity permits Puff, InfoPuff {}
```

---

### 1.3 Pattern Matching for switch - Replace instanceof Chains

**Value for This Project:** HIGH
**Effort:** LOW-MEDIUM
**Confidence:** HIGH (Official: [JEP 441](https://openjdk.org/jeps/441))

The codebase has multiple `instanceof` checks that become cleaner with pattern matching.

#### Migration Pattern: Target Filtering

**Before (Java 1.6):**
```java
// In House.java line 118-123
TargetFilter noMobFilter = new TargetFilter() {
    public boolean accepts(Entity e) {
        return !(e instanceof Peon || e instanceof Monster);
    }
};

// In Peon.java line 90-93
if (e instanceof Monster) {
    setJob(new Job.Hunt((Monster) e));
}

// In Monster.java line 54-57
if (e instanceof House || e instanceof Peon) {
    target = e;
}
```

**After (Java 21):**
```java
// Pattern matching with switch
public boolean accepts(Entity e) {
    return switch (e) {
        case Peon p -> false;
        case Monster m -> false;
        default -> true;
    };
}

// Pattern matching with instanceof (already in Java 16)
if (e instanceof Monster m) {
    setJob(new Job.Hunt(m));  // No cast needed
}

// With sealed hierarchy - exhaustive matching
var isTarget = switch (e) {
    case House h -> true;
    case Peon p -> true;
    case Monster m -> false;
    case Tree t -> false;
    // ... compiler ensures all cases handled
};
```

#### High-Value Migration Targets in Codebase

| File | Line | Current Pattern | Modernized Pattern |
|------|------|-----------------|-------------------|
| `House.java` | 207-210 | `if (e instanceof Peon)` + cast | `if (e instanceof Peon peon)` |
| `Peon.java` | 90 | `if (e instanceof Monster)` + cast | `if (e instanceof Monster m)` |
| `Monster.java` | 54-57 | `instanceof` chain | `switch` with patterns |
| `HouseType.java` | 59-70 | `if (this == MASON)` chain | `switch (this)` expression |

---

### 1.4 Sealed Classes + Records for Job System

**Value for This Project:** VERY HIGH
**Effort:** MEDIUM
**Confidence:** HIGH

The Job class hierarchy (nested static classes) is an ideal candidate for sealed classes with record-based implementations.

#### Migration Pattern: Job Hierarchy

**Before (Java 1.6):**
```java
public class Job {
    public static class Goto extends Job {
        private Entity target;
        public Goto(Entity target) {
            this.target = target;
            bonusRadius = 15;
        }
        // ...
    }

    public static class Gather extends Job {
        boolean hasResource = false;
        public int resourceId = 0;
        private House returnTo;
        // ...
    }
    // 6 more nested classes...
}
```

**After (Java 21):**
```java
public sealed interface Job permits
    Job.Goto, Job.GotoAndConvert, Job.Hunt, Job.Build, Job.Plant, Job.Gather {

    void tick();
    boolean hasTarget();
    void arrived();

    record Goto(Entity target) implements Job {
        public Goto {
            // Compact constructor for validation
        }
        @Override public void arrived() { /* ... */ }
    }

    record Hunt(Monster target) implements Job {
        @Override public void arrived() { /* ... */ }
    }

    // Stateful jobs need sealed classes, not records
    sealed class Gather implements Job {
        private final int resourceId;
        private final House returnTo;
        private boolean hasResource = false;
        // ...
    }
}
```

**Benefits:**
- Type-safe job dispatch with pattern matching
- Clear data vs behavior separation
- Compiler enforces exhaustive handling

---

## Priority 2: Syntax Modernization

### 2.1 Local Variable Type Inference (var)

**Value for This Project:** MEDIUM
**Effort:** LOW
**Confidence:** HIGH (Official: [JEP 286](https://openjdk.org/jeps/286))

Use `var` to reduce redundancy where type is obvious from context.

#### Migration Pattern

**Before:**
```java
TargetFilter peonFilter = new TargetFilter() {
    public boolean accepts(Entity e) {
        return e.isAlive() && (e instanceof Peon);
    }
};
Entity e = getRandomTarget(r, s, peonFilter);
```

**After:**
```java
var peonFilter = new TargetFilter() {
    public boolean accepts(Entity e) {
        return e.isAlive() && e instanceof Peon;
    }
};
var e = getRandomTarget(r, s, peonFilter);
```

#### Best Practices for var

| Use var When | Avoid var When |
|--------------|----------------|
| Type obvious from RHS: `var list = new ArrayList<Entity>()` | Type not obvious: `var result = process()` |
| Long generic types: `var map = new HashMap<String, List<Entity>>()` | Primitive types: prefer `int` over `var` |
| Try-with-resources | Method parameters (not supported) |
| Enhanced for loops: `for (var entity : entities)` | Fields (not supported) |

---

### 2.2 Switch Expressions

**Value for This Project:** MEDIUM
**Effort:** LOW
**Confidence:** HIGH (Official: [JEP 361](https://openjdk.org/jeps/361))

Convert switch statements to expressions for cleaner code.

#### Migration Pattern: HouseType Description

**Before (Java 1.6):**
```java
public String getDescription() {
    if (this == MASON) return "Gathers nearby stones, produces rock";
    if (this == WOODCUTTER) return "Cuts down nearby trees, produces wood";
    if (this == PLANTER) return "Plants new trees that can later be cut down";
    if (this == FARM) return "Plants crops that can later be harvested";
    if (this == WINDMILL) return "Gathers nearby grown crops, produces food";
    if (this == GUARDPOST) return "Peons and warriors generally stay near these";
    if (this == BARRACKS) return "Converts peons into warriors for 5 wood each";
    if (this == RESIDENCE) return "Produces peons for 5 food each";
    return "**unknown**";
}
```

**After (Java 21):**
```java
public String getDescription() {
    return switch (this) {
        case MASON -> "Gathers nearby stones, produces rock";
        case WOODCUTTER -> "Cuts down nearby trees, produces wood";
        case PLANTER -> "Plants new trees that can later be cut down";
        case FARM -> "Plants crops that can later be harvested";
        case WINDMILL -> "Gathers nearby grown crops, produces food";
        case GUARDPOST -> "Peons and warriors generally stay near these";
        case BARRACKS -> "Converts peons into warriors for 5 wood each";
        case RESIDENCE -> "Produces peons for 5 food each";
    };
}
```

---

### 2.3 Text Blocks

**Value for This Project:** LOW (limited multiline strings in game code)
**Effort:** LOW
**Confidence:** HIGH (Official: [JEP 378](https://openjdk.org/jeps/378))

Useful for any debug output, logging, or configuration strings.

```java
// Before
String help = "Building Types:\n" +
              "  MASON - Gathers stones\n" +
              "  WOODCUTTER - Cuts trees\n";

// After
String help = """
    Building Types:
      MASON - Gathers stones
      WOODCUTTER - Cuts trees
    """;
```

---

## Priority 3: Collection and API Modernization

### 3.1 Collection Factory Methods

**Value for This Project:** MEDIUM
**Effort:** LOW
**Confidence:** HIGH (Official: [JEP 269](https://openjdk.org/jeps/269))

Replace verbose collection initialization with factory methods.

```java
// Before
List<HouseType> buildings = new ArrayList<>();
buildings.add(HouseType.MASON);
buildings.add(HouseType.WOODCUTTER);
buildings = Collections.unmodifiableList(buildings);

// After
var buildings = List.of(HouseType.MASON, HouseType.WOODCUTTER);
```

### 3.2 Sequenced Collections (Java 21)

**Value for This Project:** MEDIUM
**Effort:** LOW
**Confidence:** HIGH (Official: [JEP 431](https://openjdk.org/jeps/431))

New interfaces for ordered collections with first/last access.

```java
// New in Java 21
SequencedCollection<Entity> entities = new ArrayList<>();
Entity first = entities.getFirst();
Entity last = entities.getLast();
var reversed = entities.reversed();  // Reversed view
```

---

## Priority 4: Features with Limited Value for This Project

### 4.1 Virtual Threads

**Value for This Project:** LOW
**Effort:** HIGH
**Confidence:** HIGH (Official: [JEP 444](https://openjdk.org/jeps/444), [Oracle Docs](https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html))

**Why NOT to prioritize:**
- Virtual threads excel at I/O-bound workloads
- Game loops are CPU-bound (rendering, physics, AI tick)
- No significant throughput benefit for single-threaded game loop
- Would require architectural changes for minimal gain

**When to consider:** If adding network multiplayer or async asset loading in future.

### 4.2 String Templates (Preview)

**Value for This Project:** SKIP
**Confidence:** HIGH

String templates were a preview feature that has been **removed in JDK 23** pending redesign. Do not adopt.

---

## Deprecated API Replacements

These changes are **mandatory** for Java 21 compatibility.

| Deprecated Pattern | Replacement | Affected Code |
|-------------------|-------------|---------------|
| `new URL(String)` | `URI.create(String).toURL()` | Any URL construction |
| `Thread.stop()` | Cooperative interruption | Not used in codebase |
| `Locale` constructor | `Locale.of()` | Localization (if any) |
| Finalization (`finalize()`) | Try-with-resources, Cleaner | Resource cleanup |

---

## Migration Order Recommendation

Based on value/effort ratio for this game codebase:

### Phase 1: Quick Wins (High Value, Low Effort)
1. **var adoption** - Immediate readability improvement
2. **Switch expressions** - Clean up HouseType, Resources
3. **Pattern matching instanceof** - Remove casts in Peon, Monster, House

### Phase 2: Architecture Improvement (High Value, Medium Effort)
4. **Records for Vec** - Cleanest win, no behavior change
5. **Sealed Entity hierarchy** - Enable exhaustive matching
6. **Job hierarchy modernization** - Sealed interface + records

### Phase 3: Full Modernization
7. **Collection factory methods** - Replace verbose initialization
8. **TargetFilter as functional interface** - Use lambdas
9. **Sequenced collections** - Where iteration order matters

---

## Build Configuration

```xml
<!-- Maven -->
<properties>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
</properties>

<!-- Or Gradle -->
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
```

---

## Sources

**High Confidence (Official Documentation):**
- [Oracle Java 21 Virtual Threads](https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html)
- [JEP 441: Pattern Matching for switch](https://openjdk.org/jeps/441)
- [JEP 395: Records](https://openjdk.org/jeps/395)
- [JEP 409: Sealed Classes](https://openjdk.org/jeps/409)
- [JEP 286: Local Variable Type Inference](https://openjdk.org/jeps/286)
- [JEP 378: Text Blocks](https://openjdk.org/jeps/378)
- [JEP 431: Sequenced Collections](https://openjdk.org/jeps/431)
- [dev.java/learn/records](https://dev.java/learn/records/)
- [OpenRewrite: Migrate to Java 21](https://docs.openrewrite.org/recipes/java/migrate/upgradetojava21)

**Medium Confidence (Verified Community Sources):**
- [Baeldung: Pattern Matching for Switch](https://www.baeldung.com/java-switch-pattern-matching)
- [nipafx: Java 21 Pattern Matching Tutorial](https://nipafx.dev/java-21-pattern-matching/)
- [Software Patterns Lexicon: Sealed Classes](https://softwarepatternslexicon.com/java/modern-java-features-and-their-impact-on-design/records-and-sealed-classes/understanding-sealed-classes/)
