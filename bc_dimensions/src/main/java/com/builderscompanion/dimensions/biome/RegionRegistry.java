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
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import terrablender.api.RegionType;
import terrablender.api.Regions;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages registration of BC regions with Terrablender.
 *
 * <p>This class handles the creation and registration of Terrablender regions
 * for dimensions using Region Mode biome distribution.
 *
 * <p><b>Registration Flow:</b>
 * <ol>
 *   <li>Dimension configured with {@code mode: region}</li>
 *   <li>BiomePoolBuilder creates pool of available biomes</li>
 *   <li>RegionRegistry creates BCRegion with configured weight</li>
 *   <li>Region is registered with Terrablender during common setup</li>
 *   <li>Terrablender handles biome placement in-game</li>
 * </ol>
 *
 * <p><b>Region Types:</b>
 * <p>Terrablender organizes regions by dimension type:
 * <ul>
 *   <li><b>OVERWORLD</b> - Standard surface dimensions</li>
 *   <li><b>NETHER</b> - Nether-style dimensions</li>
 *   <li><b>END</b> - End-style dimensions</li>
 * </ul>
 *
 * <p>BC:Dimensions determines the type from dimension config.
 *
 * @since 1.0.0
 */
public class RegionRegistry {

    /**
     * Registers a BC region for a dimension with Terrablender.
     *
     * <p>This creates a new BCRegion and registers it with Terrablender's
     * region system. The region will participate in Terrablender's biome
     * competition system.
     *
     * <p><b>Weight Guidelines:</b>
     * <ul>
     *   <li><b>10</b> - Standard weight (Terrablender default)</li>
     *   <li><b>5-15</b> - Typical range for balanced blending</li>
     *   <li><b>20+</b> - Dominant presence (use sparingly)</li>
     *   <li><b>1-4</b> - Rare presence (occasional biomes)</li>
     * </ul>
     *
     * @param dimensionKey the dimension resource key
     * @param config the dimension configuration
     * @param biomePool the available biomes for this dimension
     * @return the created region
     */
    public static BCRegion registerRegion(ResourceKey<Level> dimensionKey,
                                         DimensionConfig config,
                                         Set<Holder<Biome>> biomePool) {
        BCLogger.debug("Registering Terrablender region for dimension: {}", dimensionKey.location());

        // Convert biome holders to resource keys
        Set<ResourceKey<Biome>> biomeKeys = biomePool.stream()
                .map(Holder::unwrapKey)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .collect(Collectors.toSet());

        if (biomeKeys.isEmpty()) {
            BCLogger.error("Cannot create region for {}: no biomes in pool", dimensionKey.location());
            return null;
        }

        // Create region name
        ResourceLocation regionName = ResourceLocation.fromNamespaceAndPath(
                BCDimensions.MODID,
                dimensionKey.location().getPath() + "_region"
        );

        // Determine region type from config
        RegionType regionType = determineRegionType(config);

        // Get weight from config (or use default)
        int weight = getRegionWeight(regionName, config);

        // Create BC region
        BCRegion region = new BCRegion(
                regionName,
                regionType,
                config,
                biomeKeys,
                weight
        );

        // Register with Terrablender
        try {
            Regions.register(region);
            BCLogger.info("Registered Terrablender region: {} (type={}, weight={}, biomes={})",
                    regionName, regionType, weight, biomeKeys.size());
        } catch (Exception e) {
            BCLogger.error("Failed to register region with Terrablender: {}", e.getMessage());
            e.printStackTrace();
            return null;
        }

        // Store metadata
        Set<ResourceLocation> biomeIds = biomeKeys.stream()
                .map(ResourceKey::location)
                .collect(Collectors.toSet());

        RegionMetadataDB.registerRegion(regionName, dimensionKey, config, biomeIds);

        return region;
    }

    /**
     * Determines the Terrablender region type from dimension config.
     *
     * <p>This looks at the dimension's properties to decide if it's an
     * overworld-like, nether-like, or end-like dimension.
     *
     * @param config the dimension config
     * @return the appropriate region type
     */
    private static RegionType determineRegionType(DimensionConfig config) {
        // Check vanilla dimension source if copying vanilla biomes
        if (config.biomes.includeVanilla) {
            String vanillaDim = config.biomes.vanillaDimension;

            if ("the_nether".equals(vanillaDim) || "nether".equals(vanillaDim)) {
                return RegionType.NETHER;
            }
        }

        // Terrablender only supports OVERWORLD and NETHER
        // End dimensions don't use Terrablender regions
        return RegionType.OVERWORLD;
    }

    /**
     * Gets the configured weight for a region.
     *
     * <p>Checks if user has overridden the weight, otherwise uses default.
     *
     * @param regionName the region name
     * @param config the dimension config
     * @return the effective weight
     */
    private static int getRegionWeight(ResourceLocation regionName, DimensionConfig config) {
        // Check for user override
        Integer override = config.biomes.regionOverrides.get(regionName.toString());
        if (override != null) {
            BCLogger.debug("Using override weight {} for region {}", override, regionName);
            return override;
        }

        // Use default weight (10 is Terrablender standard)
        return 10;
    }

    /**
     * Generates a report showing all registered BC regions.
     *
     * <p>This provides transparency into what regions exist and their
     * configuration.
     */
    public static void logRegionReport() {
        BCLogger.info("=".repeat(60));
        BCLogger.info("BC:Dimensions Terrablender Region Report");
        BCLogger.info("=".repeat(60));

        if (RegionMetadataDB.getRegionCount() == 0) {
            BCLogger.info("No BC regions registered");
            return;
        }

        BCLogger.info(RegionMetadataDB.getStatistics());
        BCLogger.info("");

        for (RegionMetadata region : RegionMetadataDB.getAllRegions()) {
            BCLogger.info("Region: {}", region.name);
            BCLogger.info("  Dimension: {}", region.dimension.location());
            BCLogger.info("  Weight: {}{}",
                    region.getCurrentWeight(),
                    region.isWeightOverridden() ? " (overridden from " + region.defaultWeight + ")" : "");
            BCLogger.info("  Biomes: {}", region.getBiomeCount());
        }

        BCLogger.info("=".repeat(60));
    }
}
