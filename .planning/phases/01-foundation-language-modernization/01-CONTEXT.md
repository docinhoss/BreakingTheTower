# Phase 1: Foundation & Language Modernization - Context

**Gathered:** 2026-02-05
**Status:** Ready for planning

<domain>
## Phase Boundary

Establish safety infrastructure (golden master tests) and modernize Java syntax from 1.6 to 21 without changing architecture. The golden master captures exact gameplay behavior as a safety net before any refactoring. Syntax modernization adopts records, pattern matching, var, and switch expressions where they improve clarity.

</domain>

<decisions>
## Implementation Decisions

### Golden master scope
- Capture full entity state + RNG state each tick (positions, resources, health, job state, movement targets, RNG seed)
- Run until win condition — full game playthrough for complete coverage
- Capture every tick (no sampling) — maximum precision, pinpoints exact divergence
- Fixed seed at game start for determinism — assumes no external randomness sources

### Test infrastructure
- JUnit 5 for test framework
- JSON format for snapshots — human-readable, easy to diff
- Store snapshots in src/test/resources/ — standard Maven location, committed to git
- Maven as build tool — simpler setup, good Java 21 support

### Modernization boundaries
- Conservative var usage — only where type is obvious from right-hand side (new, literals, factory methods)
- Whole file sweep — when touching a file, modernize all applicable patterns in it
- Text blocks replace all multi-line string concatenation

### Migration safety
- Compile check after each file change — catches issues immediately
- One file per commit — maximum granularity, easy to revert specific changes
- Golden master tests BEFORE syntax modernization — safety net in place first
- Debug and fix forward on golden master breaks — investigate diff, don't revert blindly

### Claude's Discretion
- Switch expression conversion — judge case-by-case what reads cleaner (value-returning vs void switches)
- Exact JSON snapshot structure
- Maven project structure details

</decisions>

<specifics>
## Specific Ideas

No specific requirements — open to standard approaches

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 01-foundation-language-modernization*
*Context gathered: 2026-02-05*
