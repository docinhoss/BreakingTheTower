# Requirements: Breaking the Tower v2 Pathfinding

**Defined:** 2026-02-05
**Core Value:** Peons navigate intelligently around obstacles instead of bumping repeatedly

## v2 Requirements

Requirements for pathfinding milestone. Each maps to roadmap phases.

### Core Algorithm

- [x] **PATH-01**: Peon finds walkable route to target around static obstacles
- [x] **PATH-02**: Peon navigates using 8-directional movement (including diagonals)
- [x] **PATH-03**: Pathfinding uses deterministic data structures (golden master compatibility)
- [x] **PATH-04**: Pathfinding completes within tick budget (no frame drops)

### Unreachable Handling

- [ ] **REACH-01**: Peon detects when target is completely blocked (surrounded by obstacles)
- [ ] **REACH-02**: Peon abandons unreachable targets quickly (not 10% random abandon)
- [ ] **REACH-03**: Pathfinding has node limit to prevent unbounded search

### Dynamic Recalculation

- [ ] **DYN-01**: Peon recalculates path when blocked mid-route
- [ ] **DYN-02**: Peon reroutes when obstacle appears in path (house built, tree harvested)
- [ ] **DYN-03**: Path invalidation does not cause frame spikes (lazy recalculation)

### Integration

- [x] **INT-01**: Pathfinding integrates with existing MovementSystem
- [x] **INT-02**: Pathfinding uses NavigationGrid for walkability queries
- [x] **INT-03**: PathfindingService accessible via ServiceLocator

## Future Requirements

Deferred to v3 or later. Tracked but not in v2 roadmap.

### Polish

- **POLISH-01**: Path smoothing for natural-looking movement
- **POLISH-02**: Path caching for frequently traveled routes

### Advanced

- **ADV-01**: Local avoidance (peons dodge each other in motion)
- **ADV-02**: Group movement coordination

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
|---------|--------|
| NavMesh | Overkill for simple grid-based world |
| Flow fields | Not enough units (10-30 peons) to justify complexity |
| Hierarchical pathfinding (HPA*) | Grid too small (~96x96 cells) |
| Jump Point Search (JPS) | Premature optimization |
| Predictive obstacle avoidance | Simple reactive sufficient |
| Formation movement | Casual game, loose grouping fine |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| PATH-01 | Phase 5 | Complete |
| PATH-02 | Phase 5 | Complete |
| PATH-03 | Phase 5 | Complete |
| PATH-04 | Phase 5 | Complete |
| INT-01 | Phase 5 | Complete |
| INT-02 | Phase 5 | Complete |
| INT-03 | Phase 5 | Complete |
| REACH-01 | Phase 6 | Pending |
| REACH-02 | Phase 6 | Pending |
| REACH-03 | Phase 6 | Pending |
| DYN-01 | Phase 7 | Pending |
| DYN-02 | Phase 7 | Pending |
| DYN-03 | Phase 7 | Pending |

**Coverage:**
- v2 requirements: 13 total
- Mapped to phases: 13
- Unmapped: 0

---
*Requirements defined: 2026-02-05*
*Last updated: 2026-02-05 — Phase 5 requirements complete (7/13)*
