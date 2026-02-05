package com.mojang.tower.state;

/**
 * Sealed interface for game state machine.
 *
 * States control game behavior such as rotation, island ticking, and game time.
 * States return new states from tick() and handleClick() to signal transitions.
 * TowerComponent manages the actual transition via transitionTo().
 */
public sealed interface GameState permits TitleState, PlayingState, WonState {
    /**
     * Called each game tick. May return a new state to transition to.
     * @return this state or a new state to transition to
     */
    GameState tick();

    /**
     * Handle mouse click. May return a new state to transition to.
     * @param x mouse x coordinate
     * @param y mouse y coordinate
     * @param width game width
     * @param height game height
     * @return this state or a new state to transition to
     */
    GameState handleClick(int x, int y, int width, int height);

    /**
     * @return true if the island should tick (update entities)
     */
    boolean shouldTickIsland();

    /**
     * @return true if game time should increment
     */
    boolean shouldIncrementGameTime();

    /**
     * @return rotation delta to apply each tick (for auto-rotate)
     */
    double getRotationDelta();

    /**
     * Called when entering this state.
     */
    default void onEnter() {}

    /**
     * Called when exiting this state.
     */
    default void onExit() {}
}
