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
package com.builderscompanion.dimensions.biome;

import com.builderscompanion.core.config.DimensionConfig;
import com.builderscompanion.core.util.BCLogger;
import com.builderscompanion.dimensions.BCDimensions;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * BC-specific Terrablender region for custom dimensions.
 *
 * <p>This extends Terrablender's {@link Region} class to create regions that
 * distribute biomes according to BC:Dimensions configuration.
 *
 * <p><b>How Terrablender Regions Work:</b>
 * <p>Terrablender uses a "region competition" system where multiple regions
 * compete for space in the world. Each region has:
 * <ul>
 *   <li><b>Weight</b> - How often this region wins competition (higher = more common)</li>
 *   <li><b>Biomes</b> - What biomes can be placed when this region wins</li>
 *   <li><b>Climate Parameters</b> - Where biomes appear based on temperature/moisture/etc</li>
 * </ul>
 *
 * <p><b>Example:</b>
 * <pre>
 * // A region for "Avalon" dimension with BOP biomes
 * BCRegion avalon = new BCRegion(
 *     new ResourceLocation("bc_dimensions", "avalon"),
 *     RegionType.OVERWORLD,
 *     config,
 *     biomesSet
 * );
 * // Register with Terrablender during initialization
 * </pre>
 *
 * <p><b>Coordinate Scales:</b>
 * <p>Terrablender uses "quart-resolution" climate sampling (4 blocks = 1 sample).
 * Climate parameters are in range [-1.0, 1.0]:
 * <ul>
 *   <li>Temperature: -1.0 (frozen) to 1.0 (hot)</li>
 *   <li>Humidity: -1.0 (dry) to 1.0 (wet)</li>
 *   <li>Continentalness: -1.0 (ocean) to 1.0 (inland)</li>
 *   <li>Erosion: -1.0 (peaks) to 1.0 (valleys)</li>
 *   <li>Depth: -1.0 (surface) to 1.0 (underground)</li>
 *   <li>Weirdness: -1.0 (normal) to 1.0 (unusual)</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class BCRegion extends Region {

    private final DimensionConfig config;
    private final Set<ResourceKey<Biome>> biomes;

    /**
     * Creates a new BC region.
     *
     * @param name the region's unique name
     * @param type the region type (OVERWORLD, NETHER, or END)
     * @param config the dimension configuration
     * @param biomes the biomes available in this region
     * @param weight the region's weight (10 is standard)
     */
    public BCRegion(ResourceLocation name, RegionType type, DimensionConfig config,
                   Set<ResourceKey<Biome>> biomes, int weight) {
        super(name, type, weight);
        this.config = config;
        this.biomes = biomes;

        BCLogger.debug("Created BC region: {} (type={}, weight={}, biomes={})",
                name, type, weight, biomes.size());
    }

    /**
     * Adds biomes to this region with climate parameters.
     *
     * <p>This is called by Terrablender during world generation setup.
     * We add all biomes from our pool with appropriate climate parameters.
     *
     * <p>For Region Mode, we use a broad climate distribution to allow biomes
     * to appear in varied locations. This mimics vanilla behavior where biomes
     * have flexible placement.
     *
     * @param registry the biome registry
     * @param mapper the climate parameter mapper
     */
    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        BCLogger.debug("Adding {} biomes to region {}", biomes.size(), this.getName());

        // For each biome in our pool, add it with climate parameters
        for (ResourceKey<Biome> biomeKey : biomes) {
            // Get biome metadata to determine climate
            BiomeMetadata metadata = BiomeMetadataDB.getBiome(biomeKey.location());

            if (metadata == null) {
                BCLogger.warn("No metadata found for biome: {}. Using default climate.", biomeKey.location());
                // Add with default/neutral climate
                addBiomeWithDefaultClimate(mapper, biomeKey);
                continue;
            }

            // Create climate parameter point from biome's natural climate
            Climate.ParameterPoint point = createClimatePoint(metadata);

            // Add biome with its climate parameters
            mapper.accept(Pair.of(point, biomeKey));

            BCLogger.trace("Added biome {} with climate (temp={}, moisture={})",
                    biomeKey.location(), metadata.temperature, metadata.moisture);
        }

        BCLogger.info("Registered {} biomes for region {}", biomes.size(), this.getName());
    }

    /**
     * Creates a climate parameter point from biome metadata.
     *
     * <p>This converts BC's normalized climate values to Terrablender's
     * multi-dimensional climate parameters.
     *
     * @param metadata the biome metadata
     * @return the climate parameter point
     */
    private Climate.ParameterPoint createClimatePoint(BiomeMetadata metadata) {
        // Convert normalized temperature/moisture to climate parameters
        // BC uses -1.0 to 1.0 scale, same as Terrablender

        // Temperature
        Climate.Parameter temperature = Climate.Parameter.point(
                (float) metadata.temperature
        );

        // Humidity (moisture)
        Climate.Parameter humidity = Climate.Parameter.point(
                (float) metadata.moisture
        );

        // For other parameters, use wide ranges to allow flexible placement
        // This gives biomes room to appear in various locations

        // Continentalness: Allow both ocean and land
        Climate.Parameter continentalness = Climate.Parameter.span(-0.5f, 1.0f);

        // Erosion: Allow varied erosion levels
        Climate.Parameter erosion = Climate.Parameter.span(-1.0f, 1.0f);

        // Depth: Surface level only (0.0)
        Climate.Parameter depth = Climate.Parameter.point(0.0f);

        // Weirdness: Allow some variation
        Climate.Parameter weirdness = Climate.Parameter.span(-0.5f, 0.5f);

        return new Climate.ParameterPoint(
                temperature,
                humidity,
                continentalness,
                erosion,
                depth,
                weirdness,
                0L  // offset (unused)
        );
    }

    /**
     * Adds a biome with default neutral climate parameters.
     *
     * <p>Used as fallback when biome metadata is not available.
     *
     * @param mapper the climate mapper
     * @param biomeKey the biome to add
     */
    private void addBiomeWithDefaultClimate(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper,
                                           ResourceKey<Biome> biomeKey) {
        // Create neutral/moderate climate point
        Climate.ParameterPoint point = new Climate.ParameterPoint(
                Climate.Parameter.point(0.0f),  // Temperature: neutral
                Climate.Parameter.point(0.0f),  // Humidity: neutral
                Climate.Parameter.span(-1.0f, 1.0f),  // Continentalness: any
                Climate.Parameter.span(-1.0f, 1.0f),  // Erosion: any
                Climate.Parameter.point(0.0f),  // Depth: surface
                Climate.Parameter.span(-1.0f, 1.0f),  // Weirdness: any
                0L
        );

        mapper.accept(Pair.of(point, biomeKey));
    }

    /**
     * Gets the biomes in this region.
     *
     * @return set of biome keys
     */
    public Set<ResourceKey<Biome>> getBiomes() {
        return biomes;
    }

    /**
     * Gets the dimension configuration.
     *
     * @return the config
     */
    public DimensionConfig getConfig() {
        return config;
    }
}
