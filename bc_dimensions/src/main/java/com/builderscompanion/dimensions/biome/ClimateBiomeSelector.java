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
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;

import java.util.*;

/**
 * Selects biomes based on climate matching.
 *
 * <p>This finds biomes that best match a target climate (temperature + moisture)
 * and uses weighted random selection to choose among candidates.
 *
 * <p><b>Selection Algorithm:</b>
 * <ol>
 *   <li>Find all biomes within climate tolerance</li>
 *   <li>Weight candidates by inverse distance (closer = heavier weight)</li>
 *   <li>Use noise-seeded random to select from weighted candidates</li>
 * </ol>
 *
 * <p><b>Why Weighted Selection?</b>
 * <p>This prevents hard boundaries between biomes. Instead of always picking
 * the closest biome, we give closer biomes higher probability while still
 * allowing variety.
 *
 * <p><b>Example:</b>
 * <pre>
 * Target climate: temp=0.5, moisture=0.2
 * Candidates:
 *   - Plains: distance=0.1, weight=10.0 (90% chance)
 *   - Savanna: distance=0.2, weight=5.0 (9% chance)
 *   - Forest: distance=0.8, weight=1.25 (1% chance)
 * </pre>
 *
 * @since 1.0.0
 */
public class ClimateBiomeSelector {

    private final DimensionConfig.ClimateGridConfig config;
    private final Set<Holder<Biome>> biomePool;
    private final Map<Holder<Biome>, BiomeMetadata> metadataCache;
    private final double climateTolerance;

    /**
     * Creates a new climate biome selector.
     *
     * @param config the climate grid configuration
     * @param biomePool the available biomes
     */
    public ClimateBiomeSelector(DimensionConfig.ClimateGridConfig config, Set<Holder<Biome>> biomePool) {
        this.config = config;
        this.biomePool = biomePool;
        this.metadataCache = new HashMap<>();
        this.climateTolerance = config.climateTolerance;

        // Pre-cache metadata for all biomes
        for (Holder<Biome> biome : biomePool) {
            biome.unwrapKey().ifPresent(key -> {
                BiomeMetadata metadata = BiomeMetadataDB.getBiome(key.location());
                if (metadata != null) {
                    metadataCache.put(biome, metadata);
                }
            });
        }

        BCLogger.debug("Created ClimateBiomeSelector with {} biomes (cached {} metadata entries)",
                biomePool.size(), metadataCache.size());
    }

    /**
     * Selects the best biome for a climate.
     *
     * <p>This is the main entry point for biome selection.
     *
     * @param climate the target climate
     * @param chunkX chunk X for noise seeding
     * @param chunkZ chunk Z for noise seeding
     * @param random random source
     * @return selected biome, or a fallback if no matches found
     */
    public Holder<Biome> selectBiome(ClimateGridGenerator.Climate climate,
                                     int chunkX, int chunkZ, RandomSource random) {
        // Find candidate biomes within tolerance
        List<WeightedBiome> candidates = findCandidates(climate);

        if (candidates.isEmpty()) {
            BCLogger.warn("No biomes match climate {} within tolerance {}, using fallback",
                    climate, climateTolerance);
            return getFallbackBiome();
        }

        // Select from weighted candidates using position-based seed
        long seed = getSeed(chunkX, chunkZ);
        RandomSource selectionRandom = RandomSource.create(seed);

        return weightedSelect(candidates, selectionRandom);
    }

    /**
     * Finds all biomes within climate tolerance.
     *
     * <p>Biomes are weighted by inverse distance - closer biomes get higher weights.
     *
     * @param climate the target climate
     * @return list of weighted candidate biomes
     */
    private List<WeightedBiome> findCandidates(ClimateGridGenerator.Climate climate) {
        List<WeightedBiome> candidates = new ArrayList<>();

        for (Map.Entry<Holder<Biome>, BiomeMetadata> entry : metadataCache.entrySet()) {
            Holder<Biome> biome = entry.getKey();
            BiomeMetadata metadata = entry.getValue();

            // Calculate climate distance
            double distance = calculateDistance(
                    climate.temperature, climate.moisture,
                    metadata.temperature, metadata.moisture
            );

            // Check if within tolerance
            if (distance <= climateTolerance) {
                // Weight by inverse distance (closer = heavier)
                // Add small constant to avoid division by zero
                double weight = 1.0 / (distance + 0.1);

                candidates.add(new WeightedBiome(biome, weight, distance));
            }
        }

        return candidates;
    }

    /**
     * Selects a biome from weighted candidates.
     *
     * @param candidates list of weighted biomes
     * @param random random source
     * @return selected biome
     */
    private Holder<Biome> weightedSelect(List<WeightedBiome> candidates, RandomSource random) {
        // Calculate total weight
        double totalWeight = candidates.stream()
                .mapToDouble(wb -> wb.weight)
                .sum();

        // Random point in [0, totalWeight)
        double randomPoint = random.nextDouble() * totalWeight;

        // Find which candidate the point lands in
        double accumulated = 0.0;
        for (WeightedBiome candidate : candidates) {
            accumulated += candidate.weight;
            if (randomPoint < accumulated) {
                return candidate.biome;
            }
        }

        // Fallback to last candidate (shouldn't happen but handles floating point edge cases)
        return candidates.get(candidates.size() - 1).biome;
    }

    /**
     * Gets a fallback biome when no candidates match.
     *
     * @return a fallback biome (first in pool)
     */
    private Holder<Biome> getFallbackBiome() {
        return biomePool.iterator().next();
    }

    /**
     * Calculates Euclidean distance in climate space.
     *
     * @param temp1 temperature 1
     * @param moisture1 moisture 1
     * @param temp2 temperature 2
     * @param moisture2 moisture 2
     * @return distance
     */
    private double calculateDistance(float temp1, float moisture1, float temp2, float moisture2) {
        double tempDiff = temp1 - temp2;
        double moistDiff = moisture1 - moisture2;
        return Math.sqrt(tempDiff * tempDiff + moistDiff * moistDiff);
    }

    /**
     * Gets a deterministic seed from chunk coordinates.
     *
     * <p>This ensures the same chunk always gets the same biome selection
     * (important for consistency).
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return seed value
     */
    private long getSeed(int chunkX, int chunkZ) {
        // Use cantor pairing function for deterministic 2D -> 1D mapping
        long a = (long) chunkX;
        long b = (long) chunkZ;

        // Handle negative coordinates
        a = a >= 0 ? 2 * a : -2 * a - 1;
        b = b >= 0 ? 2 * b : -2 * b - 1;

        return (a + b) * (a + b + 1) / 2 + b;
    }

    /**
     * Represents a biome with selection weight.
     */
    private static class WeightedBiome {
        final Holder<Biome> biome;
        final double weight;
        final double distance;

        WeightedBiome(Holder<Biome> biome, double weight, double distance) {
            this.biome = biome;
            this.weight = weight;
            this.distance = distance;
        }
    }
}
