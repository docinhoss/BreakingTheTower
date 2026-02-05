package com.mojang.tower;

/**
 * Immutable record representing building costs.
 * Replaces the wood/rock/food fields previously scattered in HouseType.
 */
public record Cost(int wood, int rock, int food) {

    /**
     * Check if resources can cover this cost.
     */
    public boolean canAfford(Resources resources) {
        return resources.wood >= wood
            && resources.rock >= rock
            && resources.food >= food;
    }

    /**
     * Deduct this cost from resources.
     */
    public void chargeFrom(Resources resources) {
        resources.wood -= wood;
        resources.rock -= rock;
        resources.food -= food;
    }
}
