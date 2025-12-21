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

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Set;

/**
 * Metadata about a Terrablender region.
 *
 * <p>This stores information about regions registered by biome mods for use
 * in Region Mode biome distribution.
 *
 * <p><b>What is a Region?</b>
 * <p>In Terrablender, a "region" is a collection of biomes that compete for
 * placement in the world. Each region has:
 * <ul>
 *   <li><b>Weight</b> - How often this region's biomes appear (higher = more common)</li>
 *   <li><b>Biomes</b> - The biomes this region can place</li>
 *   <li><b>Dimension</b> - Which dimension this region applies to</li>
 * </ul>
 *
 * <p><b>Example:</b>
 * <pre>
 * Name: "biomesoplenty:bop_overworld_1"
 * Mod: "biomesoplenty"
 * Weight: 10
 * Biomes: [cherry_blossom_grove, lavender_field, mystic_grove, ...]
 * Dimension: minecraft:overworld
 * </pre>
 *
 * @since 1.0.0
 */
public class RegionMetadata {

    /** The unique name of this region */
    public final ResourceLocation name;

    /** The mod that registered this region */
    public final String modId;

    /** The default weight assigned by the mod */
    public final int defaultWeight;

    /** The current weight (may be overridden by user config) */
    private int currentWeight;

    /** The biomes this region can place */
    public final Set<ResourceLocation> biomes;

    /** The dimension this region applies to */
    public final ResourceKey<Level> dimension;

    /** Whether user has overridden the weight */
    private boolean weightOverridden;

    /**
     * Creates a new region metadata entry.
     *
     * @param name the region name
     * @param modId the mod that registered this region
     * @param weight the region's weight
     * @param biomes the biomes in this region
     * @param dimension the dimension this region applies to
     */
    public RegionMetadata(ResourceLocation name, String modId, int weight,
                         Set<ResourceLocation> biomes, ResourceKey<Level> dimension) {
        this.name = name;
        this.modId = modId;
        this.defaultWeight = weight;
        this.currentWeight = weight;
        this.biomes = biomes;
        this.dimension = dimension;
        this.weightOverridden = false;
    }

    /**
     * Gets the current weight for this region.
     *
     * @return the effective weight
     */
    public int getCurrentWeight() {
        return currentWeight;
    }

    /**
     * Sets an override weight for this region.
     *
     * <p>This is called when user has a region_overrides entry in config.
     *
     * @param weight the new weight
     */
    public void setOverrideWeight(int weight) {
        this.currentWeight = weight;
        this.weightOverridden = true;
    }

    /**
     * Resets the weight to the default.
     */
    public void resetWeight() {
        this.currentWeight = this.defaultWeight;
        this.weightOverridden = false;
    }

    /**
     * Checks if the weight has been overridden.
     *
     * @return true if user override is active
     */
    public boolean isWeightOverridden() {
        return weightOverridden;
    }

    /**
     * Gets the number of biomes in this region.
     *
     * @return biome count
     */
    public int getBiomeCount() {
        return biomes.size();
    }

    /**
     * Checks if this region contains a specific biome.
     *
     * @param biomeId the biome to check
     * @return true if this region includes the biome
     */
    public boolean containsBiome(ResourceLocation biomeId) {
        return biomes.contains(biomeId);
    }

    @Override
    public String toString() {
        return String.format("Region{name=%s, mod=%s, weight=%d%s, biomes=%d}",
                name,
                modId,
                currentWeight,
                weightOverridden ? " (overridden)" : "",
                biomes.size());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RegionMetadata)) return false;
        RegionMetadata other = (RegionMetadata) obj;
        return name.equals(other.name) && dimension.equals(other.dimension);
    }

    @Override
    public int hashCode() {
        return name.hashCode() * 31 + dimension.hashCode();
    }
}
