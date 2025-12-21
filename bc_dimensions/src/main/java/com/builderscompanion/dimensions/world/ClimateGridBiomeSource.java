/*
 * MIT License
 * Copyright (c) 2025 Builders Companion
 */
package com.builderscompanion.dimensions.world;

import com.builderscompanion.core.config.DimensionConfig;
import com.builderscompanion.dimensions.biome.ClimateGridGenerator;
import com.builderscompanion.dimensions.biome.ClimateBiomeSelector;
import com.builderscompanion.dimensions.biome.OrganicBlobEnforcer;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.Set;
import java.util.stream.Stream;  // <-- added

public class ClimateGridBiomeSource extends BiomeSource {

    public static final Codec<ClimateGridBiomeSource> CODEC = null;

    private final ClimateGridGenerator climateGen;
    private final ClimateBiomeSelector biomeSelector;
    private final OrganicBlobEnforcer blobEnforcer;
    private final Set<Holder<Biome>> biomePool;
    private final long seed;

    public ClimateGridBiomeSource(DimensionConfig.ClimateGridConfig config,
                                  Set<Holder<Biome>> biomePool,
                                  long seed) {
        this.biomePool = biomePool;
        this.seed = seed;
        this.climateGen = new ClimateGridGenerator(config, seed);
        this.biomeSelector = new ClimateBiomeSelector(config, biomePool);
        this.blobEnforcer = new OrganicBlobEnforcer(config, seed);
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        int chunkX = quartX >> 2;
        int chunkZ = quartZ >> 2;

        OrganicBlobEnforcer.SnappedCoords snapped = blobEnforcer.snapToBlob(chunkX, chunkZ);
        ClimateGridGenerator.Climate climate = climateGen.getClimate(snapped.chunkX, snapped.chunkZ);

        long mixedSeed = seed ^ (((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL));
        RandomSource random = RandomSource.create(mixedSeed);

        return biomeSelector.selectBiome(climate, snapped.chunkX, snapped.chunkZ, random);
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        // BiomeSource expects a Stream<Holder<Biome>>
        return biomePool.stream();
    }
}
