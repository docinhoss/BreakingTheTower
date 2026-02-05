package com.mojang.tower.navigation;

import com.mojang.tower.Entity;
import com.mojang.tower.TargetFilter;

/**
 * Interface for world navigation queries.
 * Abstracts walkability and collision detection for movement and pathfinding systems.
 */
public interface NavigationGrid {
    /**
     * Check if a position is on valid ground (within bounds, on solid terrain).
     */
    boolean isOnGround(double x, double y);

    /**
     * Check if a circular area is free for movement.
     * @param exclude entity to exclude from collision check (null to check all)
     */
    boolean isFree(double x, double y, double radius, Entity exclude);

    /**
     * Find entity at a position matching an optional filter.
     * @param filter entity filter (null accepts all entities)
     * @param exclude entity to exclude from search (null to search all)
     * @return closest matching entity, or null if none found
     */
    Entity getEntityAt(double x, double y, double radius, TargetFilter filter, Entity exclude);
}
