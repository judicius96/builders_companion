/*
 * MIT License
 *
 * Copyright (c) 2025 Builders Companion
 */
package com.builderscompanion.dimensions.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.stream.Stream;

/**
 * Simple BiomeSource that returns the same biome for every chunk.
 *
 * <p>This is used for dimensions with a single biome type (like sand_world with only desert).
 *
 * @since 1.0.0
 */
public class FixedBiomeSource extends BiomeSource {

    public static final Codec<FixedBiomeSource> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("biome").forGetter(source -> source.biomeId)
            ).apply(instance, FixedBiomeSource::new)
    );

    private final ResourceLocation biomeId;
    private Holder<Biome> biome;

    public FixedBiomeSource(ResourceLocation biomeId) {
        this.biomeId = biomeId;
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        if (biome == null) {
            return Stream.empty();
        }
        return Stream.of(biome);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        // Lazy init - biome registry not available during construction
        if (biome == null) {
            initBiome();
        }
        return biome;
    }

    /**
     * Initializes the biome holder from the registry.
     */
    private void initBiome() {
        // This will be called when the world loads and registries are available
        // For now, we'll return a default - this needs proper registry access
        // TODO: Get biome from registry properly
    }

    /**
     * Sets the biome holder directly (used during dimension setup).
     *
     * @param biome the biome holder
     */
    public void setBiome(Holder<Biome> biome) {
        this.biome = biome;
    }
}