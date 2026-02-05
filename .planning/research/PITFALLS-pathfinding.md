# Pitfalls Research: Pathfinding

**Project:** Breaking the Tower
**Domain:** A* pathfinding for existing RTS/god game
**Researched:** 2026-02-05
**Overall Confidence:** HIGH (verified against authoritative sources)

## Context

Breaking the Tower has specific constraints that make certain pathfinding pitfalls especially dangerous:
- **Golden master testing** verifies 5000 ticks of deterministic gameplay
- **30 tps tick budget** (~33ms per tick)
- **Multiple peons** moving simultaneously
- **Dynamic obstacles** (trees harvested, houses built)
- **Floating-point positions** using `double` in Vec record

---

## Critical Pitfalls

These will break the game in ways that are hard to debug.

### P1: Non-Deterministic Data Structures

**What goes wrong:** Using `HashMap`, `HashSet`, or unordered collections in pathfinding causes iteration order to vary between runs, producing different paths for identical inputs. Golden master test fails intermittently or after unrelated code changes.

**Why it happens:** Java's `HashMap` iteration order depends on hash codes and bucket placement, which can vary across JVM runs, especially after rehashing. When A* iterates over the open set or closed set, different iteration orders explore different nodes first, potentially finding different (but equally valid) paths.

**Consequences:**
- Golden master test fails unpredictably
- "It works on my machine" syndrome
- Debugging becomes nearly impossible (non-reproducible behavior)

**Warning signs:**
- Tests pass sometimes, fail other times
- Different paths on different JVMs or Java versions
- State diverges mid-game but starting conditions identical

**Prevention:**
- Use `TreeMap`/`TreeSet` with explicit `Comparator` for all pathfinding collections
- Or use `LinkedHashMap`/`LinkedHashSet` for insertion-order determinism
- Implement explicit tie-breaking in priority queue (not relying on insertion order alone)
- Add determinism validation test: run pathfinding twice with same input, assert identical output

**Phase to address:** Phase 1 (core A* implementation) - bake into initial design

---

### P2: Floating-Point Tie-Breaking Non-Determinism

**What goes wrong:** When two nodes have equal f-scores (f = g + h), the priority queue must choose one. If the tie-breaking uses floating-point comparisons susceptible to precision errors, results become non-deterministic.

**Why it happens:**
- `(A * B) * C != A * (B * C)` for floating-point
- Heuristic calculations may produce values that are "equal" within epsilon but have tiny differences
- Different node expansion orders based on rounding at 15th decimal place

**Consequences:**
- Paths vary subtly between runs
- Golden master fails with "off by one tile" divergences
- Extremely hard to diagnose (paths are valid, just different)

**Warning signs:**
- Two nodes that "should" have equal f-scores produce different comparison results
- Path differences that appear random
- Divergence only happens for certain map configurations

**Prevention:**
- Use integer or fixed-point arithmetic for g and h scores
- If using doubles, implement explicit secondary tie-breaker using deterministic node property (e.g., node ID, or `Integer.compare(node1.x * 1000 + node1.y, node2.x * 1000 + node2.y)`)
- Avoid `Math.sqrt()` in heuristic - use squared distance for comparisons
- Grid-based coordinates should be integers; only final interpolated movement uses doubles

**Phase to address:** Phase 1 (core A* implementation)

---

### P3: Path Request Order Sensitivity

**What goes wrong:** Multiple peons request paths in the same tick. If request processing order affects results (e.g., through shared mutable state or non-deterministic queuing), different peons get different paths between runs.

**Why it happens:**
- Entity iteration order in `Island.tick()` uses `ArrayList` (deterministic), but if pathfinding has global mutable state, it may be affected by request order
- Path caching with eviction: which path gets evicted depends on request timing
- Shared "visited" structures not properly reset between requests

**Consequences:**
- First peon to request path succeeds; later peons get corrupted results
- Golden master diverges after multiple peons start moving
- Works with one peon, fails with many

**Warning signs:**
- Behavior changes based on number of active peons
- First tick after spawning multiple peons shows divergence
- Path results seem "contaminated" by previous requests

**Prevention:**
- Pathfinder must be stateless between requests OR explicitly reset state before each request
- If using object pooling, ensure complete reset
- Process requests in deterministic order (entity list order in `Island`)
- Test with varying numbers of simultaneous path requests

**Phase to address:** Phase 1 (core A* implementation) - design for multi-request from start

---

### P4: Heuristic Inadmissibility Breaking Optimality

**What goes wrong:** Heuristic overestimates actual cost, causing A* to find suboptimal paths or miss valid paths entirely. Peons walk through obstacles or take bizarre routes.

**Why it happens:**
- Euclidean distance heuristic with diagonal movement cost mismatch
- Terrain cost multipliers not reflected in heuristic
- Mixing coordinate systems (world coords vs grid coords)

**Consequences:**
- Non-optimal paths (longer than necessary)
- In extreme cases, fails to find path when one exists
- Peons appear "drunk" - taking nonsensical routes

**Warning signs:**
- Paths clearly longer than direct route around obstacles
- Pathfinding fails in situations where path obviously exists
- Adding obstacles sometimes makes paths shorter (red flag!)

**Prevention:**
- Use consistent distance metric: if movement allows diagonals at cost 1.414, heuristic must use Euclidean
- If movement only allows cardinal directions at cost 1, heuristic must use Manhattan
- h(n) <= actual cost to goal for all nodes (admissibility)
- Add assertion: path cost >= heuristic estimate (always true for admissible heuristics)

**Phase to address:** Phase 1 (core A* implementation)

---

## Performance Pitfalls

These cause lag spikes or frame drops.

### P5: Pathfinding Spike on Obstacle Change

**What goes wrong:** When an obstacle appears/disappears (tree harvested, house built), all active paths through that area need recalculation. If this happens synchronously, game freezes for hundreds of milliseconds.

**Why it happens:**
- Naive implementation: obstacle change -> invalidate all paths -> recalculate all immediately
- Multiple peons affected simultaneously
- Dynamic obstacle changes cluster (e.g., building completion triggers cascade)

**Consequences:**
- Visible stutter when placing buildings
- Frame rate tanks when forest is cleared
- 30 tps budget blown by single event

**Warning signs:**
- Profiler shows pathfinding spikes after obstacle changes
- Players report "lag when building"
- Frame time variance extremely high

**Prevention:**
- **Lazy invalidation:** Mark paths as "possibly invalid" but don't recalculate until peon reaches invalidated segment
- **Path splicing:** When obstacle detected in path, only recalculate from current position to rejoining point
- **Budget spreading:** Limit path recalculations per tick (e.g., max 3), queue remainder for next ticks
- **Local detection:** Peon checks only next N waypoints, not entire path

**Phase to address:** Phase 3 (dynamic obstacle handling) - after basic A* works

---

### P6: Unbounded Search Space

**What goes wrong:** Path request between distant points or toward unreachable destination causes A* to explore entire map before failing.

**Why it happens:**
- No maximum search limit
- Destination blocked (e.g., inside rock formation) - A* must prove no path exists
- Heuristic guides toward destination but path requires long detour

**Consequences:**
- Single path request takes 100+ ms
- Game hitches for faraway movement commands
- Invalid destinations (inside walls) cause catastrophic slowdown

**Warning signs:**
- Frame time spikes for certain movement commands
- Pathfinding to "impossible" locations hangs the game
- Longer distances take disproportionately longer

**Prevention:**
- **Node limit:** Cap at reasonable maximum (e.g., 2000 nodes explored)
- **Early rejection:** Before searching, verify destination is walkable (`isOnGround` check)
- **Hierarchical search:** For distances >40 tiles, use coarse pathfinding first
- **Failure mode:** When limit exceeded, return "no path" gracefully - let peon behavior handle it

**Phase to address:** Phase 1 (core A* implementation) - build in limits from start

---

### P7: Per-Tick Recalculation Accumulation

**What goes wrong:** Naive implementation recalculates path every tick "just in case." With 10 peons at 30 tps, that's 300 pathfinding operations per second.

**Why it happens:**
- Defensive programming: "recalculate to be safe"
- No path validity tracking
- Confusing "path following" with "pathfinding"

**Consequences:**
- Constant high CPU usage
- No headroom for other game logic
- Battery drain on mobile/laptop

**Warning signs:**
- Profiler shows pathfinding as top CPU consumer even when nothing is changing
- CPU usage flat regardless of game activity
- "Idle" game state still burns cycles

**Prevention:**
- **Path following vs pathfinding:** Once path calculated, follow waypoints without recalculating
- **Event-driven invalidation:** Only recalculate when obstacle actually changes
- **Dirty flag:** Mark paths invalid, recalculate only when peon next needs to move
- **Amortization:** If multiple recalculations needed, spread across ticks

**Phase to address:** Phase 2 (path caching) - critical for efficiency

---

### P8: Memory Allocation in Hot Path

**What goes wrong:** Pathfinding allocates new objects (nodes, lists, etc.) during search. With frequent path requests, garbage collector pressure causes periodic hitches.

**Why it happens:**
- Creating new `Node` objects for each explored position
- Building new `ArrayList` for open/closed sets each search
- Returning new `List<Vec>` for each path result

**Consequences:**
- GC pauses cause frame hitches
- Memory fragmentation
- Non-deterministic GC timing (another source of non-reproducibility!)

**Warning signs:**
- GC logs show frequent young-gen collections
- Memory sawtooth pattern in profiler
- Occasional long pauses during heavy pathfinding

**Prevention:**
- **Object pooling:** Reuse node objects
- **Pre-allocated collections:** Size collections to expected maximum
- **Primitive arrays:** Use `int[]` for coordinates instead of `Vec` objects during search
- **Path object recycling:** Return paths to pool when peon completes

**Phase to address:** Phase 2 (optimization) - after correctness verified

---

## Gameplay Feel Pitfalls

These technically work but feel wrong to players.

### P9: Zigzag Path on Grid

**What goes wrong:** A* returns grid-aligned path with unnecessary zigzags. Peon movement looks robotic, not natural.

**Why it happens:**
- Standard A* on 8-directional grid produces paths that hug grid lines
- No post-processing to smooth path
- Tie-breaking causes systematic bias toward certain directions

**Consequences:**
- Peons look like they're playing Snake
- Movement doesn't match player intention
- Feels cheap/amateur

**Warning signs:**
- Paths have unnecessary turns
- Peons never move diagonally (or always move diagonally)
- Straight-line destinations produce zigzag paths

**Prevention:**
- **Path smoothing:** Post-process with funnel algorithm or line-of-sight checks
- **String pulling:** Remove intermediate waypoints when direct path exists
- **Any-angle pathfinding:** Consider Theta* variant if smoothness critical
- But: keep smoothing deterministic! Same input -> same smoothed output

**Phase to address:** Phase 1 (core A*) or optional Phase 4 (polish)

---

### P10: Path Recalculation Visible Jitter

**What goes wrong:** When path is recalculated, peon visibly changes direction mid-stride, causing jerky movement.

**Why it happens:**
- New path has different initial direction than current heading
- No blending between old path and new path
- Recalculation triggered too frequently

**Consequences:**
- Peons "vibrate" or "dance" when obstacles change nearby
- Movement looks indecisive
- Player loses trust in AI

**Warning signs:**
- Peons hesitate at decision points
- Frequent small direction changes
- Paths "pop" visibly when obstacles change

**Prevention:**
- **Commit distance:** Once started toward waypoint, complete movement to it before accepting new path
- **Smoothing buffer:** Blend new path direction with current heading over several frames
- **Recalculation throttle:** Minimum interval between path updates (e.g., 500ms)
- **Path splicing:** Preserve beginning of path, only modify end

**Phase to address:** Phase 3 (dynamic obstacles)

---

### P11: Units Bunching at Chokepoints

**What goes wrong:** Multiple peons all find same "optimal" path through narrow gap, causing collisions and congestion.

**Why it happens:**
- All peons pathfind independently to same destination
- No awareness of other peons' paths
- Pathfinding doesn't account for collision footprint in narrow areas

**Consequences:**
- Peons stack on top of each other
- Movement throughput collapses at bottlenecks
- Looks like rush hour traffic jam

**Warning signs:**
- Multiple peons occupy same tile
- Progress stalls at narrow passages
- First peon through, rest stuck

**Prevention:**
- **Reservation system:** Peons "claim" path tiles, others route around
- **Flow fields:** Single shared field for common destination
- **Local avoidance (RVO):** Separate from pathfinding, handles real-time collision
- **Staggered timing:** Delay path execution to spread load

**Phase to address:** Optional Phase 4 (advanced) - complex feature, may be out of scope for v2

---

### P12: Oscillation Between Two States

**What goes wrong:** Peon alternates between two paths when obstacle state flickers, or when path exactly straddles a threshold.

**Why it happens:**
- Obstacle appears/disappears rapidly (e.g., another peon passing through)
- Path decision is at exact boundary between two routes
- No hysteresis in path selection

**Consequences:**
- Peon walks back and forth without making progress
- Looks like AI is broken
- May never reach destination

**Warning signs:**
- Peon walks same segment repeatedly
- Path changes every tick
- Behavior depends on exact peon position

**Prevention:**
- **Hysteresis:** Once committed to path, require significant cost difference to switch (e.g., >20% improvement)
- **Cooldown:** After path change, immune to further changes for N ticks
- **Obstacle debouncing:** Don't treat moving entities as obstacles until stationary

**Phase to address:** Phase 3 (dynamic obstacles)

---

## Project-Specific Warnings

| Area | Likely Pitfall | Mitigation | Phase |
|------|---------------|------------|-------|
| Vec record uses `double` | P2: Float tie-breaking | Grid coordinates as int internally | Phase 1 |
| Golden master 5000 ticks | P1, P2, P3: Any non-determinism | Comprehensive determinism tests | Phase 1 |
| Multiple peons | P3: Request order, P7: Overhead | Stateless pathfinder, budget limits | Phase 1 |
| Trees harvested | P5: Spike on change | Lazy invalidation | Phase 3 |
| Houses built | P5: Spike, P10: Jitter | Path splicing, commit distance | Phase 3 |
| 30 tps timing | P6: Unbounded, P7: Per-tick | Node limits, caching | Phase 1-2 |
| `Island.entities` ArrayList | Deterministic order (good) | Maintain this property | All |

---

## Prevention Strategies Summary

### For Determinism (Critical for this project)

1. **Data structure audit:** Every Set/Map in pathfinding must be ordered
2. **Tie-breaking protocol:** Explicit secondary comparator, never rely on default
3. **Integer coordinates internally:** Grid cells as `int`, convert to `double` only for final movement
4. **Determinism test:** Run same scenario twice, byte-compare results
5. **No external dependencies:** System time, random without seed, etc.

### For Performance

1. **Budget system:** Max N pathfinding operations per tick, queue overflow for next tick
2. **Early rejection:** Verify destination walkable before searching
3. **Node limit:** Hard cap on explored nodes (2000 reasonable starting point)
4. **Lazy evaluation:** Don't recalculate until actually needed
5. **Profile first:** Don't optimize imagined problems - measure actual bottlenecks

### For Gameplay Feel

1. **Path smoothing:** Remove unnecessary waypoints post-A*
2. **Commit distance:** Don't interrupt mid-waypoint
3. **Recalculation throttle:** Minimum time between updates
4. **Visual continuity:** New path should blend, not snap

---

## Phase Recommendations

Based on pitfall analysis, recommended phase structure:

**Phase 1: Core A* (CRITICAL)**
- Address: P1, P2, P3, P4, P6
- Must be deterministic from day one
- Include node limit, proper heuristic
- Test: golden master passes, determinism validation

**Phase 2: Path Caching and Optimization**
- Address: P7, P8
- Cache computed paths
- Object pooling for performance
- Test: no recalculation without cause

**Phase 3: Dynamic Obstacles**
- Address: P5, P10, P12
- Path invalidation on obstacle change
- Lazy recalculation, path splicing
- Hysteresis to prevent oscillation
- Test: tree harvesting doesn't spike frame time

**Phase 4: Polish (Optional)**
- Address: P9, P11
- Path smoothing for natural movement
- Local avoidance for multi-peon (stretch goal)

---

## Sources

### Determinism
- [Floating-Point Determinism - Random ASCII](https://randomascii.wordpress.com/2013/07/16/floating-point-determinism/)
- [Determinism in League of Legends - Riot Games](https://technology.riotgames.com/news/determinism-league-legends-fixing-divergences)
- [Game Networking: Deterministic - Ruoyu Sun](https://ruoyusun.com/2019/03/29/game-networking-2.html)
- [A* Pathfinding for Deterministic Simulation - Aron Granberg Forum](https://forum.arongranberg.com/t/pathfinding-for-deterministic-simulation/53)
- [Java TreeMap vs HashMap - Baeldung](https://www.baeldung.com/java-treemap-vs-hashmap)

### Performance
- [Pathfinding Architecture Optimizations - Game AI Pro](http://www.gameaipro.com/GameAIPro/GameAIPro_Chapter17_Pathfinding_Architecture_Optimizations.pdf)
- [Multi-threaded A* Pathfinding - arXiv](https://arxiv.org/html/2602.04130)
- [A* Pathfinding Project: Optimization](https://arongranberg.com/astar/documentation/stable/optimization.html)
- [RTS Pathfinding over Multiple Frames - GameDev.net](https://www.gamedev.net/forums/topic/88736-how-to-implement-pathfinding-in-rts-over-multiple-frames/)

### Dynamic Obstacles
- [Moving Obstacles - Amit Patel / Stanford](http://theory.stanford.edu/~amitp/GameProgramming/MovingObstacles.html)
- [RTS Pathfinding: Dynamic Navmesh - jdxdev](https://www.jdxdev.com/blog/2021/07/06/rts-pathfinding-2-dynamic-navmesh-with-constrained-delaunay-triangles/)
- [Group Pathfinding in RTS Games - Game Developer](https://www.gamedeveloper.com/programming/group-pathfinding-movement-in-rts-style-games)

### Algorithms and Feel
- [Introduction to A* - Red Blob Games](https://www.redblobgames.com/pathfinding/a-star/introduction.html)
- [Toward More Realistic Pathfinding - Game Developer](https://www.gamedeveloper.com/programming/toward-more-realistic-pathfinding)
- [Analyzing Tie-Breaking Strategies for A* - IJCAI](https://www.ijcai.org/Proceedings/2018/0655.pdf)
- [How to RTS: Avoidance Behaviours](https://howtorts.github.io/2014/01/14/avoidance-behaviours.html)

### Multi-Unit
- [Boids for RTS - jdxdev](https://www.jdxdev.com/blog/2021/03/19/boids-for-rts/)
- [Group Movement Research](https://ap011y0n.github.io/Group-Movement/)
