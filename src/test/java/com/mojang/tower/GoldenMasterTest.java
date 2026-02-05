package com.mojang.tower;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Golden master test for verifying game behavior remains consistent.
 *
 * <p>This test captures tick-by-tick game state and compares against an approved
 * baseline snapshot. Any behavioral change will cause the test to fail, immediately
 * identifying regressions during refactoring.
 *
 * <p>The test uses a deterministic seed (8844) to ensure reproducible results.
 * All Random instances in entities and jobs are seeded for consistency.
 *
 * <h3>Usage:</h3>
 * <ul>
 *   <li>First run: Generates snapshot and fails with instructions to review</li>
 *   <li>Subsequent runs: Compares against approved snapshot</li>
 *   <li>To regenerate: Delete the snapshot file and re-run</li>
 * </ul>
 *
 * <h3>Snapshot Location:</h3>
 * <p>{@code src/test/resources/golden/full-game-snapshot.json}
 *
 * <h3>Performance Note:</h3>
 * <p>The test runs until win condition or max 50000 ticks. A typical game completes
 * in several thousand ticks. The snapshot captures every tick for maximum precision
 * in detecting behavioral changes.
 */
class GoldenMasterTest {

    /**
     * Maximum ticks for golden master capture.
     * Set lower for faster CI; increase for more complete coverage.
     */
    private static final int MAX_TICKS = 5000;

    private static final Path GOLDEN_PATH =
        Path.of("src/test/resources/golden/full-game-snapshot.json");

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    void fullGameplayMatchesGoldenMaster() throws Exception {
        // Run deterministic game simulation
        List<GameState> actualStates = GameRunner.runDeterministicGame(MAX_TICKS);

        if (Files.exists(GOLDEN_PATH)) {
            // Compare mode - verify against approved snapshot
            List<GameState> expectedStates = MAPPER.readValue(
                GOLDEN_PATH.toFile(),
                new TypeReference<List<GameState>>() {}
            );

            assertEquals(expectedStates.size(), actualStates.size(),
                "Game length changed - expected " + expectedStates.size() +
                " ticks but got " + actualStates.size() +
                ". Delete snapshot to regenerate if this is intentional.");

            for (int i = 0; i < expectedStates.size(); i++) {
                GameState expected = expectedStates.get(i);
                GameState actual = actualStates.get(i);

                assertEquals(expected, actual,
                    "State diverged at tick " + i +
                    ".\n\nExpected: " + formatState(expected) +
                    "\n\nActual: " + formatState(actual) +
                    "\n\nTo update the golden master, delete the snapshot file and re-run the test.");
            }
        } else {
            // Initial capture mode - generate snapshot
            Files.createDirectories(GOLDEN_PATH.getParent());
            MAPPER.writeValue(GOLDEN_PATH.toFile(), actualStates);

            fail("Golden master snapshot created at " + GOLDEN_PATH + " (" +
                 actualStates.size() + " ticks captured).\n\n" +
                 "Please review the snapshot and re-run the test to verify it passes.\n" +
                 "The snapshot should be committed to git as the approved baseline.");
        }
    }

    /**
     * Formats a GameState for readable error messages.
     */
    private String formatState(GameState state) {
        return String.format(
            "tick=%d, entities=%d, pop=%d/%d, monsters=%d, resources=%s, won=%s",
            state.tick(),
            state.entities().size(),
            state.population(),
            state.populationCap(),
            state.monsterPopulation(),
            state.resources(),
            state.won()
        );
    }
}
