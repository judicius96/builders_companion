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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

import java.util.List;

/**
 * Generates climate values based on world coordinates.
 *
 * <p>This implements Climate Grid Mode's geographic climate system where:
 * <ul>
 *   <li><b>North → South</b> creates temperature gradient (cold → hot)</li>
 *   <li><b>West → East</b> creates moisture gradient (wet → dry)</li>
 *   <li><b>Noise</b> adds natural variation (~10%)</li>
 *   <li><b>Boundaries</b> can reverse for bounded "Vintage Story" style worlds</li>
 * </ul>
 *
 * <p><b>Example Climate Zones:</b>
 * <pre>
 * Northwest: Cold + Wet = Taiga
 * Northeast: Cold + Dry = Tundra
 * Southwest: Hot + Wet = Jungle
 * Southeast: Hot + Dry = Desert
 * </pre>
 *
 * <p><b>Coordinate Scale:</b>
 * <p>Input coordinates are in "chunk coordinates" (divide block coords by 16).
 * Output climate values are -1.0 to 1.0.
 *
 * @since 1.0.0
 */
public class ClimateGridGenerator {

    private final DimensionConfig.ClimateGridConfig config;
    private final PerlinSimplexNoise temperatureNoise;
    private final PerlinSimplexNoise moistureNoise;
    private final int spawnChunkX;
    private final int spawnChunkZ;
    private final int boundaryChunks;
    private final boolean useReversal;

    /**
     * Creates a new climate grid generator.
     *
     * @param config the climate grid configuration
     * @param seed the world seed for noise generation
     */
    public ClimateGridGenerator(DimensionConfig.ClimateGridConfig config, long seed) {
        this.config = config;
        this.spawnChunkX = config.spawnLocation[0];  // Already in chunks
        this.spawnChunkZ = config.spawnLocation[1];  // Already in chunks
        this.boundaryChunks = config.boundaryChunks;
        this.useReversal = config.reversal;

        // Create noise generators for variation
        RandomSource randomSource = RandomSource.create(seed);
        this.temperatureNoise = new PerlinSimplexNoise(randomSource, List.of(0));
        this.moistureNoise = new PerlinSimplexNoise(randomSource, List.of(1));

        BCLogger.debug("Created ClimateGridGenerator: spawn=({}, {}), boundary={} chunks, reversal={}",
                spawnChunkX, spawnChunkZ, boundaryChunks, useReversal);
    }

    /**
     * Gets the climate at a specific chunk position.
     *
     * <p>This is the main entry point for climate calculation.
     *
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return the climate at this position
     */
    public Climate getClimate(int chunkX, int chunkZ) {
        // Calculate distance from spawn
        int distX = chunkX - spawnChunkX;
        int distZ = chunkZ - spawnChunkZ;

        // Apply boundary and reversal if configured
        distX = applyBoundary(distX, boundaryChunks, useReversal);
        distZ = applyBoundary(distZ, boundaryChunks, useReversal);

        // Calculate base gradients
        float temperature = calculateTemperatureGradient(distZ);
        float moisture = calculateMoistureGradient(distX);

        // Add noise for natural variation
        temperature += sampleNoise(temperatureNoise, chunkX, chunkZ) * 0.1f;
        moisture += sampleNoise(moistureNoise, chunkX, chunkZ) * 0.1f;

        // Clamp to -1.0 to 1.0
        temperature = Mth.clamp(temperature, -1.0f, 1.0f);
        moisture = Mth.clamp(moisture, -1.0f, 1.0f);

        return new Climate(temperature, moisture);
    }

    /**
     * Calculates temperature based on Z-axis distance (north-south).
     *
     * <p><b>Gradient:</b>
     * <ul>
     *   <li>North (-distZ) → Cold (-1.0)</li>
     *   <li>Spawn (0) → Moderate (configurable)</li>
     *   <li>South (+distZ) → Hot (+1.0)</li>
     * </ul>
     *
     * @param distZ distance from spawn in Z direction (chunks)
     * @return temperature value (-1.0 to 1.0)
     */
    private float calculateTemperatureGradient(int distZ) {
        float north = (float) config.temperature.north;
        float south = (float) config.temperature.south;
        float spawn = (float) config.temperature.spawn;

        if (distZ == 0) return spawn;

        float ratio = Math.abs((float) distZ / boundaryChunks);
        ratio = Mth.clamp(ratio, 0.0f, 1.0f);

        if (distZ < 0) {
            // North (cold)
            return Mth.lerp(ratio, spawn, north);
        } else {
            // South (hot)
            return Mth.lerp(ratio, spawn, south);
        }
    }

    /**
     * Calculates moisture based on X-axis distance (west-east).
     *
     * <p><b>Gradient:</b>
     * <ul>
     *   <li>West (-distX) → Wet (-1.0 or configurable)</li>
     *   <li>Spawn (0) → Moderate (configurable)</li>
     *   <li>East (+distX) → Dry (+1.0 or configurable)</li>
     * </ul>
     *
     * @param distX distance from spawn in X direction (chunks)
     * @return moisture value (-1.0 to 1.0)
     */
    private float calculateMoistureGradient(int distX) {
        float west = (float) config.moisture.west;
        float east = (float) config.moisture.east;
        float spawn = (float) config.moisture.spawn;

        if (distX == 0) return spawn;

        float ratio = Math.abs((float) distX / boundaryChunks);
        ratio = Mth.clamp(ratio, 0.0f, 1.0f);

        if (distX < 0) {
            // West (wet)
            return Mth.lerp(ratio, spawn, west);
        } else {
            // East (dry)
            return Mth.lerp(ratio, spawn, east);
        }
    }

    /**
     * Applies boundary behavior to a distance value.
     *
     * <p>If reversal is enabled, coordinates "bounce back" at boundaries
     * creating a Vintage Story-style bounded world.
     *
     * <p><b>Examples (with boundary=2500 chunks):</b>
     * <pre>
     * No reversal:
     *   dist=3000 → clamped to 2500
     *
     * With reversal:
     *   dist=2600 → reflected to 2400 (100 chunks past boundary)
     *   dist=5100 → reflected to -100 (wraps around)
     * </pre>
     *
     * @param dist the distance from spawn
     * @param boundary the boundary distance in chunks
     * @param reversal whether to use reversal behavior
     * @return adjusted distance
     */
    private int applyBoundary(int dist, int boundary, boolean reversal) {
        if (!reversal) {
            // Simple clamping
            return Mth.clamp(dist, -boundary, boundary);
        }

        // Sawtooth pattern for reversal
        if (Math.abs(dist) <= boundary) {
            return dist;  // Within boundary, no change
        }

        // Calculate how far past boundary
        int sign = dist > 0 ? 1 : -1;
        int overshoot = Math.abs(dist) - boundary;

        // Reflect back
        // Pattern: boundary -> boundary-overshoot -> -boundary -> etc.
        int cycle = overshoot / (boundary * 2);
        int remainder = overshoot % (boundary * 2);

        if (cycle % 2 == 0) {
            // Even cycle: going back towards spawn
            return sign * (boundary - remainder);
        } else {
            // Odd cycle: going away from spawn (opposite direction)
            return -sign * (remainder - boundary);
        }
    }

    /**
     * Samples Perlin noise at a position.
     *
     * @param noise the noise generator
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return noise value (-1.0 to 1.0)
     */
    private float sampleNoise(PerlinSimplexNoise noise, int chunkX, int chunkZ) {
        // Use blob noise scale from config
        double scale = config.blobNoiseScale * 100.0;  // Convert to reasonable scale
        double x = chunkX / scale;
        double z = chunkZ / scale;

        return (float) noise.getValue(x, z, false);
    }

    /**
     * Represents a climate point with temperature and moisture.
     */
    public static class Climate {
        /** Temperature value (-1.0 = cold, 1.0 = hot) */
        public final float temperature;

        /** Moisture value (-1.0 = dry, 1.0 = wet) */
        public final float moisture;

        public Climate(float temperature, float moisture) {
            this.temperature = temperature;
            this.moisture = moisture;
        }

        /**
         * Calculates Euclidean distance to another climate.
         *
         * @param other the other climate
         * @return distance (0.0 = identical, ~2.83 = maximum)
         */
        public double distanceTo(Climate other) {
            double tempDiff = this.temperature - other.temperature;
            double moistDiff = this.moisture - other.moisture;
            return Math.sqrt(tempDiff * tempDiff + moistDiff * moistDiff);
        }

        @Override
        public String toString() {
            return String.format("Climate{temp=%.2f, moisture=%.2f}", temperature, moisture);
        }
    }
}
