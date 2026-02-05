package com.mojang.tower.event;

/**
 * Sealed interface for visual effect events.
 *
 * Decouples entity classes from direct Puff/InfoPuff creation.
 * TowerComponent subscribes to these events and creates the actual entities.
 */
public sealed interface EffectEvent permits PuffEffect, InfoPuffEffect {
}
