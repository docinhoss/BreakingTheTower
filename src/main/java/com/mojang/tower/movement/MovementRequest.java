package com.mojang.tower.movement;

import com.mojang.tower.Entity;

/**
 * Request for entity movement to a target position.
 * Contains the entity to move and the target coordinates.
 */
public record MovementRequest(
    Entity entity,
    double targetX,
    double targetY
) {
    /**
     * Factory: create request from entity moving in direction at speed.
     * Calculates target position based on current position, direction, and speed.
     */
    public static MovementRequest fromDirection(Entity entity, double direction, double speed) {
        double targetX = entity.x + Math.cos(direction) * speed;
        double targetY = entity.y + Math.sin(direction) * speed;
        return new MovementRequest(entity, targetX, targetY);
    }
}
