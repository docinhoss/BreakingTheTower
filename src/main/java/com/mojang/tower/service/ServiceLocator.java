package com.mojang.tower.service;

import com.mojang.tower.movement.MovementSystem;
import com.mojang.tower.pathfinding.PathfindingService;

/**
 * Central registry for game services.
 *
 * Services are registered at startup and can be accessed statically.
 * For testing, services can be swapped via provide() and reset via reset().
 */
public final class ServiceLocator {
    private static AudioService audioService;
    private static MovementSystem movementSystem;
    private static PathfindingService pathfindingService;

    private ServiceLocator() {
        // Static utility class
    }

    /**
     * Register an AudioService implementation.
     *
     * @param service the service to register
     */
    public static void provide(AudioService service) {
        audioService = service;
    }

    /**
     * Get the registered AudioService.
     *
     * @return the audio service
     * @throws IllegalStateException if no service has been registered
     */
    public static AudioService audio() {
        if (audioService == null) {
            throw new IllegalStateException("AudioService not initialized. Call ServiceLocator.initializeDefaults() first.");
        }
        return audioService;
    }

    /**
     * Register a MovementSystem implementation.
     *
     * @param service the service to register
     */
    public static void provide(MovementSystem service) {
        movementSystem = service;
    }

    /**
     * Get the registered MovementSystem.
     *
     * @return the movement system
     * @throws IllegalStateException if no service has been registered
     */
    public static MovementSystem movement() {
        if (movementSystem == null) {
            throw new IllegalStateException("MovementSystem not initialized. Call ServiceLocator.provide(MovementSystem) first.");
        }
        return movementSystem;
    }

    /**
     * Register a PathfindingService implementation.
     *
     * @param service the service to register
     */
    public static void provide(PathfindingService service) {
        pathfindingService = service;
    }

    /**
     * Get the registered PathfindingService.
     *
     * @return the pathfinding service
     * @throws IllegalStateException if no service has been registered
     */
    public static PathfindingService pathfinding() {
        if (pathfindingService == null) {
            throw new IllegalStateException("PathfindingService not initialized.");
        }
        return pathfindingService;
    }

    /**
     * Initialize default service implementations.
     * Call this at application startup.
     */
    public static void initializeDefaults() {
        provide(new SoundsAdapter());
    }

    /**
     * Clear all registered services.
     * Primarily for testing to ensure clean state between tests.
     */
    public static void reset() {
        audioService = null;
        movementSystem = null;
        pathfindingService = null;
    }
}
