# Phase 1: Foundation & Language Modernization - Research

**Researched:** 2026-02-05
**Domain:** Java 21 migration, golden master testing, language modernization
**Confidence:** HIGH

## Summary

This phase establishes the safety infrastructure (golden master tests) and modernizes the codebase from Java 1.6 to Java 21 syntax. The research covered three key areas: (1) setting up deterministic golden master testing with JUnit 5 and Jackson for JSON snapshots, (2) converting the `Vec` class to a Java record while preserving its behavior methods, and (3) systematically applying pattern matching, switch expressions, and var throughout the codebase.

The codebase is well-suited for modernization: it has a single `Random` instance per class (easily seedable), the `Vec` class is already immutable with final fields, and the instanceof patterns are straightforward type checks that map cleanly to pattern matching. The `Resources` class has a switch statement that converts to a switch expression. The main challenge is ensuring determinism in the golden master by controlling all Random instances.

**Primary recommendation:** Set up golden master tests FIRST with deterministic seeding, verify the game runs identically, then apply syntax modernization file-by-file with compile checks after each change.

## Standard Stack

The established libraries/tools for this domain:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| JUnit 5 (Jupiter) | 5.10.x | Test framework | Modern test API, extensions, parameterized tests |
| Jackson Databind | 2.16.x | JSON serialization | Industry standard, excellent record support |
| Maven | 3.9.x | Build tool | Decided in CONTEXT.md, good Java 21 support |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Jackson Annotations | 2.16.x | JSON customization | If default serialization needs tweaking |
| JUnit Platform | 5.10.x | Test execution | Comes with JUnit 5 |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Jackson | Gson | Gson simpler but less control; Jackson has better record support |
| ApprovalTests.Java | Custom golden master | ApprovalTests adds complexity; simple file compare sufficient for this project |

**Installation (pom.xml dependencies):**
```xml
<properties>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <junit.version>5.10.2</junit.version>
    <jackson.version>2.16.1</jackson.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>${junit.version}</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>${jackson.version}</version>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.12.1</version>
            <configuration>
                <source>21</source>
                <target>21</target>
            </configuration>
        </plugin>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.2.5</version>
        </plugin>
    </plugins>
</build>
```

## Architecture Patterns

### Recommended Project Structure
```
BreakingTheTower/
├── pom.xml
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/mojang/tower/    # Existing source files
│   └── test/
│       ├── java/
│       │   └── com/mojang/tower/
│       │       └── GoldenMasterTest.java
│       └── resources/
│           └── golden/
│               └── full-game-snapshot.json    # Approved snapshot
```

### Pattern 1: Deterministic Golden Master Testing
**What:** Capture complete game state each tick with fixed Random seeds, compare against approved baseline
**When to use:** Before any refactoring to ensure behavior preservation
**Example:**
```java
// Source: Research synthesis from JUnit 5 + Jackson docs
public class GoldenMasterTest {
    private static final long FIXED_SEED = 8844L; // Match Island's seed
    private static final ObjectMapper mapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    void fullGameplayMatchesGoldenMaster() throws Exception {
        // Load or generate expected snapshot
        Path goldenPath = Path.of("src/test/resources/golden/full-game-snapshot.json");

        // Run game with deterministic seed
        List<GameState> actualStates = runDeterministicGame(FIXED_SEED);

        if (Files.exists(goldenPath)) {
            // Compare mode
            List<GameState> expectedStates = loadSnapshot(goldenPath);
            assertStatesMatch(expectedStates, actualStates);
        } else {
            // Initial capture mode
            saveSnapshot(goldenPath, actualStates);
            fail("Golden master created at " + goldenPath + " - review and re-run");
        }
    }
}
```

### Pattern 2: Java Record with Behavior Methods
**What:** Records can have instance methods beyond auto-generated ones; Vec needs its math operations
**When to use:** Converting immutable classes that have behavior (not just data storage)
**Example:**
```java
// Source: Oracle Java 21 Records documentation
public record Vec(double x, double y, double z) {
    // Copy constructor not needed - use new Vec(other.x(), other.y(), other.z())
    // or just pass the record directly (it's immutable)

    public double distance(Vec v) {
        return Math.sqrt(distanceSqr(v));
    }

    public double distanceSqr(Vec v) {
        double xd = v.x() - x;  // Note: accessor methods, not fields
        double yd = v.y() - y;
        double zd = v.z() - z;
        return xd * xd + yd * yd + zd * zd;
    }

    public Vec rotate(double sin, double cos) {
        double _x = x * cos + z * sin;
        double _y = y;
        double _z = x * sin - z * cos;
        return new Vec(_x, _y, _z);
    }

    public Vec add(Vec m) {
        return new Vec(x + m.x(), y + m.y(), z + m.z());
    }

    public Vec scale(double v) {
        return new Vec(x * v, y * v, z * v);
    }

    public Vec sub(Vec m) {
        return new Vec(x - m.x(), y - m.y(), z - m.z());
    }

    // ... other methods
}
```

### Pattern 3: Pattern Matching for instanceof
**What:** Combine type check, cast, and variable declaration in one expression
**When to use:** Replace all `instanceof X` followed by `(X)` cast
**Example:**
```java
// Source: JEP 441 - Pattern Matching for switch (Java 21)
// BEFORE (current codebase pattern in Peon.java:90-92):
if (e instanceof Monster) {
    setJob(new Job.Hunt((Monster) e));
}

// AFTER:
if (e instanceof Monster monster) {
    setJob(new Job.Hunt(monster));
}

// With negation (current pattern in House.java:207-209):
// BEFORE:
if (e instanceof Peon) {
    Peon peon = (Peon) e;
    return peon;
}

// AFTER:
if (e instanceof Peon peon) {
    return peon;
}
```

### Pattern 4: Switch Expressions
**What:** Switch that yields a value, with arrow syntax and exhaustiveness checking
**When to use:** When switch assigns to variable or returns value; judge case-by-case for void switches
**Example:**
```java
// Source: JEP 361 - Switch Expressions
// BEFORE (Resources.java:15-20):
public void add(int resourceId, int count) {
    switch(resourceId) {
        case WOOD: wood += count; break;
        case ROCK: rock += count; break;
        case FOOD: food += count; break;
    }
}

// AFTER - Arrow syntax for clarity but still void:
public void add(int resourceId, int count) {
    switch (resourceId) {
        case WOOD -> wood += count;
        case ROCK -> rock += count;
        case FOOD -> food += count;
        default -> {} // or throw for invalid resourceId
    }
}
```

### Anti-Patterns to Avoid
- **Converting mutable classes to records:** Records are shallowly immutable; don't convert classes that need setters
- **Breaking determinism:** Don't add `new Random()` calls without seeding; don't use `System.currentTimeMillis()`
- **Overusing var:** Don't use `var` when type is non-obvious (e.g., `var result = someMethod()`)
- **Pattern matching with side effects in conditions:** Keep pattern variable scope clear

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| JSON serialization | Manual string building | Jackson ObjectMapper | Handles escaping, nested objects, records automatically |
| File comparison | Character-by-character | String.equals() on file contents | Simpler, or use diff tools for debugging |
| Test framework | Custom assertions | JUnit 5 Assertions | Standard, good error messages |
| Object equality for snapshots | Field-by-field compare | Record auto-equals or Jackson tree compare | Records have correct equals; Jackson can compare JSON trees |

**Key insight:** The golden master approach is simple - serialize state to JSON, compare strings. Don't add complexity with custom diff algorithms or partial matching.

## Common Pitfalls

### Pitfall 1: Non-Deterministic Random
**What goes wrong:** Golden master tests fail randomly because Random instances aren't seeded consistently
**Why it happens:** Each class creates `new Random()` without a seed; Sound classes have their own Random
**How to avoid:**
1. Identify ALL Random instances in codebase (Island, Entity, Monster, Peon, Job, Sound subclasses)
2. Either inject seeds or use a shared seeded Random
3. The Island already uses seed 8844 - verify all other Randoms either use this or are explicitly seeded
**Warning signs:** Tests pass sometimes, fail others; "flaky" test behavior

### Pitfall 2: Record Accessor Method Names
**What goes wrong:** Code breaks after converting to record because `vec.x` becomes `vec.x()`
**Why it happens:** Records generate accessor METHODS `x()`, not public fields `x`
**How to avoid:** After converting Vec to record, find-and-replace all `vec.x` with `vec.x()` throughout codebase
**Warning signs:** Compilation errors about "cannot find symbol"

### Pitfall 3: Record Copy Constructor
**What goes wrong:** `new Vec(existingVec)` no longer compiles after conversion
**Why it happens:** The original Vec has a copy constructor `Vec(Vec v)`; records don't auto-generate this
**How to avoid:** Either add explicit copy constructor to record, or replace usages with direct field access
**Warning signs:** Compilation error "no suitable constructor found"

### Pitfall 4: Pattern Variable Scope
**What goes wrong:** Pattern variable not accessible where expected
**Why it happens:** Pattern variables are only in scope when the pattern definitely matched
**How to avoid:**
```java
// WRONG - monster not in scope for else branch
if (!(e instanceof Monster monster)) {
    return;
}
// monster IS in scope here due to early return

// Be careful with || conditions
if (e instanceof Monster monster || e instanceof Peon) // monster may not be bound
```
**Warning signs:** Compilation errors about variable scope

### Pitfall 5: Incomplete Golden Master State
**What goes wrong:** Refactoring breaks behavior but tests pass because not enough state captured
**Why it happens:** Only capturing positions, missing RNG state, job state, health, etc.
**How to avoid:** Capture ALL gameplay-relevant state per tick:
- Entity positions (x, y)
- Entity health/stamina
- Job state (type, target, carrying)
- RNG state (or just seed + tick count)
- Resources (wood, rock, food)
- Population counts
**Warning signs:** Game plays differently but tests still pass

### Pitfall 6: Switch Expression Exhaustiveness
**What goes wrong:** Compiler requires default case or exhaustive cases
**Why it happens:** Switch expressions must be exhaustive; old switch statements didn't require this
**How to avoid:** Add default case or ensure all possible values covered
**Warning signs:** Compilation error "switch expression does not cover all possible input values"

## Code Examples

Verified patterns from official sources:

### GameState Snapshot Record
```java
// Source: Research synthesis - captures state for golden master
public record GameState(
    int tick,
    List<EntityState> entities,
    ResourceState resources,
    int population,
    int populationCap,
    int monsterPopulation,
    int warriorPopulation,
    int warriorPopulationCap,
    boolean titleScreen,
    boolean won
) {}

public record EntityState(
    String type,          // Class simple name
    double x,
    double y,
    double r,
    boolean alive,
    // Entity-specific state
    Integer hp,           // nullable for entities without HP
    Integer jobType,      // nullable, enum ordinal or null
    Integer carrying      // nullable
) {}

public record ResourceState(int wood, int rock, int food) {}
```

### Loading Test Resources
```java
// Source: Baeldung - Maven test resources
Path goldenPath = Path.of("src/test/resources/golden/full-game-snapshot.json");
String json = Files.readString(goldenPath);
// Or via classloader:
InputStream is = getClass().getResourceAsStream("/golden/full-game-snapshot.json");
```

### Jackson ObjectMapper Configuration
```java
// Source: Jackson documentation
ObjectMapper mapper = new ObjectMapper()
    .enable(SerializationFeature.INDENT_OUTPUT)      // Pretty print for diffing
    .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

// Write to file
mapper.writeValue(new File("snapshot.json"), gameStates);

// Read from file
List<GameState> states = mapper.readValue(
    new File("snapshot.json"),
    new TypeReference<List<GameState>>() {}
);
```

### var Usage Examples (Conservative)
```java
// Source: OpenJDK LVTI Style Guide
// GOOD - type obvious from constructor
var monster = new Monster(x, y);
var peon = new Peon(x, y, 0);
var mapper = new ObjectMapper();

// GOOD - type obvious from literal
var count = 0;
var speed = 1.0;

// GOOD - type obvious from factory method name
var path = Path.of("src/test/resources/golden/snapshot.json");
var entities = new ArrayList<Entity>();

// AVOID - type not obvious
var result = island.getEntityAt(x, y, r, filter);  // What type is result?
var e = entities.get(i);                            // Entity, but less clear

// AVOID - numeric literals where precision matters
var stamina = 4096;  // int, but might want long - use explicit type
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `instanceof` + cast | Pattern matching `instanceof` | Java 16 (preview 14) | Eliminates duplicate type references |
| Switch statement | Switch expression | Java 14 (preview 12) | Yields values, no fall-through bugs |
| POJO with getters | Records | Java 16 (preview 14) | 80% less boilerplate for data classes |
| Explicit types everywhere | `var` for locals | Java 10 | Reduces redundancy when type obvious |

**Deprecated/outdated:**
- Raw `instanceof` + cast: Still works but verbose; pattern matching preferred
- Traditional switch with `break`: Still works but switch expressions cleaner for value-returning cases

## Open Questions

Things that couldn't be fully resolved:

1. **Sound class Random instances**
   - What we know: Each Sound subclass has its own `new Random()` without seed
   - What's unclear: Whether sound generation affects game state or is purely audio
   - Recommendation: Sound is side-effect only (audio output), likely doesn't affect game determinism. Verify by checking if any Sound methods influence entity state. If pure audio, can ignore for golden master.

2. **Exact tick count for full game**
   - What we know: Game runs until tower destroyed (win condition)
   - What's unclear: How many ticks a typical playthrough takes with seed 8844
   - Recommendation: Run game headlessly to completion, log tick count. Expect hundreds to thousands of ticks.

3. **Entity ordering in snapshot**
   - What we know: Island.entities is sorted by yr for rendering
   - What's unclear: Whether entity order matters for determinism
   - Recommendation: Sort entities by stable key (type + creation order or ID) in snapshot to avoid false positives from rendering order changes.

## Sources

### Primary (HIGH confidence)
- [Oracle Java 21 Records Documentation](https://docs.oracle.com/en/java/javase/21/language/records.html) - Record syntax, methods, restrictions
- [JEP 441: Pattern Matching for switch](https://openjdk.org/jeps/441) - Pattern matching finalized in Java 21
- [OpenJDK LVTI Style Guide](https://openjdk.org/projects/amber/guides/lvti-style-guide) - Official var usage guidelines G1-G7
- [Apache Maven Compiler Plugin](https://maven.apache.org/plugins/maven-compiler-plugin/) - Java 21 compiler configuration

### Secondary (MEDIUM confidence)
- [Baeldung - Jackson ObjectMapper](https://www.baeldung.com/jackson-object-mapper-tutorial) - JSON serialization patterns
- [Baeldung - JUnit 5 Test Resources](https://www.baeldung.com/junit-src-test-resources-directory-path) - Resource file locations
- [DZone - Golden Master Testing](https://dzone.com/articles/testing-legacy-code-golden) - Golden master approach
- [Codurance - Testing Legacy Code with Golden Master](https://www.codurance.com/publications/2012/11/11/testing-legacy-code-with-golden-master) - Technique overview

### Tertiary (LOW confidence)
- [java-snapshot-testing GitHub](https://github.com/origin-energy/java-snapshot-testing) - Snapshot testing library (not using, but informed approach)
- [ApprovalTests.Java](https://github.com/approvals/ApprovalTests.Java) - Approval testing library (not using, but informed approach)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - JUnit 5, Jackson, Maven are industry standard with official documentation
- Architecture: HIGH - Patterns from official Oracle/OpenJDK documentation
- Pitfalls: HIGH - Based on known language semantics and codebase analysis

**Research date:** 2026-02-05
**Valid until:** 60 days (stable technologies, no rapid changes expected)
