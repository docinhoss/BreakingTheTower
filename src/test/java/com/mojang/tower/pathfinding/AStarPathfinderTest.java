package com.mojang.tower.pathfinding;

import com.mojang.tower.Entity;
import com.mojang.tower.TargetFilter;
import com.mojang.tower.navigation.NavigationGrid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for A* pathfinding algorithm.
 */
class AStarPathfinderTest {

    private static final int GRID_SIZE = 10; // Small grid for testing

    /**
     * Create a mock NavigationGrid backed by a boolean array.
     * @param walkable 2D array where true = walkable, false = blocked
     */
    private static NavigationGrid createGrid(boolean[][] walkable) {
        return new NavigationGrid() {
            @Override
            public boolean isOnGround(double x, double y) {
                // Convert world to grid: worldX = (gx * 4) - 192 + 2
                // Solve for gx: gx = (worldX + 192 - 2) / 4 = (worldX + 190) / 4
                int gx = (int) ((x + 190) / 4);
                int gy = (int) ((y + 190) / 4);
                if (gx < 0 || gx >= walkable.length) return false;
                if (gy < 0 || gy >= walkable[0].length) return false;
                return walkable[gx][gy];
            }

            @Override
            public boolean isFree(double x, double y, double radius, Entity exclude) {
                throw new UnsupportedOperationException("Not needed for pathfinding tests");
            }

            @Override
            public Entity getEntityAt(double x, double y, double radius, TargetFilter filter, Entity exclude) {
                throw new UnsupportedOperationException("Not needed for pathfinding tests");
            }
        };
    }

    /**
     * Create a fully walkable grid of the given size.
     */
    private static boolean[][] openGrid(int size) {
        boolean[][] grid = new boolean[size][size];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                grid[x][y] = true;
            }
        }
        return grid;
    }

    private AStarPathfinder pathfinder;
    private boolean[][] testGrid;

    @BeforeEach
    void setUp() {
        testGrid = openGrid(GRID_SIZE);
        pathfinder = new AStarPathfinder(createGrid(testGrid));
    }

    @Test
    void findPath_straightLine_returnsOptimalPath() {
        // Path from (0,0) to (5,0) should be 6 cells (0,1,2,3,4,5)
        PathResult result = pathfinder.findPath(new GridCell(0, 0), new GridCell(5, 0), AStarPathfinder.DEFAULT_MAX_NODES);

        assertInstanceOf(PathResult.Found.class, result);
        PathResult.Found found = (PathResult.Found) result;
        assertEquals(6, found.path().size(), "Straight line of 5 steps = 6 cells");
        assertEquals(new GridCell(0, 0), found.path().get(0));
        assertEquals(new GridCell(5, 0), found.path().get(5));
    }

    @Test
    void findPath_aroundObstacle_findsRoute() {
        // Create wall from (2,0) to (2,4)
        for (int y = 0; y <= 4; y++) {
            testGrid[2][y] = false;
        }
        pathfinder = new AStarPathfinder(createGrid(testGrid));

        // Path from (0,2) to (4,2) should go around the wall
        PathResult result = pathfinder.findPath(new GridCell(0, 2), new GridCell(4, 2), AStarPathfinder.DEFAULT_MAX_NODES);

        assertInstanceOf(PathResult.Found.class, result);
        PathResult.Found found = (PathResult.Found) result;

        // Verify path doesn't go through wall
        for (GridCell cell : found.path()) {
            if (cell.x() == 2 && cell.y() >= 0 && cell.y() <= 4) {
                fail("Path went through wall at " + cell);
            }
        }

        // Path should exist and have reasonable length (going around)
        assertTrue(found.path().size() > 5, "Path around obstacle should be longer than 5 cells");
    }

    @Test
    void findPath_diagonal_shortestPath() {
        // Path from (0,0) to (3,3) should use diagonals
        // Optimal diagonal path: (0,0) -> (1,1) -> (2,2) -> (3,3) = 4 cells
        // Cardinal-only would be 7 cells: 3 right + 3 down + start
        PathResult result = pathfinder.findPath(new GridCell(0, 0), new GridCell(3, 3), AStarPathfinder.DEFAULT_MAX_NODES);

        assertInstanceOf(PathResult.Found.class, result);
        PathResult.Found found = (PathResult.Found) result;
        assertEquals(4, found.path().size(), "Diagonal path should be 4 cells (not 7 cardinal)");
    }

    @Test
    void findPath_noPath_returnsNotFound() {
        // Completely surround target with walls
        // Target at (5,5), walls at all 8 neighbors
        int[][] neighbors = {{-1,-1},{0,-1},{1,-1},{-1,0},{1,0},{-1,1},{0,1},{1,1}};
        for (int[] n : neighbors) {
            testGrid[5 + n[0]][5 + n[1]] = false;
        }
        pathfinder = new AStarPathfinder(createGrid(testGrid));

        PathResult result = pathfinder.findPath(new GridCell(0, 0), new GridCell(5, 5), AStarPathfinder.DEFAULT_MAX_NODES);

        assertInstanceOf(PathResult.NotFound.class, result);
        PathResult.NotFound notFound = (PathResult.NotFound) result;
        assertNotNull(notFound.reason());
    }

    @Test
    void findPath_startEqualsGoal_returnsSingleCell() {
        PathResult result = pathfinder.findPath(new GridCell(3, 3), new GridCell(3, 3), AStarPathfinder.DEFAULT_MAX_NODES);

        assertInstanceOf(PathResult.Found.class, result);
        PathResult.Found found = (PathResult.Found) result;
        assertEquals(1, found.path().size());
        assertEquals(new GridCell(3, 3), found.path().get(0));
    }

    @Test
    void findPath_startNotWalkable_returnsNotFound() {
        testGrid[0][0] = false;
        pathfinder = new AStarPathfinder(createGrid(testGrid));

        PathResult result = pathfinder.findPath(new GridCell(0, 0), new GridCell(5, 5), AStarPathfinder.DEFAULT_MAX_NODES);

        assertInstanceOf(PathResult.NotFound.class, result);
        PathResult.NotFound notFound = (PathResult.NotFound) result;
        assertTrue(notFound.reason().toLowerCase().contains("start"));
    }

    @Test
    void findPath_deterministic_sameResultEveryTime() {
        // Run same pathfind 100 times, verify all paths identical
        List<GridCell> firstPath = null;

        for (int i = 0; i < 100; i++) {
            // Create fresh pathfinder each time to ensure no state leakage
            AStarPathfinder fresh = new AStarPathfinder(createGrid(testGrid));
            PathResult result = fresh.findPath(new GridCell(0, 0), new GridCell(7, 5), AStarPathfinder.DEFAULT_MAX_NODES);

            assertInstanceOf(PathResult.Found.class, result);
            PathResult.Found found = (PathResult.Found) result;

            if (firstPath == null) {
                firstPath = found.path();
            } else {
                assertEquals(firstPath, found.path(),
                    "Path should be identical on iteration " + i);
            }
        }
    }

    @Test
    void findPath_diagonalCostsMore_prefersCardinal() {
        // When straight path exists, should not take unnecessary diagonals
        // Path from (0,0) to (4,0) - pure horizontal movement
        PathResult result = pathfinder.findPath(new GridCell(0, 0), new GridCell(4, 0), AStarPathfinder.DEFAULT_MAX_NODES);

        assertInstanceOf(PathResult.Found.class, result);
        PathResult.Found found = (PathResult.Found) result;

        // All cells should have y=0 (no vertical deviation)
        for (GridCell cell : found.path()) {
            assertEquals(0, cell.y(), "Horizontal path should not deviate vertically");
        }
    }
}
