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

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import java.util.Set;

/**
 * Metadata about a registered biome.
 *
 * <p>This class stores information captured during biome registration, including
 * the biome's source mod, climate parameters, and tags. This metadata is used
 * by BC:Dimensions to build biome pools and match biomes to climate conditions.
 *
 * <p><b>Climate Parameters:</b> Temperature and moisture are extracted from the
 * biome's climate settings and normalized to a -1.0 to 1.0 scale for climate
 * grid matching.
 *
 * <p><b>Example:</b>
 * <pre>{@code
 * BiomeMetadata metadata = BiomeMetadataDB.getBiome(
 *     new ResourceLocation("minecraft", "plains")
 * );
 *
 * if (metadata != null) {
 *     System.out.println("Source mod: " + metadata.modId);
 *     System.out.println("Temperature: " + metadata.temperature);
 *     System.out.println("Moisture: " + metadata.moisture);
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public class BiomeMetadata {

    /** The biome's resource location (e.g., "minecraft:plains") */
    public final ResourceLocation id;

    /** The mod that registered this biome */
    public final String modId;

    /** The biome holder (reference to the actual biome) */
    public final Holder<Biome> biomeHolder;

    /** Temperature value (-1.0 = coldest, 1.0 = hottest) */
    public final float temperature;

    /** Moisture/downfall value (-1.0 = driest, 1.0 = wettest) */
    public final float moisture;

    /** Tags applied to this biome */
    public final Set<TagKey<Biome>> tags;

    /** Biome category (e.g., PLAINS, FOREST, OCEAN) */
    public final BCBiomeCategory category;

    /**
     * Creates new biome metadata.
     *
     * @param id the biome's resource location
     * @param modId the mod that registered this biome
     * @param biomeHolder holder reference to the biome
     * @param temperature temperature value (-1.0 to 1.0)
     * @param moisture moisture value (-1.0 to 1.0)
     * @param tags biome tags
     * @param category biome category
     */
    public BiomeMetadata(ResourceLocation id, String modId, Holder<Biome> biomeHolder,
                        float temperature, float moisture,
                        Set<TagKey<Biome>> tags, BCBiomeCategory category) {
        this.id = id;
        this.modId = modId;
        this.biomeHolder = biomeHolder;
        this.temperature = temperature;
        this.moisture = moisture;
        this.tags = tags;
        this.category = category;
    }

    /**
     * Gets the biome instance.
     *
     * @return the biome
     */
    public Biome getBiome() {
        return biomeHolder.value();
    }

    /**
     * Checks if this biome has a specific tag.
     *
     * @param tag the tag to check
     * @return true if the biome has this tag
     */
    public boolean hasTag(TagKey<Biome> tag) {
        return tags.contains(tag);
    }

    /**
     * Checks if this biome is from vanilla Minecraft.
     *
     * @return true if mod ID is "minecraft"
     */
    public boolean isVanilla() {
        return "minecraft".equals(modId);
    }

    /**
     * Calculates the climate distance to another biome.
     *
     * <p>Climate distance is the Euclidean distance in temperature-moisture space.
     * Lower values indicate more similar climates.
     *
     * <p><b>Formula:</b> {@code sqrt((temp1 - temp2)² + (moisture1 - moisture2)²)}
     *
     * @param other the other biome metadata
     * @return climate distance (0.0 = identical, ~2.83 = maximum)
     */
    public double getClimateDistance(BiomeMetadata other) {
        double tempDiff = this.temperature - other.temperature;
        double moistDiff = this.moisture - other.moisture;
        return Math.sqrt(tempDiff * tempDiff + moistDiff * moistDiff);
    }

    /**
     * Checks if this biome's climate is within tolerance of target values.
     *
     * @param targetTemp target temperature (-1.0 to 1.0)
     * @param targetMoisture target moisture (-1.0 to 1.0)
     * @param tolerance maximum allowed distance
     * @return true if within tolerance
     */
    public boolean isClimateMatch(float targetTemp, float targetMoisture, double tolerance) {
        double tempDiff = this.temperature - targetTemp;
        double moistDiff = this.moisture - targetMoisture;
        double distance = Math.sqrt(tempDiff * tempDiff + moistDiff * moistDiff);
        return distance <= tolerance;
    }

    @Override
    public String toString() {
        return String.format("BiomeMetadata{id=%s, mod=%s, temp=%.2f, moisture=%.2f, category=%s}",
                id, modId, temperature, moisture, category);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BiomeMetadata that = (BiomeMetadata) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
