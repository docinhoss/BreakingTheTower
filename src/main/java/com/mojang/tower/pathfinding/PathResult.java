package com.mojang.tower.pathfinding;

import java.util.List;

/**
 * Result of a pathfinding query.
 * Either a path was found, or the target is unreachable.
 *
 * Follows the sealed interface pattern established by MovementResult.
 */
public sealed interface PathResult {
    /**
     * Path successfully found.
     * @param path ordered list of grid cells from start to goal (inclusive)
     */
    record Found(List<GridCell> path) implements PathResult {}

    /**
     * No valid path exists.
     * @param reason human-readable explanation of why path wasn't found
     */
    record NotFound(String reason) implements PathResult {}
}
