# Project Research Summary

**Project:** Breaking the Tower - v2 Pathfinding Milestone
**Domain:** A* Pathfinding Integration for RTS/God Game
**Researched:** 2026-02-05
**Confidence:** HIGH

## Executive Summary

Breaking the Tower v2 adds A* pathfinding to an existing Java 21 RTS/god game where peons currently move in straight lines and bump randomly when colliding. Research across stack, features, architecture, and pitfalls converges on a clear recommendation: implement A* from scratch rather than using external libraries, with determinism as the paramount design constraint.

The game's existing architecture was specifically designed for this integration. The v1 milestone established MovementSystem as the single source of truth for movement and NavigationGrid for walkability queries. Pathfinding plugs in as a new service that computes paths using NavigationGrid, returns waypoints, and caches results. MovementSystem continues to handle actual movement execution and dynamic collision detection. This separation allows pathfinding to work at grid resolution while movement handles sub-grid precision.

The critical risk is non-determinism. The game has golden master tests verifying 5000 ticks of deterministic gameplay. Any non-determinism in pathfinding (from HashMap iteration order, floating-point tie-breaking, or request processing order) will cause test failures that are extremely difficult to debug. All data structures must use ordered collections, tie-breaking must be explicit and deterministic, and the pathfinder must be stateless between requests. Beyond determinism, the main architectural challenge is avoiding frame spikes when obstacles change (trees harvested, buildings placed) - solved through lazy path invalidation rather than immediate recalculation.

## Key Findings

### Recommended Stack

Custom A* implementation recommended over external libraries. The game's constraints (pure Java 21, no external frameworks, small 256x256 pixel map scaling to ~96x96 grid cells, under 100 entities) make a self-contained implementation the optimal choice. A custom implementation provides full control over determinism guarantees, avoids dependency management in a pure-Java project, and requires only 200-300 lines of well-structured code.

**Core technologies:**
- **Custom A* Algorithm**: Pathfinding computation - standard algorithm with PriorityQueue and HashSet, determinism enforced through ordered collections and explicit tie-breaking
- **Java 21 Records**: Immutable data structures (PathNode, Path, GridCell) - automatic equals/hashCode, pattern matching support, thread-safe by design
- **ServiceLocator Pattern**: Service registration and access - follows existing codebase pattern, enables clean integration without tight coupling
- **Grid Overlay System**: Discrete pathfinding on continuous world - cell size of ~4.0 world units (2x peon radius) balances path quality with performance

**Why NOT external libraries:**
- Projects like xaguzman/pathfinding add unnecessary dependencies to a zero-dependency codebase
- Black-box libraries may not provide determinism guarantees needed for golden master tests
- The NavigationGrid interface already provides the abstraction layer needed
- Full control over optimization for game-specific patterns (path caching, invalidation triggers)

### Expected Features

**Must have (table stakes):**
- **Obstacle avoidance** - Peons walk around rocks/trees instead of repeatedly bumping into them (replaces current random deflection)
- **Path to target** - Compute walkable route from current position to target entity/location
- **Unreachable detection** - Properly detect when target is impossible to reach (currently only 10% chance to abandon on collision)
- **Diagonal movement** - 8-directional pathfinding matching the game's continuous movement model (not 4-cardinal grid movement)
- **Path recalculation on block** - When path becomes blocked (building placed, tree in the way), recompute route

**Should have (competitive):**
- **Path caching** - Reuse computed paths when target unchanged, cache invalidation on world changes
- **Path smoothing** - Remove unnecessary zigzag grid steps, produce natural-looking diagonal movement
- **Partial paths** - When destination unreachable, return path to nearest accessible location
- **Local avoidance** - Basic steering behaviors to prevent peon-on-peon collisions during movement

**Defer (future milestones):**
- **NavMesh** - Overkill for simple tile grid
- **Hierarchical pathfinding (HPA*)** - Only needed for maps 10x larger
- **Flow fields** - Useful with 100+ units to single destination, not ~10-30 peons with different targets
- **Jump Point Search** - Premature optimization, basic A* sufficient
- **Formation movement** - Military-style squad coordination beyond casual game scope

### Architecture Approach

Pathfinding integrates as a new service layer that coordinates between the grid representation (Island as PathfindingGrid) and movement execution (existing MovementSystem). The pathfinder is stateless between requests, taking current position and target as inputs, querying NavigationGrid.isWalkable() for static obstacles, and returning the next waypoint in world coordinates. MovementSystem continues to handle dynamic collision detection with entities using NavigationGrid.isFree(). This separation allows pathfinding to operate at coarse grid resolution while movement handles fine-grained collision response.

**Major components:**
1. **PathfindingGrid interface** - Extends NavigationGrid with grid-based walkability queries and coordinate conversion (world ↔ grid)
2. **AStarPathfinder class** - Core A* algorithm using PriorityQueue for open set, HashSet for closed set, 8-directional neighbor generation
3. **PathCache class** - Per-entity path caching with goal-based and time-based invalidation (5 second staleness threshold)
4. **PathfindingService class** - High-level service coordinating grid conversion, path computation, caching, and invalidation
5. **Island implementation** - Adds PathfindingGrid methods using existing isOnGround() bitmap for walkability at grid resolution
6. **Peon/Monster integration** - Replace direct angle calculation with PathfindingService.getNextPosition() query

**Build order:** Foundation types (GridCell, Path records) → A* algorithm + unit tests → Island grid implementation → Service layer (cache, service, ServiceLocator registration) → Entity integration (Peon, Monster) → Golden master validation

### Critical Pitfalls

1. **Non-deterministic data structures** - Using HashMap or HashSet without ordered alternatives causes iteration order to vary between runs, breaking golden master tests. Prevention: Use TreeMap/TreeSet with explicit Comparator, or LinkedHashMap/LinkedHashSet. Add determinism validation test (run twice, assert identical output). Address in Phase 1 - bake into core A* design.

2. **Floating-point tie-breaking non-determinism** - When two nodes have equal f-scores, floating-point precision errors cause non-deterministic comparison results. Prevention: Use integer arithmetic for g/h costs, or explicit secondary tie-breaker using deterministic node properties (e.g., gridX * 1000 + gridY). Avoid Math.sqrt() in heuristic comparisons. Address in Phase 1.

3. **Path request order sensitivity** - Multiple peons requesting paths in same tick with shared mutable state produces order-dependent results. Prevention: Pathfinder must be stateless between requests, process in deterministic entity list order. Address in Phase 1 - design for multi-request from start.

4. **Pathfinding spike on obstacle change** - Synchronous recalculation of all affected paths when building placed or tree harvested causes 100+ ms frame freeze. Prevention: Lazy invalidation (mark invalid, recalculate on next use), path splicing (recompute only blocked segment), budget spreading (max N recalculations per tick). Address in Phase 3.

5. **Unbounded search space** - Path requests to distant or unreachable destinations explore entire map before failing. Prevention: Node exploration limit (~2000 nodes), early rejection (verify destination walkable before search), graceful failure mode. Address in Phase 1 with hard limits.

## Implications for Roadmap

Based on combined research, pathfinding implementation naturally divides into four phases with clear dependencies and deliverables. The critical insight is that determinism must be enforced from day one - retrofitting determinism into non-deterministic code is nearly impossible. Phases build incrementally, with each phase adding functionality while preserving determinism guarantees.

### Phase 1: Core A* Algorithm (Foundation)

**Rationale:** Establishes deterministic pathfinding foundation. All determinism pitfalls (P1, P2, P3) must be addressed in initial implementation - retrofitting determinism is extremely difficult. This phase delivers working pathfinding without integration, allowing algorithm validation in isolation.

**Delivers:**
- Grid coordinate system with conversion between world and grid space
- PathNode, Path, GridCell records with deterministic equality
- A* algorithm with explicit tie-breaking and node exploration limits
- Unit tests verifying correct paths, unreachable target handling, and determinism (same input produces same output across runs)

**Addresses features:**
- Table stakes: obstacle avoidance, path computation, unreachable detection, diagonal movement
- Stack: Custom A* with Java 21 records, PriorityQueue/TreeSet for deterministic collections

**Avoids pitfalls:**
- P1: Non-deterministic data structures (use TreeSet/LinkedHashMap)
- P2: Floating-point tie-breaking (integer coordinates, explicit secondary comparator)
- P3: Request order sensitivity (stateless pathfinder)
- P4: Inadmissible heuristic (Euclidean for 8-directional movement)
- P6: Unbounded search (hard node limit of 2000)

**Research flag:** Standard A* implementation, well-documented patterns, skip deeper research.

### Phase 2: Integration with Movement System

**Rationale:** Connects pathfinding to actual entity movement. Dependencies: Phase 1 provides working pathfinder, existing MovementSystem handles execution. This phase makes pathfinding visible in gameplay while maintaining MovementSystem as single source of truth for position updates.

**Delivers:**
- Island implements PathfindingGrid interface with grid-based walkability queries
- PathfindingService registered with ServiceLocator
- Peon and Monster query PathfindingService.getNextPosition() instead of direct angle calculation
- MovementResult.Blocked triggers path invalidation
- Waypoint advancement when entity reaches current target position

**Addresses features:**
- Table stakes: Full integration of obstacle avoidance and target pathfinding
- Architecture: Service layer pattern, MovementSystem/PathfindingService separation

**Avoids pitfalls:**
- Integration preserves determinism from Phase 1
- Clear separation between pathfinding (coarse grid) and movement execution (fine collision)

**Research flag:** Integration point verification needed - ensure grid resolution matches entity collision radii, validate waypoint following doesn't cause oscillation.

### Phase 3: Path Caching and Optimization

**Rationale:** Eliminates per-tick recalculation overhead. Dependencies: Phase 2 provides working integrated system. Without caching, every peon recalculates path every tick (10 peons * 30 tps = 300 path requests/second). Caching reduces computation by ~95% for steady movement.

**Delivers:**
- PathCache with per-entity storage and goal-based invalidation
- Time-based staleness (5 second expiry handles untracked world changes)
- Event-driven invalidation on obstacle changes (building placed, tree harvested)
- Per-tick pathfinding budget (limit simultaneous computations to prevent spikes)
- Path following logic that reuses cached path until blocked or stale

**Addresses features:**
- Should-have: Path caching for performance
- Competitive edge: Smooth performance even with many moving entities

**Avoids pitfalls:**
- P5: Pathfinding spike on obstacle change (lazy invalidation, budget system)
- P7: Per-tick recalculation overhead (event-driven invalidation)
- P8: Memory allocation in hot path (object pooling if profiling shows GC pressure)

**Research flag:** Performance profiling needed - validate 30 tps maintained with 20+ peons, check for GC pressure under heavy pathfinding load.

### Phase 4: Dynamic Obstacle Handling

**Rationale:** Handles runtime world changes gracefully. Dependencies: Phase 3 provides caching infrastructure. This phase refines path invalidation and recalculation to avoid visual jitter and oscillation when obstacles appear/disappear.

**Delivers:**
- Path splicing (preserve valid path segments, only recompute blocked portion)
- Recalculation throttling (minimum interval between updates to prevent oscillation)
- Commit distance (finish movement to current waypoint before accepting new path)
- Hysteresis in path selection (require significant cost difference to switch routes)
- Integration with building placement and resource harvesting systems

**Addresses features:**
- Table stakes: Path recalculation on block (refined from basic Phase 2 implementation)
- Polish: Smooth, natural-looking path updates without visible jitter

**Avoids pitfalls:**
- P10: Path recalculation visible jitter (commit distance, smoothing buffer)
- P12: Oscillation between two states (hysteresis, cooldown periods)

**Research flag:** Standard dynamic pathfinding patterns, may benefit from user feedback on feel during implementation.

### Phase 5: Polish and Path Smoothing (Optional)

**Rationale:** Improves visual quality of paths. Dependencies: Phases 1-4 provide fully functional pathfinding. This phase is optional polish that doesn't affect gameplay correctness.

**Delivers:**
- String-pulling algorithm to remove unnecessary waypoints
- Line-of-sight checks between waypoints for straight-line shortcuts
- Smoothed paths that look natural rather than grid-aligned
- Deterministic smoothing (same input produces same smoothed output)

**Addresses features:**
- Should-have: Path smoothing for natural movement
- Differentiators: Professional-looking AI movement

**Avoids pitfalls:**
- P9: Zigzag path on grid (string pulling, line-of-sight smoothing)
- Maintains determinism during smoothing operations

**Research flag:** Path smoothing algorithms well-documented, optional phase can be deferred to future milestone if time constrained.

### Phase Ordering Rationale

- **Phase 1 before all others:** Determinism cannot be retrofitted. Non-deterministic foundation makes debugging impossible once integrated.
- **Phase 2 before caching:** Need to see pathfinding working in gameplay before optimizing. Validates algorithm correctness and integration points.
- **Phase 3 before dynamic handling:** Simple caching establishes infrastructure. Dynamic obstacle handling builds on cache invalidation mechanisms.
- **Phase 4 timing:** Dynamic obstacles are gameplay-critical (buildings, harvesting) but can work with naive invalidation initially. Refine after basic system proven.
- **Phase 5 optional:** Path smoothing is pure polish. Defer if timeline tight, doesn't affect core functionality.

### Research Flags

**Phases needing validation during implementation:**
- **Phase 2 (Integration):** Grid resolution tuning - cell size of 4.0 units is initial estimate, may need adjustment based on collision radius testing
- **Phase 3 (Caching):** Performance profiling - validate assumptions about path computation cost, check for GC pressure
- **Phase 4 (Dynamic):** User experience testing - path recalculation feel is subjective, may need iteration

**Phases with standard patterns (minimal additional research):**
- **Phase 1 (Core A*):** Algorithm thoroughly documented, pattern matching existing implementations
- **Phase 5 (Smoothing):** String-pulling and line-of-sight algorithms well-established

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Custom A* well-documented, Java 21 records ideal for immutable nodes, existing codebase patterns (ServiceLocator) proven |
| Features | HIGH | Table stakes verified against multiple RTS implementations, anti-features explicitly identified to avoid scope creep |
| Architecture | HIGH | v1 architecture specifically designed for pathfinding integration, integration points clear and validated against codebase |
| Pitfalls | HIGH | Determinism pitfalls critical for this project, verified against authoritative sources (Riot Games, Random ASCII), prevention strategies well-established |

**Overall confidence:** HIGH

Research is comprehensive and cross-validated. Key recommendations (custom implementation, determinism-first approach, phased integration) supported by multiple authoritative sources and direct codebase analysis. The existing v1 architecture provides clean integration points that were explicitly designed for pathfinding in subsequent milestones.

### Gaps to Address

Minor unknowns that will resolve during implementation:

- **Grid cell size tuning:** Initial recommendation of 4.0 world units based on peon radius of 1.0, but actual collision behavior may require adjustment. Easily tunable constant once integrated and tested.
- **Cache size limits:** Initial design has unbounded per-entity cache. May need LRU eviction if memory profiling shows issues, but grid is small enough that this is unlikely.
- **Per-tick computation budget:** Initial recommendation of max 5 path computations per tick is estimate. Adjust based on profiling with realistic entity counts and map complexity.
- **Path smoothing algorithm choice:** Multiple smoothing algorithms available (string pulling, Catmull-Rom spline, funnel). Defer selection to Phase 5 implementation based on determinism requirements and visual results.

These gaps are implementation details that don't affect the overall architecture or phase structure. They represent tuning parameters that will be refined during development.

## Sources

### Primary (HIGH confidence)

**Stack Research:**
- [Implementing A* Pathfinding in Java | Baeldung](https://www.baeldung.com/java-a-star-pathfinding) - Java implementation patterns with PriorityQueue
- [Introduction to A* | Red Blob Games](https://www.redblobgames.com/pathfinding/a-star/introduction.html) - Authoritative A* tutorial with visualizations
- [Pathfinding with Grids | Red Blob Games](https://www.redblobgames.com/pathfinding/grids/algorithms.html) - Grid-specific considerations

**Features Research:**
- [Group Pathfinding & Movement in RTS Style Games | Game Developer](https://www.gamedeveloper.com/programming/group-pathfinding-movement-in-rts-style-games) - RTS pathfinding patterns
- [Movement Costs for Pathfinders | Amit Patel](http://theory.stanford.edu/~amitp/GameProgramming/MovementCosts.html) - Diagonal costs, heuristic selection
- [Dealing with Moving Obstacles | Amit Patel](http://theory.stanford.edu/~amitp/GameProgramming/MovingObstacles.html) - Dynamic obstacle strategies

**Architecture Research:**
- Direct codebase analysis of Breaking the Tower v1 architecture (MovementSystem, NavigationGrid, ServiceLocator patterns)
- [Pathfinding API | gdx-ai Wiki](https://github.com/libgdx/gdx-ai/wiki/Pathfinding-API) - API design patterns

**Pitfalls Research:**
- [Floating-Point Determinism | Random ASCII](https://randomascii.wordpress.com/2013/07/16/floating-point-determinism/) - Authoritative guide to deterministic floating-point
- [Determinism in League of Legends | Riot Games](https://technology.riotgames.com/news/determinism-league-legends-fixing-divergences) - Real-world determinism challenges and solutions
- [Pathfinding Architecture Optimizations | Game AI Pro](http://www.gameaipro.com/GameAIPro/GameAIPro_Chapter17_Pathfinding_Architecture_Optimizations.pdf) - Performance optimization patterns

### Secondary (MEDIUM confidence)

- [RTS Pathfinding: Flow Fields | jdxdev](https://www.jdxdev.com/blog/2020/05/03/flowfields/) - When to use flow fields (and when not to)
- [A* Pathfinding Project: Optimization](https://arongranberg.com/astar/documentation/stable/optimization.html) - Commercial pathfinding library optimization techniques
- [How to RTS: Avoidance Behaviours](https://howtorts.github.io/2014/01/14/avoidance-behaviours.html) - Local avoidance patterns
- [Analyzing Tie-Breaking Strategies for A* | IJCAI](https://www.ijcai.org/Proceedings/2018/0655.pdf) - Academic analysis of tie-breaking impact

### Tertiary (LOW confidence)

- [Pathfinding with A Star Algorithm in Java | Medium](https://medium.com/@AlexanderObregon/pathfinding-with-the-a-star-algorithm-in-java-3a66446a2352) - Basic implementation tutorial
- [Dynamic Pathfinding Algorithms in Game Development | peerdh](https://peerdh.com/blogs/programming-insights/dynamic-pathfinding-algorithms-in-game-development) - High-level overview

---
**Research completed:** 2026-02-05
**Ready for roadmap:** Yes

The research provides strong foundation for roadmap creation. Phase structure is clear with explicit dependencies, deliverables, and pitfall mitigation strategies. Determinism requirements and integration points are well-understood. Recommended approach (custom A* implementation with phased integration) is validated against authoritative sources and existing codebase architecture.
