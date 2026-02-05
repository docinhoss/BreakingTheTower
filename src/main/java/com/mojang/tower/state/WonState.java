package com.mojang.tower.state;

import com.mojang.tower.TowerComponent;

/**
 * Win screen state - shows win screen after tower destruction.
 *
 * Island keeps ticking (entities still animate), game time does not increment.
 * Auto-rotates the view. Click after 3 seconds transitions back to PlayingState.
 */
public final class WonState implements GameState {
    private static final double AUTO_ROTATE_DELTA = -0.002;
    private static final int CLICK_DELAY_TICKS = TowerComponent.TICKS_PER_SECOND * 3; // 3 seconds

    private int wonTime = 0;

    @Override
    public GameState tick() {
        wonTime++;
        return this;
    }

    @Override
    public GameState handleClick(int x, int y, int width, int height) {
        if (wonTime >= CLICK_DELAY_TICKS) {
            return new PlayingState();
        }
        return this;
    }

    @Override
    public boolean shouldTickIsland() {
        return true;
    }

    @Override
    public boolean shouldIncrementGameTime() {
        return false;
    }

    @Override
    public double getRotationDelta() {
        return AUTO_ROTATE_DELTA;
    }

    /**
     * @return the number of ticks since entering won state
     */
    public int getWonTime() {
        return wonTime;
    }
}
