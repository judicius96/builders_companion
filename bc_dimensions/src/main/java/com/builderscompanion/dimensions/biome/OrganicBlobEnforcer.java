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
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import net.minecraft.util.RandomSource;

import java.util.List;

/**
 * Enforces minimum biome sizes with organic blob shapes.
 *
 * <p>This solves the "1-chunk biome patch" problem where climate-based selection
 * can create tiny, unnatural biome patches. Instead, we:
 * <ol>
 *   <li>Snap coordinates to a "blob grid" with random offsets</li>
 *   <li>Use noise to create irregular blob boundaries</li>
 *   <li>Ensure blobs are at least minBlobSize chunks across</li>
 * </ol>
 *
 * <p><b>Key Innovation: Noise-Offset Grid</b>
 * <p>Instead of a regular grid (which creates visible seams):
 * <pre>
 * Regular grid (bad):
 * F F F F | P P P P
 * F F F F | P P P P
 * --------+-------- ← visible seam
 * F F F F | P P P P
 *
 * Noise-offset grid (good):
 * F F F P P P P P
 * F F F F P P P P
 * F F F F F P P P ← irregular boundary
 * F F F F F F P P
 * </pre>
 *
 * <p><b>Algorithm:</b>
 * <ol>
 *   <li>Divide world into blob cells (minBlobSize x minBlobSize)</li>
 *   <li>For each cell, generate random X/Z offset</li>
 *   <li>Add noise-based perturbation to create organic edges</li>
 *   <li>Sample biome at offset cell center</li>
 * </ol>
 *
 * <p><b>Configuration Parameters:</b>
 * <ul>
 *   <li><b>minBlobSize</b> - Minimum blob size in chunks (e.g., 8)</li>
 *   <li><b>irregularity</b> - How much edges wobble (0.0 to 1.0)</li>
 *   <li><b>coherence</b> - How well blob stays together (0.0 to 1.0)</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class OrganicBlobEnforcer {

    private final int minBlobSize;
    private final double irregularity;
    private final double coherence;
    private final PerlinSimplexNoise offsetNoise;
    private final PerlinSimplexNoise edgeNoise;
    private final long seed;

    /**
     * Creates a new organic blob enforcer.
     *
     * @param config the climate grid configuration
     * @param seed the world seed
     */
    public OrganicBlobEnforcer(DimensionConfig.ClimateGridConfig config, long seed) {
        this.minBlobSize = (int) Math.sqrt(config.minBiomeSizeChunks);
        this.irregularity = config.blobIrregularity;
        this.coherence = config.blobCoherence;
        this.seed = seed;

        // Create noise generators
        RandomSource randomSource = RandomSource.create(seed);
        this.offsetNoise = new PerlinSimplexNoise(randomSource, List.of(0));
        this.edgeNoise = new PerlinSimplexNoise(randomSource, List.of(1));

        BCLogger.debug("Created OrganicBlobEnforcer: minSize={}, irregularity={}, coherence={}",
                minBlobSize, irregularity, coherence);
    }

    /**
     * Snaps chunk coordinates to a blob cell.
     *
     * <p>This is the main entry point. It transforms input coordinates to
     * blob-grid coordinates with organic boundaries.
     *
     * @param chunkX input chunk X
     * @param chunkZ input chunk Z
     * @return snapped coordinates
     */
    public SnappedCoords snapToBlob(int chunkX, int chunkZ) {
        // Determine which blob cell this chunk belongs to
        int cellX = Math.floorDiv(chunkX, minBlobSize);
        int cellZ = Math.floorDiv(chunkZ, minBlobSize);

        // Get base cell center
        int baseCenterX = cellX * minBlobSize + minBlobSize / 2;
        int baseCenterZ = cellZ * minBlobSize + minBlobSize / 2;

        // Apply random offset to cell center (for irregularity)
        int offsetX = getRandomOffset(cellX, cellZ, 0);
        int offsetZ = getRandomOffset(cellX, cellZ, 1);

        // Apply noise-based edge perturbation
        double edgePerturbX = getEdgePerturbation(chunkX, chunkZ, 0);
        double edgePerturbZ = getEdgePerturbation(chunkX, chunkZ, 1);

        // Combine base + offset + perturbation
        int finalX = baseCenterX + offsetX + (int) (edgePerturbX * irregularity * minBlobSize * 0.5);
        int finalZ = baseCenterZ + offsetZ + (int) (edgePerturbZ * irregularity * minBlobSize * 0.5);

        return new SnappedCoords(finalX, finalZ);
    }

    /**
     * Gets a random offset for a blob cell.
     *
     * <p>This creates the main irregularity in blob shapes. Each cell gets a
     * consistent random offset based on its grid position.
     *
     * @param cellX cell X coordinate
     * @param cellZ cell Z coordinate
     * @param axis 0 for X, 1 for Z
     * @return offset in chunks
     */
    private int getRandomOffset(int cellX, int cellZ, int axis) {
        // Create a unique seed for this cell + axis
        long cellSeed = seed;
        cellSeed = cellSeed * 31 + cellX;
        cellSeed = cellSeed * 31 + cellZ;
        cellSeed = cellSeed * 31 + axis;

        RandomSource random = RandomSource.create(cellSeed);

        // Offset by up to ±irregularity * minBlobSize * 0.3
        int maxOffset = (int) (irregularity * minBlobSize * 0.3);
        return random.nextInt(-maxOffset, maxOffset + 1);
    }

    /**
     * Gets edge perturbation using noise.
     *
     * <p>This creates the wavy blob boundaries. Uses coherence to control
     * how much the edges wobble.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @param axis 0 for X, 1 for Z
     * @return perturbation value (-1.0 to 1.0)
     */
    private double getEdgePerturbation(int chunkX, int chunkZ, int axis) {
        // Scale for noise frequency (higher = more detail)
        double scale = minBlobSize * (1.0 + coherence);

        double x = chunkX / scale;
        double z = chunkZ / scale;

        // Sample noise with axis offset for different patterns per axis
        double noise = edgeNoise.getValue(x + axis * 1000, z + axis * 1000, false);

        // Apply coherence scaling (higher coherence = less wobble)
        return noise * (1.0 - coherence * 0.5);
    }

    /**
     * Represents snapped coordinates.
     */
    public static class SnappedCoords {
        /** Snapped chunk X coordinate */
        public final int chunkX;

        /** Snapped chunk Z coordinate */
        public final int chunkZ;

        public SnappedCoords(int chunkX, int chunkZ) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        @Override
        public String toString() {
            return String.format("(%d, %d)", chunkX, chunkZ);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SnappedCoords)) return false;
            SnappedCoords that = (SnappedCoords) o;
            return chunkX == that.chunkX && chunkZ == that.chunkZ;
        }

        @Override
        public int hashCode() {
            return 31 * chunkX + chunkZ;
        }
    }
}
