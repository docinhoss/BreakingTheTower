package com.mojang.tower;

/**
 * Immutable snapshot of game resources at a specific tick.
 */
public record ResourceState(int wood, int rock, int food) {}
