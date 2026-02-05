package com.mojang.tower.event;

/**
 * Event for creating a smoke puff effect at a location.
 */
public record PuffEffect(double x, double y) implements EffectEvent {
}
