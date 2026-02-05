package com.mojang.tower.event;

/**
 * Event for creating an info puff effect (level up indicator) at a location.
 */
public record InfoPuffEffect(double x, double y, int image) implements EffectEvent {
}
