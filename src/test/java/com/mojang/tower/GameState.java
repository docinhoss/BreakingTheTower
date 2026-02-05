package com.mojang.tower;

import java.util.List;

/**
 * Immutable snapshot of complete game state at a specific tick.
 * Used for golden master testing to verify deterministic behavior.
 */
public record GameState(
    int tick,
    List<EntityState> entities,
    ResourceState resources,
    int population,
    int populationCap,
    int monsterPopulation,
    int warriorPopulation,
    int warriorPopulationCap,
    boolean titleScreen,
    boolean won
) {}
