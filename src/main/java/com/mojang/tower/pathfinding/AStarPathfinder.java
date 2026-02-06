package com.mojang.tower.pathfinding;

import com.mojang.tower.navigation.NavigationGrid;

import java.util.*;

/**
 * A* pathfinding algorithm implementation with deterministic behavior.
 *
 * Features:
 * - 8-directional movement (cardinal + diagonal)
 * - Integer costs (10 cardinal, 14 diagonal) for determinism
 * - Octile heuristic (admissible for 8-directional movement)
 * - Deterministic tie-breaking comparator
 * - LinkedHashMap for consistent iteration order
 */
public final class AStarPathfinder {
    private static final int D = 10;   // Cardinal movement cost
    private static final int D2 = 14;  // Diagonal movement cost (approximates 10 * sqrt(2))
    /** Default node limit to prevent unbounded search */
    public static final int DEFAULT_MAX_NODES = 1024;

    // 8 directions: N, NE, E, SE, S, SW, W, NW
    private static final int[][] DIRECTIONS = {
        {0, -1}, {1, -1}, {1, 0}, {1, 1},
        {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}
    };

    private final NavigationGrid grid;

    /**
     * Create a pathfinder using the given navigation grid for walkability queries.
     * @param grid navigation grid to check terrain walkability
     */
    public AStarPathfinder(NavigationGrid grid) {
        this.grid = grid;
    }

    /**
     * Find optimal path from start to goal using A* algorithm.
     * @param start starting grid cell
     * @param goal target grid cell
     * @param maxNodes maximum nodes to explore before giving up
     * @return PathResult.Found with path, or PathResult.NotFound with reason
     */
    public PathResult findPath(GridCell start, GridCell goal, int maxNodes) {
        // Early termination: check start/goal validity
        if (!isWalkable(start)) {
            return new PathResult.NotFound("Start not walkable");
        }
        if (!isWalkable(goal)) {
            return new PathResult.NotFound("Goal not walkable");
        }
        if (start.equals(goal)) {
            return new PathResult.Found(List.of(start));
        }

        // Deterministic comparator: f, then g (descending), then x, then y
        // This ensures same path is found regardless of insertion order
        Comparator<PathNode> cmp = Comparator
            .comparingInt(PathNode::f)
            .thenComparing(Comparator.comparingInt(PathNode::g).reversed())
            .thenComparingInt(n -> n.cell().x())
            .thenComparingInt(n -> n.cell().y());

        PriorityQueue<PathNode> openSet = new PriorityQueue<>(cmp);
        Map<GridCell, PathNode> allNodes = new LinkedHashMap<>(); // Deterministic iteration order

        PathNode startNode = new PathNode(start, 0, heuristic(start, goal), null);
        openSet.add(startNode);
        allNodes.put(start, startNode);

        int nodesExplored = 0;

        while (!openSet.isEmpty() && nodesExplored < maxNodes) {
            PathNode current = openSet.poll();
            nodesExplored++;

            if (current.cell().equals(goal)) {
                return new PathResult.Found(reconstructPath(current));
            }

            current.setClosed(true);

            for (int[] dir : DIRECTIONS) {
                int nx = current.cell().x() + dir[0];
                int ny = current.cell().y() + dir[1];
                GridCell neighbor = new GridCell(nx, ny);

                if (!neighbor.isValid() || !isWalkable(neighbor)) {
                    continue;
                }

                PathNode neighborNode = allNodes.get(neighbor);
                if (neighborNode != null && neighborNode.isClosed()) {
                    continue;
                }

                // Diagonal costs 14, cardinal costs 10
                int moveCost = (dir[0] != 0 && dir[1] != 0) ? D2 : D;
                int tentativeG = current.g() + moveCost;

                if (neighborNode == null) {
                    neighborNode = new PathNode(neighbor, tentativeG, heuristic(neighbor, goal), current);
                    allNodes.put(neighbor, neighborNode);
                    openSet.add(neighborNode);
                } else if (tentativeG < neighborNode.g()) {
                    // Found better path - update node
                    openSet.remove(neighborNode);
                    neighborNode.update(tentativeG, current);
                    openSet.add(neighborNode);
                }
            }
        }

        return new PathResult.NotFound("No path found (explored " + nodesExplored + " nodes)");
    }

    /**
     * Octile heuristic for 8-directional movement.
     * Admissible and consistent for grids with diagonal movement.
     */
    private int heuristic(GridCell from, GridCell to) {
        int dx = Math.abs(from.x() - to.x());
        int dy = Math.abs(from.y() - to.y());
        // Octile distance: D * max(dx, dy) + (D2 - D) * min(dx, dy)
        // Simplified: D * (dx + dy) + (D2 - 2*D) * min(dx, dy)
        return D * (dx + dy) + (D2 - 2 * D) * Math.min(dx, dy);
    }

    /**
     * Check if a grid cell is walkable terrain.
     * Converts grid coordinates to world coordinates (center of cell).
     */
    private boolean isWalkable(GridCell cell) {
        // Grid cell (x, y) maps to world coordinates:
        // worldX = (x * 4) - 192 + 2 (center of cell)
        // worldY = (y * 4) - 192 + 2 (center of cell)
        double worldX = (cell.x() * 4.0) - 192 + 2;
        double worldY = (cell.y() * 4.0) - 192 + 2;
        return grid.isOnGround(worldX, worldY);
    }

    /**
     * Reconstruct path from goal node back to start.
     * @param goal the goal node (with parent chain)
     * @return list of grid cells from start to goal (inclusive)
     */
    private List<GridCell> reconstructPath(PathNode goal) {
        List<GridCell> path = new ArrayList<>();
        PathNode current = goal;
        while (current != null) {
            path.add(current.cell());
            current = current.parent();
        }
        Collections.reverse(path);
        return path;
    }
}
