package com.mojang.tower.state;

/**
 * Title screen state - shows logo and "click to start".
 *
 * Island does not tick, game time does not increment.
 * Auto-rotates the view. Any click transitions to PlayingState.
 */
public final class TitleState implements GameState {
    private static final double AUTO_ROTATE_DELTA = -0.002;

    @Override
    public GameState tick() {
        return this;
    }

    @Override
    public GameState handleClick(int x, int y, int width, int height) {
        return new PlayingState();
    }

    @Override
    public boolean shouldTickIsland() {
        return false;
    }

    @Override
    public boolean shouldIncrementGameTime() {
        return false;
    }

    @Override
    public double getRotationDelta() {
        return AUTO_ROTATE_DELTA;
    }
}
