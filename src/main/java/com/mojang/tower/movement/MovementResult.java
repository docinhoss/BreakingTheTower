package com.mojang.tower.movement;

import com.mojang.tower.Entity;

/**
 * Result of a movement attempt.
 * Sealed interface enables exhaustive pattern matching in switch expressions.
 */
public sealed interface MovementResult permits MovementResult.Moved, MovementResult.Blocked {
    /**
     * Movement succeeded. Entity position has been updated.
     */
    record Moved(double x, double y) implements MovementResult {}

    /**
     * Movement blocked by collision. Entity position unchanged.
     * @param blocker the blocking entity, or null if blocked by terrain/boundary
     */
    record Blocked(Entity blocker) implements MovementResult {}
}
