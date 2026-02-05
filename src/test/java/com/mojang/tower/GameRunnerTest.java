package com.mojang.tower;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sanity tests for GameRunner headless execution.
 */
class GameRunnerTest {

    @Test
    void runnerExecutesWithoutError() {
        // Run for a small number of ticks to verify basic operation
        List<GameState> states = GameRunner.runDeterministicGame(100);

        assertNotNull(states, "States should not be null");
        assertFalse(states.isEmpty(), "Should capture at least one state");

        // First tick should have initial game state
        GameState first = states.get(0);
        assertEquals(0, first.tick(), "First state should be tick 0");
        assertTrue(first.population() > 0, "Should have initial population");
        assertFalse(first.entities().isEmpty(), "Should have entities");
    }

    @Test
    void runnerProducesDeterministicResults() {
        // Run twice with same parameters
        List<GameState> run1 = GameRunner.runDeterministicGame(50);
        List<GameState> run2 = GameRunner.runDeterministicGame(50);

        assertEquals(run1.size(), run2.size(), "Both runs should produce same number of states");

        // Compare first few states
        for (int i = 0; i < Math.min(10, run1.size()); i++) {
            GameState s1 = run1.get(i);
            GameState s2 = run2.get(i);

            assertEquals(s1.tick(), s2.tick(), "Tick numbers should match at " + i);
            assertEquals(s1.population(), s2.population(), "Population should match at tick " + i);
            assertEquals(s1.resources(), s2.resources(), "Resources should match at tick " + i);
            assertEquals(s1.entities().size(), s2.entities().size(),
                "Entity count should match at tick " + i);
        }
    }

    @Test
    void capturesEntityState() {
        List<GameState> states = GameRunner.runDeterministicGame(10);
        GameState state = states.get(0);

        // Should have various entity types from initial setup
        boolean hasPeon = state.entities().stream().anyMatch(e -> e.type().equals("Peon"));
        boolean hasTower = state.entities().stream().anyMatch(e -> e.type().equals("Tower"));
        boolean hasHouse = state.entities().stream().anyMatch(e -> e.type().equals("House"));
        boolean hasTree = state.entities().stream().anyMatch(e -> e.type().equals("Tree"));

        assertTrue(hasPeon, "Should have Peon entities");
        assertTrue(hasTower, "Should have Tower entity");
        assertTrue(hasHouse, "Should have House entity (initial guardpost)");
        assertTrue(hasTree, "Should have Tree entities");
    }
}
