---
phase: 01-foundation-language-modernization
plan: 03
subsystem: language
tags: [java21, records, pattern-matching, switch-expressions, var]

# Dependency graph
requires:
  - phase: 01-01
    provides: Maven project structure with Java 21 compiler
  - phase: 01-02
    provides: Golden master test infrastructure for behavioral regression detection
provides:
  - Vec record with automatic equals/hashCode and value semantics
  - Cost record for immutable building costs (LANG-02)
  - Pattern matching for instanceof throughout codebase
  - Switch expressions in Resources.add()
  - var declarations for obvious local variable types
affects: [02-tower-mechanics, 03-movement-extraction, 04-pathfinding]

# Tech tracking
tech-stack:
  added: []
  patterns: [Java records for value objects, pattern matching instanceof, switch expressions]

key-files:
  created:
    - src/main/java/com/mojang/tower/Cost.java
  modified:
    - src/main/java/com/mojang/tower/Vec.java
    - src/main/java/com/mojang/tower/HouseType.java
    - src/main/java/com/mojang/tower/Resources.java
    - src/main/java/com/mojang/tower/House.java
    - src/main/java/com/mojang/tower/Peon.java
    - src/main/java/com/mojang/tower/TowerComponent.java
    - src/main/java/com/mojang/tower/Island.java

key-decisions:
  - "Vec and Cost as records for automatic equals/hashCode (value semantics)"
  - "Pattern matching only where cast follows instanceof (not pure type checks)"
  - "var only for local variables where type obvious from RHS (new expressions)"
  - "Switch expression with default case to match original silent-ignore behavior"

patterns-established:
  - "Records for immutable value objects: Use records when object represents pure data with value semantics"
  - "Pattern matching instanceof: Always use when instanceof is followed by cast"
  - "Conservative var usage: Only use when type obvious from new/literal RHS"

# Metrics
duration: 8min
completed: 2026-02-05
---

# Phase 01 Plan 03: Java 21 Language Modernization Summary

**Vec and Cost converted to records, pattern matching for instanceof, switch expressions in Resources.add(), and var for obvious local types**

## Performance

- **Duration:** 8 min
- **Started:** 2026-02-05T18:49:18Z
- **Completed:** 2026-02-05T18:57:37Z
- **Tasks:** 3
- **Files modified:** 8

## Accomplishments
- Vec class converted to Java record with automatic equals/hashCode (value semantics)
- Cost record created for immutable building costs (satisfies LANG-02 requirement)
- HouseType and Resources refactored to use Cost record
- Pattern matching for instanceof in Peon.java, House.java, and TowerComponent.java
- Switch expression with arrow syntax in Resources.add()
- var declarations for obvious local types in Island.java

## Task Commits

Each task was committed atomically:

1. **Task 1: Convert Vec class to record** - `c3a2796` (feat)
2. **Task 2: Create Cost record and refactor HouseType** - `9a656cd` (feat)
3. **Task 3: Apply pattern matching, switch expressions, and var** - `b7b8c75` (feat)

## Files Created/Modified
- `src/main/java/com/mojang/tower/Vec.java` - Converted to record with accessor methods
- `src/main/java/com/mojang/tower/Cost.java` - New record for building costs
- `src/main/java/com/mojang/tower/HouseType.java` - Uses Cost record instead of separate fields
- `src/main/java/com/mojang/tower/Resources.java` - Uses Cost methods, switch expression in add()
- `src/main/java/com/mojang/tower/House.java` - Pattern matching instanceof, Cost accessor for sell()
- `src/main/java/com/mojang/tower/Peon.java` - Pattern matching for Monster instanceof
- `src/main/java/com/mojang/tower/TowerComponent.java` - Pattern matching for House instanceof
- `src/main/java/com/mojang/tower/Island.java` - var for local variables with new expressions

## Decisions Made
- Vec only used internally in Vec.java itself - no other files reference Vec class
- Cost record methods (canAfford, chargeFrom) encapsulate resource comparison/deduction logic
- Pattern matching applied only where instanceof is followed by cast (not pure type checks)
- Switch expression in add() includes default case to match original silent-ignore behavior
- var restricted to local variables where type is obvious from new expressions (not class fields)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] House.sell() accessed removed type.wood/rock fields**
- **Found during:** Task 2 (Cost record refactoring)
- **Issue:** House.sell() directly accessed type.wood and type.rock which were removed
- **Fix:** Updated to use type.cost.wood() and type.cost.rock() accessor methods
- **Files modified:** src/main/java/com/mojang/tower/House.java
- **Verification:** mvn compile succeeds
- **Committed in:** 9a656cd (Task 2 commit)

**2. [Rule 1 - Bug] var cannot be used on class fields**
- **Found during:** Task 3 (var application)
- **Issue:** Initially applied var to Island.java class fields (random, resources)
- **Fix:** Reverted to explicit types for class fields, kept var only for local variables
- **Files modified:** src/main/java/com/mojang/tower/Island.java
- **Verification:** mvn compile succeeds
- **Committed in:** b7b8c75 (Task 3 commit)

---

**Total deviations:** 2 auto-fixed (1 blocking, 1 bug)
**Impact on plan:** Both auto-fixes necessary for correct compilation. No scope creep.

## Issues Encountered
None - compilation passed after fixing deviations.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Java 21 language modernization complete
- Phase 1 (Foundation & Language Modernization) is now complete
- Ready for Phase 2 (Tower Mechanics) or Phase 3 (Movement Extraction)
- Note: Golden master snapshot should be generated before Phase 3 structural refactoring

---
*Phase: 01-foundation-language-modernization*
*Completed: 2026-02-05*
