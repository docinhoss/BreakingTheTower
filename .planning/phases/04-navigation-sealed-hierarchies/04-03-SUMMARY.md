# Phase 04 Plan 03: Seal Job Class Summary

**One-liner:** Sealed Job class with 6 final nested subclasses enabling exhaustive pattern matching

## What Was Done

### Task 1: Seal Job class with permits clause
- Added `sealed` modifier to Job class declaration
- Added `permits` clause listing all 6 nested subclasses:
  - Job.Goto, Job.GotoAndConvert, Job.Hunt, Job.Build, Job.Plant, Job.Gather
- Job remains a class (not converted to interface) to preserve shared mutable state

### Task 2: Mark all 6 nested Job subclasses as final
- Added `final` modifier to all 6 nested static class declarations:
  - `public static final class Goto extends Job`
  - `public static final class GotoAndConvert extends Job`
  - `public static final class Hunt extends Job`
  - `public static final class Build extends Job`
  - `public static final class Plant extends Job`
  - `public static final class Gather extends Job`

## Key Changes

**File Modified:** `src/main/java/com/mojang/tower/Job.java`

**Before:**
```java
public class Job
{
    public static class Goto extends Job { ... }
    public static class GotoAndConvert extends Job { ... }
    // etc.
}
```

**After:**
```java
public sealed class Job
    permits Job.Goto, Job.GotoAndConvert, Job.Hunt, Job.Build, Job.Plant, Job.Gather
{
    public static final class Goto extends Job { ... }
    public static final class GotoAndConvert extends Job { ... }
    // etc.
}
```

## Decisions Made

| Decision | Rationale |
|----------|-----------|
| Keep Job as sealed class (not interface) | Nested subclasses share mutable state (peon, island, target, boreTime, bonusRadius) from parent class |
| All subclasses marked final | No further subclassing needed; enables compiler to enforce exhaustive pattern matching |
| Single commit for both tasks | Tasks are interdependent (sealed without final doesn't compile) |

## Verification Results

- `mvn compile -q`: Passed (sealed hierarchy compiles)
- `mvn test -q`: All tests pass (behavior unchanged)
- Job.java contains "sealed class Job" and permits clause
- All 6 nested subclasses have "final" modifier

## Deviations from Plan

None - plan executed exactly as written.

## Commits

| Hash | Type | Description |
|------|------|-------------|
| 5ca2f5f | feat | seal Job class with final nested subclasses |

## Duration

Approximately 2 minutes

## Next Phase Readiness

Plan 04-03 completes the Job sealed hierarchy. Future code can use exhaustive switch expressions on Job instances, with the compiler ensuring all subtypes are handled.
