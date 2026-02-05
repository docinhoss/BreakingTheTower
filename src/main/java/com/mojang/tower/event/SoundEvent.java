package com.mojang.tower.event;

/**
 * Sealed interface for all sound events.
 *
 * Each record represents a trigger for a specific sound effect.
 * Events are data-only - the actual Sound objects are created in the handler.
 */
public sealed interface SoundEvent permits
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

/** Triggered when player selects a building type in the UI. */
record SelectSound() implements SoundEvent {}

/** Triggered when placing a new building or planting a tree. */
record PlantSound() implements SoundEvent {}

/** Triggered when a building is destroyed. */
record DestroySound() implements SoundEvent {}

/** Triggered when a peon deposits a gathered resource. */
record GatherSound() implements SoundEvent {}

/** Triggered when a building finishes construction. */
record FinishBuildingSound() implements SoundEvent {}

/** Triggered when a peon spawns from a residence. */
record SpawnSound() implements SoundEvent {}

/** Triggered when a peon converts to a warrior at barracks. */
record SpawnWarriorSound() implements SoundEvent {}

/** Triggered when a peon levels up. */
record DingSound() implements SoundEvent {}

/** Triggered when a peon dies. */
record DeathSound() implements SoundEvent {}

/** Triggered when a monster dies. */
record MonsterDeathSound() implements SoundEvent {}

/** Triggered when the player wins (tower destroyed). */
record WinSound() implements SoundEvent {}
