/*
 * MIT License
 * Copyright (c) 2025 Builders Companion
 */
package com.builderscompanion.dimensions.world;

import com.builderscompanion.core.config.DimensionConfig;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class LayeredBiomeSource extends BiomeSource {

    // Placeholder – you can wire a real codec later when you do JSON/data-driven setup.
    public static final Codec<LayeredBiomeSource> CODEC = null;

    private final List<DimensionConfig.BiomeLayer> layers;
    private final long seed;
    private final PerlinSimplexNoise caveNoise;
    private final Set<Holder<Biome>> possibleBiomes;

    public LayeredBiomeSource(List<DimensionConfig.BiomeLayer> layers, long seed) {
        this.layers = layers;
        this.seed = seed;
        this.caveNoise = new PerlinSimplexNoise(RandomSource.create(seed), List.of(0));
        this.possibleBiomes = collectBiomesFromLayers(layers);
    }

    private Set<Holder<Biome>> collectBiomesFromLayers(List<DimensionConfig.BiomeLayer> layers) {
        Set<Holder<Biome>> result = new HashSet<>();

        for (DimensionConfig.BiomeLayer layer : layers) {
            // List of biomes for the layer
            if (layer.biomes != null) {
                for (String biomeId : layer.biomes) {
                    addBiome(result, biomeId);
                }
            }

            // Single-biome mode
            if (layer.biome != null && !layer.biome.isEmpty()) {
                addBiome(result, layer.biome);
            }

            // Cave biome
            if (layer.caveBiome != null && !layer.caveBiome.isEmpty()) {
                addBiome(result, layer.caveBiome);
            }
        }

        return result;
    }

    private void addBiome(Set<Holder<Biome>> set, String idString) {
        if (idString == null || idString.isEmpty()) return;

        ResourceLocation id = new ResourceLocation(idString);
        Biome biome = ForgeRegistries.BIOMES.getValue(id);
        if (biome != null) {
            set.add(Holder.direct(biome));
        }
    }

    // -------- BiomeSource overrides --------

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    // This MUST match the abstract method in your BiomeSource.
    // Same as in your now-working ClimateGridBiomeSource.
    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return possibleBiomes.stream();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        if (possibleBiomes.isEmpty()) {
            throw new IllegalStateException("LayeredBiomeSource has no possible biomes; check your dimension config.");
        }

        int worldY = quartY << 2;
        DimensionConfig.BiomeLayer layer = findLayer(worldY);

        if (layer == null) {
            // No matching layer, give *some* biome so worldgen doesn't crash
            return possibleBiomes.iterator().next();
        }

        // Single-biome layer
        if ("single".equals(layer.biomeMode) && layer.biome != null && !layer.biome.isEmpty()) {
            Holder<Biome> biome = getBiomeHolder(layer.biome);
            if (biome != null) return biome;
        }

        // Cave biome
        if (shouldBeCave(quartX, quartZ, layer)
                && layer.caveBiome != null && !layer.caveBiome.isEmpty()) {
            Holder<Biome> caveBiome = getBiomeHolder(layer.caveBiome);
            if (caveBiome != null) return caveBiome;
        }

        // Multi-biome list
        if (layer.biomes != null && !layer.biomes.isEmpty()) {
            int index = Math.abs((quartX + quartZ * 31) % layer.biomes.size());
            String biomeId = layer.biomes.get(index);
            Holder<Biome> biome = getBiomeHolder(biomeId);
            if (biome != null) return biome;
        }

        // Fallback if config has bad IDs
        return possibleBiomes.iterator().next();
    }

    // -------- Internal helpers --------

    private DimensionConfig.BiomeLayer findLayer(int worldY) {
        for (DimensionConfig.BiomeLayer layer : layers) {
            if (worldY >= layer.yMin && worldY <= layer.yMax) {
                return layer;
            }
        }
        return null;
    }

    private boolean shouldBeCave(int quartX, int quartZ, DimensionConfig.BiomeLayer layer) {
        if (layer.cavePercentage <= 0) return false;

        // In your mappings: PerlinSimplexNoise has getValue(double x, double z, boolean useOrigin)
        double noise = caveNoise.getValue(quartX * 0.05, quartZ * 0.05, false);

        double threshold = (layer.cavePercentage / 100.0) * 2.0 - 1.0;
        return noise < threshold;
    }

    private Holder<Biome> getBiomeHolder(String idString) {
        if (idString == null || idString.isEmpty()) return null;

        ResourceLocation id = new ResourceLocation(idString);
        Biome biome = ForgeRegistries.BIOMES.getValue(id);
        if (biome == null) return null;

        return Holder.direct(biome);
    }
}
