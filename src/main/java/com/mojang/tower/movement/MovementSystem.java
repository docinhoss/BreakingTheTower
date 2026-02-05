package com.mojang.tower.movement;

import com.mojang.tower.Entity;
import com.mojang.tower.navigation.NavigationGrid;

/**
 * Central service for entity movement execution.
 * Handles collision detection and position updates.
 * Entities calculate their movement intent; this system executes it.
 */
public final class MovementSystem {
    private NavigationGrid grid;

    /**
     * Set the NavigationGrid reference for collision detection.
     * Must be called before any move() calls.
     */
    public void setNavigationGrid(NavigationGrid grid) {
        this.grid = grid;
    }

    /**
     * Execute a movement request with collision detection.
     * If movement succeeds, updates entity position and returns Moved.
     * If blocked, position unchanged and returns Blocked with blocker reference.
     *
     * During Island construction (before setIsland is called), movement is
     * allowed without collision checking since entities are placed at
     * verified-free positions.
     *
     * @param request the movement request containing entity and target position
     * @return MovementResult indicating success or blocking entity
     */
    public MovementResult move(MovementRequest request) {
        Entity entity = request.entity();
        double targetX = request.targetX();
        double targetY = request.targetY();

        // During Island construction, grid reference not yet set.
        // Allow movement without collision check (entities placed at free positions).
        if (grid == null) {
            entity.x = targetX;
            entity.y = targetY;
            return new MovementResult.Moved(targetX, targetY);
        }

        if (grid.isFree(targetX, targetY, entity.r, entity)) {
            entity.x = targetX;
            entity.y = targetY;
            return new MovementResult.Moved(targetX, targetY);
        } else {
            Entity blocker = grid.getEntityAt(targetX, targetY, entity.r, null, entity);
            return new MovementResult.Blocked(blocker);
        }
    }
}
