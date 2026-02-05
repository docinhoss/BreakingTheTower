# Phase 2: Decoupling Systems - Research

**Researched:** 2026-02-05
**Domain:** Event-driven architecture, State pattern, Service Locator for Java game
**Confidence:** HIGH

## Summary

This phase decouples the Breaking the Tower codebase by introducing three complementary patterns: State pattern for game state management (title/playing/won), EventBus for sound/effect notifications, and Service Locator to wrap the Sounds singleton.

The codebase currently has **10 direct `Sounds.play()` calls** scattered across 6 files (Island, TowerComponent, House, Peon, Monster, Job) and **2 direct Puff/InfoPuff creations** in House and Peon. Game state is tracked via boolean flags (`titleScreen`, `won`) in TowerComponent with implicit transitions.

The recommended approach uses Java 21 sealed interfaces for the State pattern (leveraging pattern matching from Phase 1), a lightweight synchronous EventBus (no external dependencies), and a simple Service Locator that enables test stubbing without full DI framework complexity.

**Primary recommendation:** Implement all three patterns using only standard Java 21 features - no external libraries required. Keep EventBus synchronous since sound/effect triggers must happen within the game tick.

## Standard Stack

The established libraries/tools for this domain:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Java 21 sealed interfaces | 21 LTS | State pattern with exhaustive switch | Built-in, enables compile-time state checking |
| Java 21 records | 21 LTS | Event types (immutable, value semantics) | Built-in, no boilerplate |
| ConcurrentHashMap | Java 8+ | Listener registry | Thread-safe, no dependencies |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| java.util.function.Consumer | Java 8+ | Listener functional interface | Cleaner than custom interfaces |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Hand-rolled EventBus | Guava EventBus | Adds 3MB dependency, annotation magic |
| Hand-rolled EventBus | greenrobot EventBus | Android-focused, overkill for this |
| Service Locator | Full DI (Guice/Spring) | Massive overhead for small game |
| Sealed interface states | Enum states | Enums can't hold per-state data |

**Installation:**
```bash
# No installation needed - all Java 21 built-in features
```

## Architecture Patterns

### Recommended Project Structure
```
src/main/java/com/mojang/tower/
├── state/               # Game state pattern
│   ├── GameState.java         # Sealed interface
│   ├── TitleState.java        # Concrete state
│   ├── PlayingState.java      # Concrete state
│   └── WonState.java          # Concrete state
├── event/               # EventBus infrastructure
│   ├── GameEvent.java         # Sealed event hierarchy
│   ├── SoundEvent.java        # Sound trigger events
│   ├── EffectEvent.java       # Visual effect events
│   └── EventBus.java          # Publisher/subscriber hub
├── service/             # Service Locator
│   ├── ServiceLocator.java    # Central registry
│   └── AudioService.java      # Interface for Sounds
└── [existing files]     # Modified to use events
```

### Pattern 1: State Pattern with Sealed Interface
**What:** Encapsulate each game state (title/playing/won) as a class implementing a sealed interface. State transitions return new state instances.
**When to use:** When an object behaves differently based on internal state and states are finite.
**Example:**
```java
// Source: Java 21 sealed interface pattern
public sealed interface GameState
    permits TitleState, PlayingState, WonState {

    GameState handleInput(InputEvent event);
    GameState tick();
    void render(Graphics2D g);

    // Entry/exit hooks for state transitions
    default void onEnter() {}
    default void onExit() {}
}

public final class TitleState implements GameState {
    @Override
    public GameState handleInput(InputEvent event) {
        if (event instanceof ClickEvent) {
            return new PlayingState();
        }
        return this;
    }

    @Override
    public GameState tick() {
        // Auto-rotate island
        return this;
    }

    @Override
    public void render(Graphics2D g) {
        // Draw logo, "click to start"
    }
}
```

### Pattern 2: Lightweight Synchronous EventBus
**What:** Central publish/subscribe hub for decoupled communication. Events are processed synchronously within the same tick.
**When to use:** When components need to communicate without direct references.
**Example:**
```java
// Source: Adapted from https://www.laggner.info/posts/lightweight-eventbus-in-java/
public final class EventBus {
    private static final EventBus INSTANCE = new EventBus();
    private final Map<Class<?>, List<Consumer<?>>> listeners = new ConcurrentHashMap<>();

    public static <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
        INSTANCE.listeners
            .computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
            .add(listener);
    }

    public static <T> void unsubscribe(Class<T> eventType, Consumer<T> listener) {
        var list = INSTANCE.listeners.get(eventType);
        if (list != null) list.remove(listener);
    }

    @SuppressWarnings("unchecked")
    public static <T> void publish(T event) {
        var list = INSTANCE.listeners.get(event.getClass());
        if (list != null) {
            for (var listener : list) {
                ((Consumer<T>) listener).accept(event);
            }
        }
    }

    // Test support: clear all listeners
    public static void reset() {
        INSTANCE.listeners.clear();
    }
}
```

### Pattern 3: Service Locator for Testability
**What:** Central registry that provides access to services. Services are registered at startup and can be swapped for testing.
**When to use:** When you need testable singletons without full DI framework.
**Example:**
```java
// Source: Baeldung Service Locator pattern
public interface AudioService {
    void play(Sound sound);
    void setMute(boolean mute);
    boolean isMute();
}

public final class ServiceLocator {
    private static AudioService audioService;

    public static void provide(AudioService service) {
        audioService = service;
    }

    public static AudioService audio() {
        if (audioService == null) {
            throw new IllegalStateException("AudioService not initialized");
        }
        return audioService;
    }

    // Production initialization
    public static void initializeDefaults() {
        provide(new SoundsAdapter(Sounds.instance));
    }
}

// Adapter wraps existing Sounds singleton
public final class SoundsAdapter implements AudioService {
    private final Sounds sounds;

    public SoundsAdapter(Sounds sounds) {
        this.sounds = sounds;
    }

    @Override
    public void play(Sound sound) {
        Sounds.play(sound);
    }

    @Override
    public void setMute(boolean mute) {
        Sounds.setMute(mute);
    }

    @Override
    public boolean isMute() {
        return Sounds.isMute();
    }
}

// Test stub
public final class NullAudioService implements AudioService {
    @Override public void play(Sound sound) {} // Silent
    @Override public void setMute(boolean mute) {}
    @Override public boolean isMute() { return true; }
}
```

### Pattern 4: Typed Event Hierarchy with Records
**What:** Use sealed interface + records for type-safe events with data.
**When to use:** When events carry payloads and you want exhaustive switch matching.
**Example:**
```java
// Sound events
public sealed interface SoundEvent permits
    SelectSound, PlantSound, DestroySound, GatherSound,
    FinishBuildingSound, SpawnSound, SpawnWarriorSound,
    DingSound, DeathSound, MonsterDeathSound, WinSound {
}

public record SelectSound() implements SoundEvent {}
public record PlantSound() implements SoundEvent {}
public record DeathSound(double x, double y) implements SoundEvent {}
// etc.

// Effect events
public sealed interface EffectEvent permits PuffEffect, InfoPuffEffect {
}

public record PuffEffect(double x, double y) implements EffectEvent {}
public record InfoPuffEffect(double x, double y, int image) implements EffectEvent {}

// Handler uses pattern matching
EventBus.subscribe(SoundEvent.class, event -> {
    Sound sound = switch (event) {
        case SelectSound() -> new Sound.Select();
        case PlantSound() -> new Sound.Plant();
        case DeathSound(var x, var y) -> new Sound.Death();
        // ... exhaustive matching guaranteed by sealed interface
    };
    ServiceLocator.audio().play(sound);
});
```

### Anti-Patterns to Avoid
- **God State Manager:** Don't put all state logic in one class. Each GameState subclass should be self-contained.
- **Event Spaghetti:** Don't create events for everything. Only decouple what needs decoupling (sounds, effects).
- **Async EventBus in Game Loop:** Game logic must be deterministic. Use synchronous event dispatch only.
- **Leaking Service Locator:** Don't pass ServiceLocator around. Access statically, configure at startup.

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Thread-safe listener list | ArrayList with synchronized | CopyOnWriteArrayList | Handles concurrent iteration during modification |
| Event type dispatch | instanceof chains | sealed interface + switch expression | Compile-time exhaustiveness checking |
| Singleton replacement | Custom framework | Service Locator with interface | Simple, testable, no magic |
| State data per instance | Static fields | State classes with instance fields | Each state instance owns its data |

**Key insight:** This phase requires NO external libraries. Java 21's sealed interfaces, records, and pattern matching provide all needed infrastructure. Adding dependencies for something this simple would be over-engineering.

## Common Pitfalls

### Pitfall 1: Breaking Determinism with Async Events
**What goes wrong:** Using async event dispatch causes sounds to play at inconsistent times, breaking golden master tests.
**Why it happens:** Developers default to async patterns from web/enterprise contexts.
**How to avoid:** EventBus.publish() must dispatch synchronously within the same call stack.
**Warning signs:** Golden master tests fail non-deterministically; sounds play "late."

### Pitfall 2: State Transition Without Entry/Exit Hooks
**What goes wrong:** State initialization code scattered across callers. Duplicate setup when entering same state from multiple paths.
**Why it happens:** Classic FSM implementations often skip entry/exit actions.
**How to avoid:** Always call `oldState.onExit()` before `newState.onEnter()` in state manager.
**Warning signs:** Duplicated setup code, initialization bugs when entering state via different paths.

### Pitfall 3: Forgetting to Wire EventBus at Startup
**What goes wrong:** Events fire but nothing happens. Silent failures.
**Why it happens:** EventBus listeners must be registered before events occur.
**How to avoid:** Wire all listeners in TowerComponent initialization, before game loop starts.
**Warning signs:** Sounds don't play, puffs don't appear, but no exceptions.

### Pitfall 4: Service Locator Not Reset Between Tests
**What goes wrong:** Test pollution - one test's service stub leaks to another.
**Why it happens:** Static state persists across test runs.
**How to avoid:** Add ServiceLocator.reset() and EventBus.reset() methods; call in @BeforeEach.
**Warning signs:** Tests pass individually but fail when run together.

### Pitfall 5: Creating Sound Objects Before Event Dispatch
**What goes wrong:** Sound objects created in entity code defeat the purpose of decoupling.
**Why it happens:** Trying to pass Sound instance in event instead of event type.
**How to avoid:** Events should be pure data (records). Sound object creation happens in handler.
**Warning signs:** Entity classes still import Sound class.

### Pitfall 6: Over-Decoupling Puff Creation
**What goes wrong:** Puff entities need Island reference for init(). Can't create via event alone.
**Why it happens:** Trying to decouple entity creation without considering initialization.
**How to avoid:** EffectEvent contains location data; handler calls island.addEntity(new Puff(x, y)).
**Warning signs:** NullPointerException in Puff.init() when created via event.

## Code Examples

Verified patterns for this codebase:

### Current Coupling (Before)
```java
// In House.java - direct coupling to Sounds singleton
public void die() {
    Sounds.play(new Sound.Destroy());  // Direct call
    // ...
}

public void puff() {
    island.addEntity(new Puff(x, y));  // Direct entity creation
}
```

### Decoupled Version (After)
```java
// In House.java - fires events, no direct dependencies
public void die() {
    EventBus.publish(new DestroySound());  // Event, not direct call
    // ...
}

public void puff() {
    EventBus.publish(new PuffEffect(x, y));  // Event for effect
}

// In TowerComponent.java initialization - wires handlers
private void initEventHandlers() {
    EventBus.subscribe(SoundEvent.class, this::handleSoundEvent);
    EventBus.subscribe(EffectEvent.class, this::handleEffectEvent);
}

private void handleSoundEvent(SoundEvent event) {
    Sound sound = switch (event) {
        case SelectSound() -> new Sound.Select();
        case PlantSound() -> new Sound.Plant();
        case DestroySound() -> new Sound.Destroy();
        case GatherSound() -> new Sound.Gather();
        case FinishBuildingSound() -> new Sound.FinishBuilding();
        case SpawnSound() -> new Sound.Spawn();
        case SpawnWarriorSound() -> new Sound.SpawnWarrior();
        case DingSound() -> new Sound.Ding();
        case DeathSound() -> new Sound.Death();
        case MonsterDeathSound() -> new Sound.MonsterDeath();
        case WinSound() -> new Sound.WinSound();
    };
    ServiceLocator.audio().play(sound);
}

private void handleEffectEvent(EffectEvent event) {
    switch (event) {
        case PuffEffect(var x, var y) -> island.addEntity(new Puff(x, y));
        case InfoPuffEffect(var x, var y, var img) -> island.addEntity(new InfoPuff(x, y, img));
    }
}
```

### State Pattern Integration
```java
// In TowerComponent.java - replace boolean flags with state object
// BEFORE:
private boolean titleScreen = true, won = false;

// AFTER:
private GameState currentState = new TitleState();

private void tick() {
    // State handles its own logic
    GameState nextState = currentState.tick();
    if (nextState != currentState) {
        currentState.onExit();
        currentState = nextState;
        currentState.onEnter();
    }
}

private void handleClick() {
    GameState nextState = currentState.handleInput(new ClickEvent());
    if (nextState != currentState) {
        currentState.onExit();
        currentState = nextState;
        currentState.onEnter();
    }
}
```

### Test Example with Service Locator
```java
class HouseTest {
    @BeforeEach
    void setup() {
        ServiceLocator.reset();
        EventBus.reset();
        ServiceLocator.provide(new NullAudioService());
    }

    @Test
    void sellPlaysDestroySound() {
        var soundsFired = new ArrayList<SoundEvent>();
        EventBus.subscribe(SoundEvent.class, soundsFired::add);

        House house = createTestHouse();
        house.sell();

        assertThat(soundsFired).contains(new DestroySound());
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Boolean flags for game state | Sealed interface + State pattern | Java 17+ (sealed) | Type-safe state transitions |
| instanceof chains for event dispatch | Pattern matching switch | Java 21 | Exhaustive, compile-time checked |
| Singletons for services | Service Locator + interface | Classic pattern | Testable without mocking frameworks |
| Observer pattern with Object type | Typed Consumer<T> + generics | Java 8 | Type-safe listeners |

**Deprecated/outdated:**
- Raw Observer/Observable (Java 9 deprecated): Use typed EventBus instead
- Guava EventBus @Subscribe annotation: Reflection-based, harder to trace

## Open Questions

Things that couldn't be fully resolved:

1. **Win state sound timing**
   - What we know: `Sound.WinSound` exists but no `Sounds.play()` call found for it
   - What's unclear: When should win sound play? On tower destruction? State entry?
   - Recommendation: Check TowerComponent.win() and Tower.gatherResource() for implicit win trigger; add WinSound event

2. **Effect event handler location**
   - What we know: Handler needs Island reference to call addEntity()
   - What's unclear: Should handler live in TowerComponent (has Island) or Island itself?
   - Recommendation: Put in TowerComponent during init alongside sound handler; Island passed to handler via closure

## Sources

### Primary (HIGH confidence)
- [Game Programming Patterns - State](https://gameprogrammingpatterns.com/state.html) - State pattern for games
- [JEP 409: Sealed Classes](https://openjdk.org/jeps/409) - Java sealed interface specification
- [Baeldung - Service Locator Pattern](https://www.baeldung.com/java-service-locator-pattern) - Service Locator implementation

### Secondary (MEDIUM confidence)
- [Lightweight EventBus in Java 17+](https://www.laggner.info/posts/lightweight-eventbus-in-java/) - Modern EventBus implementation
- [Sealed Java State Machines](https://benjiweber.co.uk/blog/2020/10/03/sealed-java-state-machines/) - Sealed interface for FSM
- [Baeldung - Java State Design Pattern](https://www.baeldung.com/java-state-design-pattern) - State pattern basics

### Tertiary (LOW confidence)
- N/A - All findings verified with official sources

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All Java 21 built-in features, no external dependencies
- Architecture patterns: HIGH - Classic GoF patterns with modern Java idioms, verified against official sources
- Pitfalls: HIGH - Derived from codebase analysis and established best practices

**Research date:** 2026-02-05
**Valid until:** 90 days (stable patterns, no fast-moving dependencies)
