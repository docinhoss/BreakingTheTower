---
phase: 01-foundation-language-modernization
plan: 01
subsystem: infra
tags: [maven, java21, build-infrastructure]

# Dependency graph
requires: []
provides:
  - Maven build configuration with Java 21
  - Standard Maven directory structure
  - JUnit 5 and Jackson test dependencies
  - Executable JAR with Main-Class manifest
affects: [01-02, 01-03, all-future-phases]

# Tech tracking
tech-stack:
  added: [Maven 3.9.x, JUnit 5.10.2, Jackson 2.16.1, maven-compiler-plugin 3.12.1, maven-surefire-plugin 3.2.5, maven-jar-plugin 3.3.0]
  patterns: [standard-maven-structure]

key-files:
  created:
    - pom.xml
    - src/main/java/com/mojang/tower/*.java
    - src/main/resources/*.gif
    - src/test/java/.gitkeep
    - src/test/resources/golden/.gitkeep
  modified: []

key-decisions:
  - "Use Maven over Gradle (simpler setup, consistent with CONTEXT.md)"
  - "Preserve original res/ folder for hot-reload development feature"
  - "Resources copied to both res/ and src/main/resources/ for development and packaging"

patterns-established:
  - "Maven build: mvn compile, mvn package -DskipTests"
  - "JAR execution: java -jar target/tower-1.0-SNAPSHOT.jar"

# Metrics
duration: 2min
completed: 2026-02-05
---

# Phase 01 Plan 01: Maven Build Infrastructure Summary

**Maven project with Java 21 compilation, JUnit 5/Jackson test dependencies, and executable JAR packaging**

## Performance

- **Duration:** 2 min
- **Started:** 2026-02-05T18:28:35Z
- **Completed:** 2026-02-05T18:30:34Z
- **Tasks:** 2
- **Files modified:** 30 (5 config + 21 Java + 4 resources)

## Accomplishments
- Created pom.xml with Java 21 source/target, JUnit 5 and Jackson test dependencies
- Migrated 21 Java source files to standard Maven structure (src/main/java/com/mojang/tower/)
- Copied 4 resource GIFs to src/main/resources/ for classpath loading
- Verified Maven compilation succeeds and JAR is executable

## Task Commits

Each task was committed atomically:

1. **Task 1: Create Maven project structure with pom.xml** - `093b75e` (chore)
2. **Task 2: Migrate source and resource files to Maven structure** - `f175291` (feat)

## Files Created/Modified
- `pom.xml` - Maven build configuration with Java 21, JUnit 5, Jackson, plugin configs
- `src/main/java/com/mojang/tower/*.java` - 21 source files in Maven standard structure
- `src/main/resources/*.gif` - 4 game assets (island.gif, logo.gif, sheet.gif, winscreen.gif)
- `src/test/java/.gitkeep` - Test source directory placeholder
- `src/test/resources/golden/.gitkeep` - Golden master snapshot directory placeholder

## Decisions Made
- Preserved original res/ folder alongside src/main/resources/ for hot-reload development feature
- Used .gitkeep files for empty test directories to ensure they're tracked in git

## Deviations from Plan
None - plan executed exactly as written.

## Issues Encountered
None - compilation succeeded on first attempt. The Bitmaps.java resource loading code (`getResource("/" + name)`) works correctly with Maven's classpath packaging.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Maven build infrastructure complete
- Ready for golden master test implementation (Plan 02)
- JUnit 5 and Jackson dependencies available in test scope
- Test directory structure ready for GoldenMasterTest.java

---
*Phase: 01-foundation-language-modernization*
*Completed: 2026-02-05*
