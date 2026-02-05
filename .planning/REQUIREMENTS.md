# Requirements: Breaking the Tower Modernization

**Defined:** 2026-02-05
**Core Value:** A clean, extensible architecture that makes adding pathfinding straightforward

## v1 Requirements

Requirements for this modernization milestone. Each maps to roadmap phases.

### Foundation

- [x] **FOUN-01**: Project compiles and runs on Java 21
- [x] **FOUN-02**: Golden master test captures current gameplay behavior
- [x] **FOUN-03**: Behavior preservation verified after each refactoring phase

### Language Features

- [x] **LANG-01**: Vec class converted to record with value semantics
- [x] **LANG-02**: Cost/resource data represented as records
- [ ] **LANG-03**: Entity hierarchy sealed with explicit permitted subtypes
- [ ] **LANG-04**: Job interface sealed with explicit permitted implementations
- [x] **LANG-05**: instanceof checks replaced with pattern matching
- [x] **LANG-06**: Switch statements modernized to switch expressions where applicable
- [x] **LANG-07**: var used for local variables where type is obvious

### Design Patterns

- [ ] **PTRN-01**: Game states (title/playing/won) use State pattern
- [ ] **PTRN-02**: EventBus created for decoupled event handling
- [ ] **PTRN-03**: Sound effects triggered via events instead of direct Sounds.play() calls
- [ ] **PTRN-04**: Visual effects (Puffs) triggered via events
- [ ] **PTRN-05**: Service Locator wraps Sounds singleton

### Architecture

- [ ] **ARCH-01**: MovementSystem extracted from entity classes
- [ ] **ARCH-02**: Movement logic in Peon decoupled from behavior logic
- [ ] **ARCH-03**: Movement logic in Monster decoupled from behavior logic
- [ ] **ARCH-04**: Jobs request movement via MovementSystem instead of direct position updates
- [ ] **ARCH-05**: NavigationGrid interface created for world queries
- [ ] **ARCH-06**: Island implements NavigationGrid for collision/walkability queries

## v2 Requirements

Deferred to future milestone. Tracked but not in current roadmap.

### Pathfinding

- **PATH-01**: A* pathfinding algorithm implementation
- **PATH-02**: Path caching for frequently traveled routes
- **PATH-03**: Dynamic path recalculation when obstacles change

### Performance

- **PERF-01**: Spatial partitioning for entity queries
- **PERF-02**: Render culling for off-screen entities

### Additional Patterns

- **PATN-01**: Behavior trees for complex AI decisions
- **PATN-02**: Object pooling for frequently created entities (Puffs)

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
|---------|--------|
| Full ECS architecture | Overkill for ~20 entity types; incremental component extraction sufficient |
| Virtual threads | CPU-bound game loop doesn't benefit; adds complexity |
| Build system (Maven/Gradle) | Works fine with direct javac; not blocking |
| New gameplay features | Focus is architecture, not content |
| Graphics modernization | Keeping existing AWT/Java2D and sprites |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| FOUN-01 | Phase 1 | Complete |
| FOUN-02 | Phase 1 | Complete |
| FOUN-03 | Phase 1 | Complete |
| LANG-01 | Phase 1 | Complete |
| LANG-02 | Phase 1 | Complete |
| LANG-05 | Phase 1 | Complete |
| LANG-06 | Phase 1 | Complete |
| LANG-07 | Phase 1 | Complete |
| PTRN-01 | Phase 2 | Pending |
| PTRN-02 | Phase 2 | Pending |
| PTRN-03 | Phase 2 | Pending |
| PTRN-04 | Phase 2 | Pending |
| PTRN-05 | Phase 2 | Pending |
| ARCH-01 | Phase 3 | Pending |
| ARCH-02 | Phase 3 | Pending |
| ARCH-03 | Phase 3 | Pending |
| ARCH-04 | Phase 3 | Pending |
| LANG-03 | Phase 4 | Pending |
| LANG-04 | Phase 4 | Pending |
| ARCH-05 | Phase 4 | Pending |
| ARCH-06 | Phase 4 | Pending |

**Coverage:**
- v1 requirements: 19 total
- Mapped to phases: 19
- Unmapped: 0

---
*Requirements defined: 2026-02-05*
*Last updated: 2026-02-05 - Phase 1 complete (8 requirements satisfied)*
