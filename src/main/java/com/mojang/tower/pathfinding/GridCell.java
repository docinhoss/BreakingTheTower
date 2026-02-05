package com.mojang.tower.pathfinding;

/**
 * Represents a discrete cell in the pathfinding grid.
 * Immutable value type suitable for use as HashMap key.
 *
 * The 96x96 grid maps to world coordinates -192 to +192 (4 world units per cell).
 */
public record GridCell(int x, int y) {
    /**
     * Grid dimension (96x96 cells covering 384x384 world units).
     */
    public static final int GRID_SIZE = 96;

    /**
     * Check if this cell is within the valid grid bounds.
     * @return true if coordinates are in range [0, GRID_SIZE)
     */
    public boolean isValid() {
        return x >= 0 && x < GRID_SIZE && y >= 0 && y < GRID_SIZE;
    }
}
