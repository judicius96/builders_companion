/*
 * MIT License
 *
 * Copyright (c) 2025 Builders Companion
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 */
package com.builderscompanion.core.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Interface for providing custom geode generation.
 *
 * <p>Geode providers add amethyst-style geodes with custom blocks to dimensions.
 * This is primarily used by BC:Geodes to add various mineral geodes.
 *
 * <p><b>What is a Geode?</b> Geodes are hollow, roughly spherical structures that
 * generate underground with:
 * <ul>
 *   <li><b>Outer shell</b> - Hard outer layer (e.g., calcite)</li>
 *   <li><b>Middle layer</b> - Softer middle layer (e.g., amethyst block)</li>
 *   <li><b>Inner layer</b> - Crystal buds growing inward (e.g., amethyst clusters)</li>
 * </ul>
 *
 * <p><b>Example Implementation:</b>
 * <pre>{@code
 * public class EmeraldGeodeProvider implements IGeodeProvider {
 *
 *     @Override
 *     public List<GeodeConfig> getGeodeConfigs(ResourceKey<Level> dimension) {
 *         // Only generate in specific dimension
 *         if (!dimension.location().equals(new ResourceLocation("mymod", "mydim"))) {
 *             return Collections.emptyList();
 *         }
 *
 *         return List.of(new GeodeConfig(
 *             "emerald_geode",
 *             Blocks.DEEPSLATE,           // Outer shell
 *             MyBlocks.EMERALD_BLOCK,     // Middle layer
 *             MyBlocks.EMERALD_CLUSTER,   // Inner crystals
 *             0.002,                       // Spawn chance per chunk
 *             -64, 0                       // Y range
 *         ));
 *     }
 * }
 *
 * // Register during mod initialization
 * BCDimensionsAPI.registerGeodeProvider(new EmeraldGeodeProvider());
 * }</pre>
 *
 * <p><b>Generation Process:</b>
 * <ol>
 *   <li>BC:Dimensions queries all providers for geode configs</li>
 *   <li>For each chunk, rolls for geode generation based on spawn chance</li>
 *   <li>If successful, picks a random Y level in the specified range</li>
 *   <li>Generates the geode structure at that position</li>
 * </ol>
 *
 * <p><b>Registration:</b> Use {@link BCDimensionsAPI#registerGeodeProvider(IGeodeProvider)}
 * during FMLCommonSetupEvent. Multiple providers can be registered.
 *
 * <p><b>Thread Safety:</b> Implementations must be thread-safe as world generation
 * happens across multiple threads.
 *
 * @since 1.0.0
 * @see BCDimensionsAPI#registerGeodeProvider(IGeodeProvider)
 */
public interface IGeodeProvider {

    /**
     * Gets the geode configurations for a specific dimension.
     *
     * <p>This is called during world generation to determine which geodes should
     * generate in the given dimension.
     *
     * <p>Return an empty list if no geodes should generate in this dimension.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * @Override
     * public List<GeodeConfig> getGeodeConfigs(ResourceKey<Level> dimension) {
     *     List<GeodeConfig> configs = new ArrayList<>();
     *
     *     // Emerald geodes in mountains dimension
     *     if (dimension.location().equals(new ResourceLocation("mymod", "mountains"))) {
     *         configs.add(new GeodeConfig(
     *             "emerald_geode",
     *             Blocks.STONE,
     *             MyBlocks.EMERALD_BLOCK,
     *             MyBlocks.EMERALD_CLUSTER,
     *             0.003,
     *             32, 128
     *         ));
     *     }
     *
     *     // Ruby geodes in desert dimension
     *     if (dimension.location().equals(new ResourceLocation("mymod", "desert"))) {
     *         configs.add(new GeodeConfig(
     *             "ruby_geode",
     *             Blocks.SANDSTONE,
     *             MyBlocks.RUBY_BLOCK,
     *             MyBlocks.RUBY_CLUSTER,
     *             0.002,
     *             0, 64
     *         ));
     *     }
     *
     *     return configs;
     * }
     * }</pre>
     *
     * @param dimension the dimension to get geode configs for
     * @return list of geode configurations, or empty list if none
     */
    List<GeodeConfig> getGeodeConfigs(ResourceKey<Level> dimension);

    /**
     * Configuration for a geode type.
     *
     * <p>Defines the appearance and generation parameters for a geode.
     */
    class GeodeConfig {
        /** Unique identifier for this geode type */
        public final String id;

        /** Block for outer shell (e.g., calcite, deepslate) */
        public final BlockState outerShell;

        /** Block for middle layer (e.g., amethyst block) */
        public final BlockState middleLayer;

        /** Block for inner crystals (e.g., amethyst cluster) */
        public final BlockState innerCrystals;

        /** Chance per chunk to generate (0.0 to 1.0) */
        public final double spawnChance;

        /** Minimum Y level for generation */
        public final int minY;

        /** Maximum Y level for generation */
        public final int maxY;

        /** Minimum radius in blocks */
        public final int minRadius;

        /** Maximum radius in blocks */
        public final int maxRadius;

        /**
         * Creates a basic geode configuration with default radius (4-7 blocks).
         *
         * @param id unique identifier
         * @param outerShell outer shell block
         * @param middleLayer middle layer block
         * @param innerCrystals inner crystal block
         * @param spawnChance chance per chunk (0.0 to 1.0)
         * @param minY minimum Y level
         * @param maxY maximum Y level
         */
        public GeodeConfig(String id, BlockState outerShell, BlockState middleLayer,
                          BlockState innerCrystals, double spawnChance, int minY, int maxY) {
            this(id, outerShell, middleLayer, innerCrystals, spawnChance, minY, maxY, 4, 7);
        }

        /**
         * Creates a geode configuration with custom radius.
         *
         * @param id unique identifier
         * @param outerShell outer shell block
         * @param middleLayer middle layer block
         * @param innerCrystals inner crystal block
         * @param spawnChance chance per chunk (0.0 to 1.0)
         * @param minY minimum Y level
         * @param maxY maximum Y level
         * @param minRadius minimum radius in blocks
         * @param maxRadius maximum radius in blocks
         */
        public GeodeConfig(String id, BlockState outerShell, BlockState middleLayer,
                          BlockState innerCrystals, double spawnChance, int minY, int maxY,
                          int minRadius, int maxRadius) {
            this.id = id;
            this.outerShell = outerShell;
            this.middleLayer = middleLayer;
            this.innerCrystals = innerCrystals;
            this.spawnChance = spawnChance;
            this.minY = minY;
            this.maxY = maxY;
            this.minRadius = minRadius;
            this.maxRadius = maxRadius;
        }

        /**
         * Checks if this geode should generate in the given chunk.
         *
         * @param random random source for this chunk
         * @return true if a geode should generate
         */
        public boolean shouldGenerate(RandomSource random) {
            return random.nextDouble() < spawnChance;
        }

        /**
         * Gets a random Y level within this geode's range.
         *
         * @param random random source
         * @return random Y coordinate
         */
        public int getRandomY(RandomSource random) {
            return random.nextInt(maxY - minY + 1) + minY;
        }

        /**
         * Gets a random radius within this geode's range.
         *
         * @param random random source
         * @return random radius in blocks
         */
        public int getRandomRadius(RandomSource random) {
            return random.nextInt(maxRadius - minRadius + 1) + minRadius;
        }
    }

    /**
     * Optional: Custom generation logic for a geode.
     *
     * <p>By default, BC:Dimensions will generate geodes using a standard
     * spherical algorithm. Override this method to provide custom generation.
     *
     * <p>Default implementation returns false to use standard generation.
     *
     * @param level the world gen level
     * @param pos the center position for the geode
     * @param config the geode configuration
     * @param random random source
     * @return true if custom generation was performed, false to use standard generation
     */
    default boolean generateCustom(WorldGenLevel level, BlockPos pos,
                                   GeodeConfig config, RandomSource random) {
        return false; // Use standard generation by default
    }
}
