package com.mojang.tower.pathfinding;

import com.mojang.tower.Entity;
import com.mojang.tower.TargetFilter;
import com.mojang.tower.navigation.NavigationGrid;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PathfindingService.
 *
 * Tests coordinate conversion between world space (continuous, -192 to +192)
 * and grid space (discrete, 0 to 95), and verifies pathfinding API works
 * correctly with world coordinates.
 */
class PathfindingServiceTest {

    @Test
    void worldToGrid_originMapsToCenter() {
        // World origin (0,0) is center of 384x384 world
        // Should map to approximately center of 96x96 grid
        // (0 + 192) / 4 = 48
        NavigationGrid allWalkable = createAllWalkableGrid();
        PathfindingService service = new PathfindingService(allWalkable);

        PathResult result = service.findPath(0, 0, 0, 0);

        assertInstanceOf(PathResult.Found.class, result);
        PathResult.Found found = (PathResult.Found) result;
        assertEquals(1, found.path().size());
        assertEquals(new GridCell(48, 48), found.path().get(0));
    }

    @Test
    void worldToGrid_boundsClamp() {
        // World coordinates outside -192 to +192 should clamp to grid bounds
        NavigationGrid allWalkable = createAllWalkableGrid();
        PathfindingService service = new PathfindingService(allWalkable);

        // -500 should clamp to grid cell 0
        // +500 should clamp to grid cell 95
        PathResult result = service.findPath(-500, -500, 500, 500);

        assertInstanceOf(PathResult.Found.class, result);
        PathResult.Found found = (PathResult.Found) result;
        // First cell should be (0,0) - clamped from -500
        assertEquals(new GridCell(0, 0), found.path().get(0));
        // Last cell should be (95,95) - clamped from 500
        assertEquals(new GridCell(95, 95), found.path().get(found.path().size() - 1));
    }

    @Test
    void gridToWorld_roundTrip() {
        // Converting grid to world and back should return same grid cell
        GridCell original = new GridCell(30, 70);

        double worldX = PathfindingService.gridToWorldX(original);
        double worldY = PathfindingService.gridToWorldY(original);

        // Convert back - world to grid
        // worldX = (30 * 4) - 192 + 2 = -70
        // gx = (-70 + 192) / 4 = 30.5 -> 30
        int gx = (int) ((worldX + 192) / 4);
        int gy = (int) ((worldY + 192) / 4);

        assertEquals(original.x(), gx);
        assertEquals(original.y(), gy);
    }

    @Test
    void findPath_worldCoordinates_returnsGridPath() {
        // Path from world coords returns list of GridCells
        NavigationGrid allWalkable = createAllWalkableGrid();
        PathfindingService service = new PathfindingService(allWalkable);

        // From near origin to a nearby point
        PathResult result = service.findPath(0, 0, 20, 20);

        assertInstanceOf(PathResult.Found.class, result);
        PathResult.Found found = (PathResult.Found) result;
        assertTrue(found.path().size() > 1, "Path should have multiple cells");

        // First cell should be near grid center (48, 48)
        GridCell start = found.path().get(0);
        assertEquals(48, start.x());
        assertEquals(48, start.y());
    }

    @Test
    void findPath_acrossMap_succeeds() {
        NavigationGrid allWalkable = createAllWalkableGrid();
        PathfindingService service = new PathfindingService(allWalkable);

        // From bottom-left to top-right of world
        PathResult result = service.findPath(-180, -180, 180, 180);

        assertInstanceOf(PathResult.Found.class, result);
        PathResult.Found found = (PathResult.Found) result;
        assertTrue(found.path().size() > 1, "Path across map should have multiple waypoints");

        // Verify diagonal path is efficient (should use diagonal moves)
        // From grid (3,3) to grid (93,93) - distance ~127 cells
        // With diagonal movement, should be around 90-100 steps (diagonal = 1 step)
        assertTrue(found.path().size() < 150,
            "Path should use diagonal movement, got " + found.path().size() + " steps");
    }

    @Test
    void performance_20PeonsPerTick_completesWithinBudget() {
        // PATH-04: Pathfinding completes within tick budget (no frame drops)
        // Tick budget at 60fps = 16.67ms, allow pathfinding up to ~8ms (half budget)
        NavigationGrid allWalkable = createAllWalkableGrid();
        PathfindingService service = new PathfindingService(allWalkable);

        // Warm-up run to avoid JIT compilation skew
        for (int i = 0; i < 5; i++) {
            service.findPath(-180, -180, 180, 180);
        }

        // Worst case: 20 peons all computing long paths across map
        long startTime = System.nanoTime();
        for (int i = 0; i < 20; i++) {
            // Random positions across the map
            double fromX = -180 + (i * 18);
            double fromY = -180 + (i * 9);
            double toX = 180 - (i * 18);
            double toY = 180 - (i * 9);
            service.findPath(fromX, fromY, toX, toY);
        }
        long endTime = System.nanoTime();
        long elapsedMs = (endTime - startTime) / 1_000_000;

        // Must complete in under 8ms to leave headroom for other tick work
        assertTrue(elapsedMs < 8, "20 pathfinding calls took " + elapsedMs + "ms, exceeds 8ms budget");
    }

    /**
     * Creates a mock NavigationGrid where all positions are walkable.
     */
    private NavigationGrid createAllWalkableGrid() {
        return new NavigationGrid() {
            @Override
            public boolean isOnGround(double x, double y) {
                return true;
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
}
