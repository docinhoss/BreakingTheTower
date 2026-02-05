package com.mojang.tower.pathfinding;

import com.mojang.tower.navigation.NavigationGrid;

/**
 * Public facade for pathfinding.
 *
 * Converts between world coordinates (continuous) and grid coordinates (discrete)
 * and delegates to the internal AStarPathfinder for path computation.
 */
public final class PathfindingService {
    private final AStarPathfinder pathfinder;

    public PathfindingService(NavigationGrid grid) {
        this.pathfinder = new AStarPathfinder(grid);
    }

    /**
     * Find a path from world coordinates to target world coordinates.
     * Converts between continuous world space and discrete grid cells.
     *
     * @return PathResult.Found with waypoints (in grid cells), or PathResult.NotFound
     */
    public PathResult findPath(double fromX, double fromY, double toX, double toY) {
        GridCell start = worldToGrid(fromX, fromY);
        GridCell goal = worldToGrid(toX, toY);
        return pathfinder.findPath(start, goal);
    }

    /**
     * Convert grid cell to world coordinates (center of cell).
     */
    public static double gridToWorldX(GridCell cell) {
        return (cell.x() * 4.0) - 192 + 2;
    }

    /**
     * Convert grid cell to world coordinates (center of cell).
     */
    public static double gridToWorldY(GridCell cell) {
        return (cell.y() * 4.0) - 192 + 2;
    }

    /**
     * Convert world coordinates to grid cell.
     * Clamps to valid grid bounds.
     */
    private GridCell worldToGrid(double x, double y) {
        int gx = (int) ((x + 192) / 4);
        int gy = (int) ((y + 192) / 4);
        return new GridCell(
            Math.clamp(gx, 0, GridCell.GRID_SIZE - 1),
            Math.clamp(gy, 0, GridCell.GRID_SIZE - 1)
        );
    }
}
