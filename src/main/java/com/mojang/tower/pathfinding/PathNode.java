package com.mojang.tower.pathfinding;

/**
 * Internal node for A* algorithm.
 * Mutable to allow g-score updates when better paths are found.
 *
 * Package-private, not part of public API.
 */
final class PathNode {
    private final GridCell cell;
    private int g;        // Cost from start
    private final int h;  // Heuristic to goal (constant once computed)
    private PathNode parent;
    private boolean closed;

    /**
     * Create a new path node.
     * @param cell grid coordinates
     * @param g cost from start node
     * @param h heuristic estimate to goal
     * @param parent previous node in path (null for start node)
     */
    PathNode(GridCell cell, int g, int h, PathNode parent) {
        this.cell = cell;
        this.g = g;
        this.h = h;
        this.parent = parent;
        this.closed = false;
    }

    GridCell cell() { return cell; }
    int g() { return g; }
    int f() { return g + h; }
    PathNode parent() { return parent; }
    boolean isClosed() { return closed; }
    void setClosed(boolean closed) { this.closed = closed; }

    /**
     * Update node with better path.
     * @param newG new g-score (must be lower than current)
     * @param newParent new parent node for reconstructing path
     */
    void update(int newG, PathNode newParent) {
        this.g = newG;
        this.parent = newParent;
    }
}
