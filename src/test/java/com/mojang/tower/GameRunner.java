package com.mojang.tower;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Headless game runner for deterministic golden master testing.
 * Runs the game without rendering and captures state each tick.
 */
public class GameRunner {

    /**
     * Maximum ticks to run before aborting (safety limit for CI).
     * A typical game win takes thousands of ticks.
     */
    private static final int MAX_TICKS = 50000;

    /**
     * Fixed seed for determinism (matches Island's hardcoded seed).
     */
    private static final long FIXED_SEED = 8844L;

    /**
     * Counter for seeding entity Randoms uniquely but deterministically.
     */
    private static int entitySeedCounter = 0;

    /**
     * Runs a deterministic game simulation and captures state each tick.
     *
     * @return List of GameState snapshots, one per tick
     */
    public static List<GameState> runDeterministicGame() {
        return runDeterministicGame(MAX_TICKS);
    }

    /**
     * Runs a deterministic game simulation with custom max ticks.
     *
     * @param maxTicks maximum ticks before aborting
     * @return List of GameState snapshots, one per tick
     */
    public static List<GameState> runDeterministicGame(int maxTicks) {
        // Reset seed counter for reproducibility
        entitySeedCounter = 0;

        // Create minimal components for headless operation
        HeadlessTowerComponent tower = new HeadlessTowerComponent();
        Island island = new Island(tower, createDummyImage());

        // Seed all entity Randoms for determinism
        seedAllEntityRandoms(island);

        List<GameState> states = new ArrayList<>();
        boolean won = false;

        for (int tick = 0; tick < maxTicks && !won; tick++) {
            // Capture state before tick
            GameState state = captureState(tick, island, false, won);
            states.add(state);

            // Run one tick
            island.tick();

            // Check win condition (tower destroyed)
            won = tower.hasWon();

            // Seed any newly added entities
            seedAllEntityRandoms(island);
        }

        // Capture final state
        if (won) {
            states.add(captureState(states.size(), island, false, true));
        }

        return states;
    }

    /**
     * Creates a dummy 256x256 ARGB image for Island's ground detection.
     * The island image is used for collision/ground checking via pixel alpha.
     */
    private static BufferedImage createDummyImage() {
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);

        // Fill with walkable ground (alpha > 128 = walkable)
        // Create a circular island roughly matching the real game
        int[] pixels = new int[256 * 256];
        int centerX = 128;
        int centerY = 128;
        int radius = 100;

        for (int y = 0; y < 256; y++) {
            for (int x = 0; x < 256; x++) {
                int dx = x - centerX;
                int dy = y - centerY;
                if (dx * dx + dy * dy < radius * radius) {
                    // Fully opaque (alpha=255) = walkable ground
                    pixels[y * 256 + x] = 0xFF00FF00; // Green with full alpha
                } else {
                    // Transparent (alpha=0) = water/void
                    pixels[y * 256 + x] = 0x00000000;
                }
            }
        }

        image.setRGB(0, 0, 256, 256, pixels, 0, 256);
        return image;
    }

    /**
     * Seeds all entity Random fields for deterministic behavior.
     * Uses reflection to access protected Random fields.
     */
    private static void seedAllEntityRandoms(Island island) {
        for (Entity entity : island.entities) {
            seedEntityRandom(entity);

            // Seed Job's Random if entity is a Peon with a job
            if (entity instanceof Peon peon) {
                Job job = getJob(peon);
                if (job != null) {
                    seedJobRandom(job);
                }
            }
        }
    }

    /**
     * Seeds an entity's Random field using reflection.
     */
    private static void seedEntityRandom(Entity entity) {
        try {
            Field randomField = Entity.class.getDeclaredField("random");
            randomField.setAccessible(true);
            Random random = (Random) randomField.get(entity);

            // Use a deterministic seed based on entity creation order
            // The counter ensures each entity gets a unique but reproducible seed
            random.setSeed(FIXED_SEED + entitySeedCounter++);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to seed entity Random", e);
        }
    }

    /**
     * Seeds a job's Random field using reflection.
     */
    private static void seedJobRandom(Job job) {
        try {
            Field randomField = Job.class.getDeclaredField("random");
            randomField.setAccessible(true);
            Random random = (Random) randomField.get(job);
            random.setSeed(FIXED_SEED + entitySeedCounter++);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to seed job Random", e);
        }
    }

    /**
     * Gets a Peon's job using reflection (job is private).
     */
    private static Job getJob(Peon peon) {
        try {
            Field jobField = Peon.class.getDeclaredField("job");
            jobField.setAccessible(true);
            return (Job) jobField.get(peon);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to get Peon job", e);
        }
    }

    /**
     * Captures complete game state at current tick.
     */
    private static GameState captureState(int tick, Island island, boolean titleScreen, boolean won) {
        List<EntityState> entityStates = new ArrayList<>();

        for (Entity entity : island.entities) {
            EntityState state = captureEntityState(entity);
            if (state != null) {
                entityStates.add(state);
            }
        }

        // Sort for stable ordering
        Collections.sort(entityStates);

        ResourceState resources = new ResourceState(
            island.resources.wood,
            island.resources.rock,
            island.resources.food
        );

        return new GameState(
            tick,
            entityStates,
            resources,
            island.population,
            island.populationCap,
            island.monsterPopulation,
            island.warriorPopulation,
            island.warriorPopulationCap,
            titleScreen,
            won
        );
    }

    /**
     * Captures state for a single entity.
     * Returns null for entities that don't contribute meaningful state (Puff, InfoPuff).
     */
    private static EntityState captureEntityState(Entity entity) {
        String type = entity.getClass().getSimpleName();

        // Skip visual-only entities
        if (type.equals("Puff") || type.equals("InfoPuff")) {
            return null;
        }

        Integer hp = null;
        String jobType = null;
        Integer carrying = null;

        if (entity instanceof Peon peon) {
            hp = getPeonHp(peon);
            Job job = getJob(peon);
            if (job != null) {
                jobType = job.getClass().getSimpleName();
                carrying = job.getCarried() >= 0 ? job.getCarried() : null;
            }
        } else if (entity instanceof Monster) {
            hp = getMonsterHp((Monster) entity);
        } else if (entity instanceof House) {
            hp = getHouseHp((House) entity);
        } else if (entity instanceof Tower) {
            // Tower doesn't expose hp directly, but we could capture stamina
            // For now, skip Tower-specific state
        }

        return new EntityState(
            type,
            entity.x,
            entity.y,
            entity.r,
            entity.isAlive(),
            hp,
            jobType,
            carrying
        );
    }

    /**
     * Gets Peon HP using reflection.
     */
    private static Integer getPeonHp(Peon peon) {
        try {
            Field hpField = Peon.class.getDeclaredField("hp");
            hpField.setAccessible(true);
            return (Integer) hpField.get(peon);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }

    /**
     * Gets Monster HP using reflection.
     */
    private static Integer getMonsterHp(Monster monster) {
        try {
            Field hpField = Monster.class.getDeclaredField("hp");
            hpField.setAccessible(true);
            return (Integer) hpField.get(monster);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }

    /**
     * Gets House HP using reflection.
     */
    private static Integer getHouseHp(House house) {
        try {
            Field hpField = House.class.getDeclaredField("hp");
            hpField.setAccessible(true);
            return (Integer) hpField.get(house);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }

    /**
     * Minimal TowerComponent implementation for headless testing.
     * Doesn't require AWT/rendering but satisfies Island's constructor.
     */
    private static class HeadlessTowerComponent extends TowerComponent {
        private boolean won = false;

        public HeadlessTowerComponent() {
            super(512, 320);
            // Initialize bitmaps to null-safe stubs
            bitmaps = new HeadlessBitmaps();
        }

        @Override
        public void win() {
            won = true;
        }

        public boolean hasWon() {
            return won;
        }
    }

    /**
     * Minimal Bitmaps implementation that doesn't load actual images.
     * Provides null/empty arrays for bitmap fields to prevent NPE.
     */
    private static class HeadlessBitmaps extends Bitmaps {
        public HeadlessBitmaps() {
            // Initialize all bitmap fields to prevent NPE during entity init
            // These are only used for rendering, which we skip
            trees = new BufferedImage[16];
            farmPlots = new BufferedImage[9];
            rocks = new BufferedImage[4];
            carriedResources = new BufferedImage[4];
            peons = new BufferedImage[4][12];
            smoke = new BufferedImage[5];
            infoPuffs = new BufferedImage[5];
            houses = new BufferedImage[3][8];
            soundButtons = new BufferedImage[2];

            // Create tiny dummy images for required ones
            island = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            towerTop = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            towerMid = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            towerBot = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            logo = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            wonScreen = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            delete = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            help = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        }
    }
}
