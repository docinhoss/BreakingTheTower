package com.mojang.tower;

/**
 * Immutable snapshot of an entity's state at a specific tick.
 * Entities are sorted by (type, x, y) for stable ordering in snapshots.
 */
public record EntityState(
    String type,          // Entity class simple name (e.g., "Peon", "Monster", "House")
    double x,
    double y,
    double r,
    boolean alive,
    Integer hp,           // nullable for entities without HP
    String jobType,       // nullable, Job inner class name or null (for Peon only)
    Integer carrying      // nullable, resource being carried (for Peon only)
) implements Comparable<EntityState> {

    @Override
    public int compareTo(EntityState other) {
        int typeCompare = this.type.compareTo(other.type);
        if (typeCompare != 0) return typeCompare;

        int xCompare = Double.compare(this.x, other.x);
        if (xCompare != 0) return xCompare;

        return Double.compare(this.y, other.y);
    }
}
