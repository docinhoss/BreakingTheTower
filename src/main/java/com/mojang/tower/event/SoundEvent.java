package com.mojang.tower.event;

/**
 * Sealed interface for all sound events.
 *
 * Each record represents a trigger for a specific sound effect.
 * Events are data-only - the actual Sound objects are created in the handler.
 */
public sealed interface SoundEvent permits
    AbandonedTargetSound,
    SelectSound,
    PlantSound,
    DestroySound,
    GatherSound,
    FinishBuildingSound,
    SpawnSound,
    SpawnWarriorSound,
    DingSound,
    DeathSound,
    MonsterDeathSound,
    WinSound {
}
