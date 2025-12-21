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

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Interface for modifying biome placement during world generation.
 *
 * <p>Biome modifiers can swap which biome is placed at a location during chunk
 * generation. This is useful for mods that want to customize biome distribution
 * based on custom criteria.
 *
 * <p><b>Use Cases:</b>
 * <ul>
 *   <li><b>BC:Fluids</b> - Swap ocean biomes based on water color</li>
 *   <li><b>Temperature Mods</b> - Adjust biomes based on season</li>
 *   <li><b>Pollution Mods</b> - Change biomes near polluted areas</li>
 * </ul>
 *
 * <p><b>Example Implementation:</b>
 * <pre>{@code
 * public class WaterColorBiomeModifier implements IBiomeModifier {
 *
 *     @Override
 *     public Holder<Biome> modifyBiome(Holder<Biome> original, BiomeContext context) {
 *         // Only modify ocean biomes
 *         if (!original.value().getBiomeCategory().equals(Biome.BiomeCategory.OCEAN)) {
 *             return original;
 *         }
 *
 *         // Get water color at this location
 *         int waterColor = getWaterColor(context.chunkPos, context.biomePos);
 *
 *         // Swap to custom ocean biome based on color
 *         if (waterColor == 0xFF0000) { // Red water
 *             return getRedOceanBiome();
 *         }
 *
 *         return original;
 *     }
 * }
 *
 * // Register during mod initialization
 * BCDimensionsAPI.registerBiomeModifier(new WaterColorBiomeModifier());
 * }</pre>
 *
 * <p><b>Execution Order:</b> Multiple biome modifiers can be registered and will
 * be called in registration order. Each modifier receives the result of the previous
 * modifier, creating a pipeline:
 * <pre>
 * Original Biome → Modifier 1 → Modifier 2 → Modifier 3 → Final Biome
 * </pre>
 *
 * <p><b>Performance:</b> Biome modifiers are called for every biome position in
 * every chunk during world generation. Keep implementations fast:
 * <ul>
 *   <li>Return early if modification isn't needed</li>
 *   <li>Cache expensive calculations</li>
 *   <li>Avoid world access if possible (use context data)</li>
 * </ul>
 *
 * <p><b>Registration:</b> Use {@link BCDimensionsAPI#registerBiomeModifier(IBiomeModifier)}
 * during FMLCommonSetupEvent.
 *
 * <p><b>Thread Safety:</b> Implementations must be thread-safe as biome generation
 * happens across multiple threads.
 *
 * @since 1.0.0
 * @see BCDimensionsAPI#registerBiomeModifier(IBiomeModifier)
 */
public interface IBiomeModifier {

    /**
     * Modifies the biome at a given position during chunk generation.
     *
     * <p>This method receives the biome that would normally be placed at this
     * position (either from vanilla generation or previous modifiers) and can
     * return a different biome.
     *
     * <p><b>Parameters:</b>
     * <ul>
     *   <li><b>original</b> - The biome that would be placed without modification</li>
     *   <li><b>context</b> - Context data about the position being generated</li>
     * </ul>
     *
     * <p><b>Return Value:</b> The biome to actually place. Return the original
     * biome if no modification is needed.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * @Override
     * public Holder<Biome> modifyBiome(Holder<Biome> original, BiomeContext context) {
     *     // Only modify in specific dimension
     *     if (!context.dimension.location().equals(new ResourceLocation("mymod", "mydim"))) {
     *         return original;
     *     }
     *
     *     // Swap plains with forest 50% of the time
     *     if (original.is(Biomes.PLAINS)) {
     *         if (context.random.nextBoolean()) {
     *             return context.biomeRegistry.get(Biomes.FOREST);
     *         }
     *     }
     *
     *     return original;
     * }
     * }</pre>
     *
     * @param original the original biome that would be placed
     * @param context context information about the generation position
     * @return the biome to actually place (may be the same as original)
     */
    Holder<Biome> modifyBiome(Holder<Biome> original, BiomeContext context);

    /**
     * Context information passed to biome modifiers.
     *
     * <p>Provides data about the position being generated without requiring
     * expensive world access.
     */
    class BiomeContext {
        /** The dimension being generated */
        public final ResourceKey<net.minecraft.world.level.Level> dimension;

        /** The chunk being generated (may be null for preview generation) */
        public final ChunkAccess chunk;

        /** X coordinate of the biome position (in biome coordinates, not block coordinates) */
        public final int biomeX;

        /** Y coordinate of the biome position (in biome coordinates, not block coordinates) */
        public final int biomeY;

        /** Z coordinate of the biome position (in biome coordinates, not block coordinates) */
        public final int biomeZ;

        /** Random instance seeded for this position (use for deterministic randomness) */
        public final java.util.Random random;

        /**
         * Creates a new biome context.
         *
         * @param dimension the dimension being generated
         * @param chunk the chunk being generated
         * @param biomeX X coordinate in biome space
         * @param biomeY Y coordinate in biome space
         * @param biomeZ Z coordinate in biome space
         * @param random seeded random for this position
         */
        public BiomeContext(ResourceKey<net.minecraft.world.level.Level> dimension,
                           ChunkAccess chunk,
                           int biomeX, int biomeY, int biomeZ,
                           java.util.Random random) {
            this.dimension = dimension;
            this.chunk = chunk;
            this.biomeX = biomeX;
            this.biomeY = biomeY;
            this.biomeZ = biomeZ;
            this.random = random;
        }

        /**
         * Converts biome coordinates to block coordinates.
         *
         * <p>Biome coordinates are in 4x4x4 block sections.
         *
         * @return block X coordinate
         */
        public int getBlockX() {
            return biomeX << 2; // Multiply by 4
        }

        /**
         * Converts biome coordinates to block coordinates.
         *
         * @return block Y coordinate
         */
        public int getBlockY() {
            return biomeY << 2; // Multiply by 4
        }

        /**
         * Converts biome coordinates to block coordinates.
         *
         * @return block Z coordinate
         */
        public int getBlockZ() {
            return biomeZ << 2; // Multiply by 4
        }
    }
}
