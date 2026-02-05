# Features Research: Pathfinding

**Domain:** RTS/god game pathfinding
**Researched:** 2026-02-05
**Confidence:** HIGH (verified against established RTS game patterns)

## Context

Breaking the Tower is a small indie RTS/god game with:
- Tile-based world (256x256 pixel map, 1.5x scale factor)
- Multiple peons moving simultaneously (~10 at start, can grow)
- Obstacles: rocks (static), trees (harvestable), houses (player-built), tower (boss)
- Current movement: straight lines toward targets with random deflection on collision
- Existing systems: `NavigationGrid`, `MovementSystem`, `Job` hierarchy

The current `cantReach()` method has only 10% chance to abandon target on collision - peons often get stuck bumping repeatedly.

---

## Table Stakes (Must Have)

Features users expect. Missing = pathfinding feels broken.

| Feature | Why Expected | Complexity | Existing System Impact |
|---------|--------------|------------|------------------------|
| **Obstacle avoidance** | Peons walk around rocks/trees, not into them repeatedly | Medium | `MovementSystem`, `NavigationGrid.isFree()` |
| **Path to target** | Given target Entity, find walkable route | Medium | `Job.hasTarget()`, `Peon.tick()` |
| **Unreachable detection** | Know when target is impossible (surrounded by rocks) | Low | `Job.cantReach()` - needs proper failure instead of 10% abandon |
| **Diagonal movement** | 8-directional, not just 4-cardinal | Low | `MovementRequest.fromDirection()` already supports any angle |
| **Path recalculation on block** | If path becomes blocked (new house placed), recompute | Medium | `MovementResult.Blocked` already triggers; needs A* integration |

**Critical Insight:** Current system randomly rotates on collision (`rot = random.nextDouble() * Math.PI * 2`). This is the core problem - no intelligence about routing around obstacles.

### Must-Have Behaviors

1. **Peon ordered to tree behind rock** - must path around rock, not bump into it 50 times
2. **Multiple peons moving to same area** - should not all take identical path (causes bunching)
3. **Target becomes unreachable** - should abandon quickly, not bump indefinitely
4. **House placed mid-path** - should reroute

---

## Differentiators (Nice to Have)

Features that add polish but not strictly required for "working" pathfinding.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| **Path smoothing** | Paths look natural, not grid-jagged | Low | Remove intermediate waypoints via line-of-sight checks |
| **Local avoidance (steering)** | Peons dodge each other in motion | Medium | Separation steering behavior, useful for group movement |
| **Partial paths** | If fully unreachable, get as close as possible | Low | Return path to nearest reachable node on failure |
| **Path caching** | Same start/end reuses path | Low | Low value for small maps with dynamic obstacles |
| **Terrain cost weighting** | Prefer paths through grass over near tower | Low | Could make peons avoid danger zones |
| **Group movement** | Multiple selected peons arrive in formation | High | Flow fields or relative offset from group center |

### Polish Behavior Examples

1. **Path smoothing** - Instead of zig-zag grid steps, smooth diagonal motion
2. **Local avoidance** - Two peons approaching from opposite directions sidestep, not deadlock
3. **Partial paths** - Peon gets "as close as possible" to unreachable target then stops

---

## Anti-Features (Avoid)

Things to deliberately NOT build. Overkill for this project scope.

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| **NavMesh** | Complex setup, overkill for simple tile grid | Use grid-based A* directly on world coordinates |
| **Hierarchical pathfinding (HPA*)** | Only needed for huge maps (1000+ tiles) | 256x256 map is tiny, brute A* is fine |
| **Flow fields** | Great for 100+ units, complex to implement | A* per-entity is fine for ~10-30 peons |
| **Jump Point Search (JPS)** | Optimization for uniform cost grids | Premature optimization; basic A* first |
| **Predictive obstacle avoidance** | Predicting where monsters will be | Simple reactive avoidance sufficient |
| **Formation movement** | Military-style squad formations | Casual game, loose grouping is fine |
| **True real-time replanning (D* Lite)** | Incremental pathfinding | Full recompute on block is fine for small paths |

**Guiding Principle:** This is a small indie game, not StarCraft 2. A* with basic features will feel polished; advanced systems would be engineering overhead with no player-visible benefit.

---

## Feature Dependencies

What existing systems each feature touches.

```
A* Core Algorithm
    - Needs: NavigationGrid.isFree(x, y, radius, entity)
    - Needs: World bounds from Island.isOnGround(x, y)
    - Creates: Path (list of positions/nodes)

Path Following
    - Needs: A* path output
    - Modifies: Peon.tick() movement logic
    - Uses: MovementSystem.move() for actual movement
    - Uses: MovementResult.Blocked for replanning trigger

Job Integration
    - Modifies: Job.hasTarget() to request path
    - Modifies: Job.cantReach() for true unreachable detection
    - Modifies: Job.collide() behavior

Grid Representation
    - Needs: Decision on grid cell size (8px? 16px? entity radius?)
    - Needs: NavigationGrid extension or new PathfindingGrid
```

### Dependency Order (Recommended Build Sequence)

1. **Grid representation** - Define how world maps to pathfinding nodes
2. **A* algorithm** - Core pathfinding logic
3. **Path following** - Peon follows waypoints
4. **Job integration** - Jobs request and use paths
5. **Recalculation triggers** - Handle blocked paths
6. (Optional) **Path smoothing** - Polish pass
7. (Optional) **Local avoidance** - Polish pass

---

## Implementation Considerations

### Grid Cell Size

Current entity radii from codebase:
- Peon: r = 1
- Trees/Rocks: variable (likely ~3-8 based on collision patterns)
- Houses: larger (check House.java)

**Recommendation:** 8-pixel grid cells. Small enough for reasonable paths, large enough for performance. Entity radius ~1 means peons fit in single cells.

### Diagonal Movement Cost

Standard approach: Cardinal moves cost 1.0, diagonal moves cost 1.414 (sqrt 2). This ensures paths prefer straight lines when equally valid.

### Heuristic

**Recommendation:** Euclidean distance (since diagonal movement allowed). Manhattan would over-estimate for diagonal-enabled grids.

### Performance Budget

With ~30 peons and small map, pathfinding could run:
- Per-peon when target changes
- Spread across frames if needed (compute N nodes per tick)
- Typical path lengths ~20-50 nodes, well within real-time budget

---

## Sources

- [Group Pathfinding & Movement in RTS Style Games](https://www.gamedeveloper.com/programming/group-pathfinding-movement-in-rts-style-games) - Group movement patterns, common problems
- [Movement Costs for Pathfinders](http://theory.stanford.edu/~amitp/GameProgramming/MovementCosts.html) - Diagonal costs, terrain weights
- [Dealing with Moving Obstacles](http://theory.stanford.edu/~amitp/GameProgramming/MovingObstacles.html) - Recalculation strategies
- [How to RTS: Avoidance Behaviours](https://howtorts.github.io/2014/01/14/avoidance-behaviours.html) - Steering behaviors
- [Let A* Fail but Still Return a Path](https://coffeebraingames.wordpress.com/2018/12/29/let-a-fail-but-still-return-a-path/) - Partial path on unreachable
- [jdxdev RTS Pathfinding Series](https://www.jdxdev.com/blog/2020/05/03/flowfields/) - Practical RTS implementation notes

---

## Summary for Roadmap

**MVP Pathfinding (Table Stakes):**
1. Grid-based A* algorithm
2. Path following in Peon
3. Job integration (path request, arrival, failure)
4. Unreachable target detection

**Polish Pass (Differentiators):**
1. Path smoothing
2. Basic local avoidance between peons

**Explicitly Exclude:**
- NavMesh, flow fields, hierarchical pathfinding
- Complex group movement / formations
- Predictive avoidance
