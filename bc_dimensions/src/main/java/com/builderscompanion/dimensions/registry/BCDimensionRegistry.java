/*
 * MIT License
 *
 * Copyright (c) 2025 Builders Companion
 */
package com.builderscompanion.dimensions.registry;

import com.builderscompanion.core.config.DimensionConfig;
import com.builderscompanion.core.util.BCLogger;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages registration of custom dimensions.
 *
 * <p>This class:
 * <ul>
 *   <li>Creates ResourceKeys for dimensions</li>
 *   <li>Tracks registered dimensions</li>
 *   <li>Provides lookup methods</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class BCDimensionRegistry {

    private static final Map<String, ResourceKey<Level>> DIMENSION_KEYS = new HashMap<>();
    private static final Map<ResourceKey<Level>, DimensionConfig> DIMENSION_CONFIGS = new HashMap<>();

    /**
     * Registers a dimension and stores its config.
     *
     * @param config the dimension configuration
     * @return the ResourceKey for this dimension
     */
    public static ResourceKey<Level> registerDimension(DimensionConfig config) {
        ResourceLocation location = new ResourceLocation("bc_dimensions", config.id);
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, location);

        DIMENSION_KEYS.put(config.id, key);
        DIMENSION_CONFIGS.put(key, config);

        BCLogger.debug("Registered dimension key: bc_dimensions:{}", config.id);
        return key;
    }

    /**
     * Gets the ResourceKey for a dimension by ID.
     *
     * @param dimensionId the dimension ID (e.g., "sand_world")
     * @return the ResourceKey, or null if not registered
     */
    public static ResourceKey<Level> getDimensionKey(String dimensionId) {
        return DIMENSION_KEYS.get(dimensionId);
    }

    /**
     * Gets the config for a dimension.
     *
     * @param key the dimension ResourceKey
     * @return the dimension config, or null if not registered
     */
    public static DimensionConfig getDimensionConfig(ResourceKey<Level> key) {
        return DIMENSION_CONFIGS.get(key);
    }

    /**
     * Gets all registered dimension keys.
     *
     * @return map of dimension ID to ResourceKey
     */
    public static Map<String, ResourceKey<Level>> getAllDimensions() {
        return new HashMap<>(DIMENSION_KEYS);
    }
}