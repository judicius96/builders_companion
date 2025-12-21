/*
 * MIT License
 *
 * Copyright (c) 2025 Builders Companion
 */
package com.builderscompanion.dimensions.registry;

import com.builderscompanion.core.util.BCLogger;
import com.builderscompanion.dimensions.BCDimensions;
import com.builderscompanion.dimensions.biome.FixedBiomeSource;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.BiomeSource;

/**
 * Registry for custom BiomeSource types.
 *
 * @since 1.0.0
 */
public class BCBiomeSources {

    private static boolean registered = false;

    /**
     * Registers all custom BiomeSource types.
     *
     * <p>This must be called during FMLCommonSetupEvent, not in the constructor.
     */
    public static void register() {
        if (registered) {
            return;
        }

        registerBiomeSource("fixed", FixedBiomeSource.CODEC);

        registered = true;
        BCLogger.info("Registered BiomeSource types");
    }

    /**
     * Registers a BiomeSource codec.
     *
     * @param name the biome source name
     * @param codec the codec
     */
    private static void registerBiomeSource(String name, Codec<? extends BiomeSource> codec) {
        ResourceLocation id = new ResourceLocation(BCDimensions.MODID, name);
        Registry.register(BuiltInRegistries.BIOME_SOURCE, id, codec);
        BCLogger.debug("Registered BiomeSource: {}", id);
    }
}