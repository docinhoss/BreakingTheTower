package com.mojang.tower.state;

/**
 * Active gameplay state - player can place/sell buildings and interact.
 *
 * Island ticks, game time increments.
 * Rotation is controlled by mouse (no auto-rotate).
 * Win condition is handled externally via TowerComponent.win().
 */
public final class PlayingState implements GameState {

    @Override
    public GameState tick() {
        return this;
    }

    @Override
    public GameState handleClick(int x, int y, int width, int height) {
        // Building placement/sell is handled by TowerComponent after state check
        return this;
    }

    @Override
    public boolean shouldTickIsland() {
        return true;
    }

    @Override
    public boolean shouldIncrementGameTime() {
        return true;
    }

    @Override
    public double getRotationDelta() {
        return 0; // Player controls rotation via mouse
    }

    /**
     * Called when the game is won. Returns the WonState to transition to.
     * @return new WonState
     */
    public WonState win() {
        return new WonState();
    }
}
