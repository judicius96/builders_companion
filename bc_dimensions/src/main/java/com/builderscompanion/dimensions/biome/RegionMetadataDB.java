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
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Database of Terrablender regions registered for BC dimensions.
 *
 * <p>This tracks all regions created by BC:Dimensions for use in Region Mode.
 * Each BC dimension using Region Mode gets its own Terrablender region with
 * configurable weight and biome distribution.
 *
 * <p><b>How Region Mode Works:</b>
 * <ol>
 *   <li>BC:Dimensions creates a custom Terrablender region for each dimension</li>
 *   <li>The region's weight and biomes are loaded from YAML config</li>
 *   <li>User can override weights via {@code region_overrides} config</li>
 *   <li>Terrablender handles the actual biome placement using its competition system</li>
 * </ol>
 *
 * <p><b>Important:</b> This only tracks BC-created regions. Regions from other
 * mods (Biomes O' Plenty, Terralith, etc.) are managed by those mods and
 * registered directly with Terrablender.
 *
 * <p><b>Thread-Safety:</b> All operations are thread-safe using ConcurrentHashMap.
 *
 * @since 1.0.0
 */
public class RegionMetadataDB {

    // Map of dimension -> list of regions for that dimension
    private static final Map<ResourceKey<Level>, List<RegionMetadata>> DIMENSION_REGIONS =
            new ConcurrentHashMap<>();

    // Map of region name -> region metadata
    private static final Map<ResourceLocation, RegionMetadata> REGION_REGISTRY =
            new ConcurrentHashMap<>();

    /**
     * Registers a BC region for a dimension.
     *
     * <p>This is called when a dimension using Region Mode is initialized.
     *
     * @param regionName the unique region name
     * @param dimension the dimension this region applies to
     * @param config the dimension configuration
     * @param biomes the biomes available in this region
     * @return the created region metadata
     */
    public static RegionMetadata registerRegion(ResourceLocation regionName,
                                                ResourceKey<Level> dimension,
                                                DimensionConfig config,
                                                Set<ResourceLocation> biomes) {
        // Determine initial weight from config
        int weight = getRegionWeight(regionName, config);

        // Create metadata
        RegionMetadata metadata = new RegionMetadata(
                regionName,
                "bc_dimensions",  // All BC regions are from this mod
                weight,
                biomes,
                dimension
        );

        // Store in registries
        REGION_REGISTRY.put(regionName, metadata);
        DIMENSION_REGIONS.computeIfAbsent(dimension, k -> new ArrayList<>()).add(metadata);

        BCLogger.info("Registered BC region: {} for dimension {} (weight={}, biomes={})",
                regionName, dimension.location(), weight, biomes.size());

        return metadata;
    }

    /**
     * Gets the configured weight for a region.
     *
     * <p>Checks if user has overridden the weight in config, otherwise uses default.
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
     * Gets all regions registered for a dimension.
     *
     * @param dimension the dimension
     * @return list of regions, or empty if none
     */
    public static List<RegionMetadata> getRegionsForDimension(ResourceKey<Level> dimension) {
        return DIMENSION_REGIONS.getOrDefault(dimension, Collections.emptyList());
    }

    /**
     * Gets a specific region by name.
     *
     * @param regionName the region name
     * @return the region metadata, or null if not found
     */
    @Nullable
    public static RegionMetadata getRegion(ResourceLocation regionName) {
        return REGION_REGISTRY.get(regionName);
    }

    /**
     * Gets all registered BC regions.
     *
     * @return collection of all regions
     */
    public static Collection<RegionMetadata> getAllRegions() {
        return Collections.unmodifiableCollection(REGION_REGISTRY.values());
    }

    /**
     * Checks if a region is registered.
     *
     * @param regionName the region name
     * @return true if registered
     */
    public static boolean isRegionRegistered(ResourceLocation regionName) {
        return REGION_REGISTRY.containsKey(regionName);
    }

    /**
     * Gets the total number of registered regions.
     *
     * @return region count
     */
    public static int getRegionCount() {
        return REGION_REGISTRY.size();
    }

    /**
     * Gets statistics about registered regions.
     *
     * @return formatted statistics string
     */
    public static String getStatistics() {
        int totalRegions = REGION_REGISTRY.size();
        int totalDimensions = DIMENSION_REGIONS.size();
        int totalBiomes = REGION_REGISTRY.values().stream()
                .mapToInt(RegionMetadata::getBiomeCount)
                .sum();
        int totalWeight = REGION_REGISTRY.values().stream()
                .mapToInt(RegionMetadata::getCurrentWeight)
                .sum();

        return String.format(
                "BC Regions: %d regions across %d dimensions (%d biomes, %d total weight)",
                totalRegions, totalDimensions, totalBiomes, totalWeight
        );
    }

    /**
     * Generates a report of all regions for a dimension.
     *
     * <p>This creates a detailed breakdown showing:
     * <ul>
     *   <li>Region names and weights</li>
     *   <li>Which weights are overridden</li>
     *   <li>Biome counts per region</li>
     *   <li>Total influence percentage</li>
     * </ul>
     *
     * @param dimension the dimension
     * @return formatted report lines
     */
    public static List<String> generateDimensionReport(ResourceKey<Level> dimension) {
        List<String> report = new ArrayList<>();
        List<RegionMetadata> regions = getRegionsForDimension(dimension);

        if (regions.isEmpty()) {
            report.add("No BC regions registered for dimension: " + dimension.location());
            return report;
        }

        int totalWeight = regions.stream()
                .mapToInt(RegionMetadata::getCurrentWeight)
                .sum();

        report.add("=".repeat(60));
        report.add("BC Regions for Dimension: " + dimension.location());
        report.add("=".repeat(60));
        report.add("");
        report.add(String.format("Total Regions: %d", regions.size()));
        report.add(String.format("Total Weight: %d", totalWeight));
        report.add("");
        report.add("Regions:");
        report.add("-".repeat(60));

        for (RegionMetadata region : regions) {
            double influence = (region.getCurrentWeight() / (double) totalWeight) * 100.0;

            report.add(String.format("  %s", region.name));
            report.add(String.format("    Weight: %d%s (%.1f%% influence)",
                    region.getCurrentWeight(),
                    region.isWeightOverridden() ? " [OVERRIDDEN from " + region.defaultWeight + "]" : "",
                    influence));
            report.add(String.format("    Biomes: %d", region.getBiomeCount()));
            report.add("");
        }

        return report;
    }

    /**
     * Clears all registered regions.
     *
     * <p><b>Internal use only.</b> Called during server shutdown/reload.
     */
    public static void clear() {
        DIMENSION_REGIONS.clear();
        REGION_REGISTRY.clear();
        BCLogger.debug("Cleared region metadata database");
    }
}
