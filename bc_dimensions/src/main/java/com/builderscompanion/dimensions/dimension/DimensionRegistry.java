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
package com.builderscompanion.dimensions.dimension;

import com.builderscompanion.core.api.BCDimensionsAPI;
import com.builderscompanion.core.config.BCConfigLoader;
import com.builderscompanion.core.config.ConfigValidator;
import com.builderscompanion.core.config.DimensionConfig;
import com.builderscompanion.core.config.ValidationResult;
import com.builderscompanion.core.events.BCEvents;
import com.builderscompanion.core.util.BCLogger;
import com.builderscompanion.dimensions.BCDimensions;
import com.builderscompanion.dimensions.biome.BiomePoolBuilder;
import com.builderscompanion.dimensions.biome.RegionRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registers custom dimensions with Forge and Minecraft.
 *
 * <p>This class is responsible for:
 * <ul>
 *   <li>Loading enabled dimensions from configuration</li>
 *   <li>Validating dimension configurations</li>
 *   <li>Creating dimension ResourceKeys</li>
 *   <li>Registering dimensions with the BC API</li>
 *   <li>Firing dimension registration events</li>
 * </ul>
 *
 * <p><b>Initialization Flow:</b>
 * <ol>
 *   <li>BC-Core loads configuration files during mod initialization</li>
 *   <li>DimensionRegistry loads enabled dimensions when server starts</li>
 *   <li>Each dimension is validated and registered</li>
 *   <li>DimensionRegisteredEvent is fired for each dimension</li>
 *   <li>Other mods can react to these events</li>
 * </ol>
 *
 * <p><b>Note on Dimension Creation:</b> In Minecraft 1.20.1, custom dimensions
 * are typically added through datapacks. This registry manages the configuration
 * and coordination but relies on datapacks for the actual dimension creation.
 * Future versions may implement programmatic dimension creation.
 *
 * @since 1.0.0
 */
public class DimensionRegistry {

    // Map of registered dimension keys to their configurations
    private static final Map<ResourceKey<Level>, DimensionConfig> REGISTERED_DIMENSIONS = new HashMap<>();

    // Validator for dimension configs
    private static final ConfigValidator VALIDATOR = new ConfigValidator();

    /**
     * Registers dimensions when the server starts.
     *
     * <p>This is called automatically by Forge during server initialization.
     * We use ServerAboutToStartEvent because:
     * <ul>
     *   <li>Configuration has been loaded by BC-Core</li>
     *   <li>BiomeMetadataDB has been initialized</li>
     *   <li>Server registries are available</li>
     * </ul>
     *
     * @param event the server startup event
     */
    @SubscribeEvent
    public static void onServerStarting(ServerAboutToStartEvent event) {
        BCLogger.info("Registering custom dimensions...");

        try {
            // Load enabled dimensions from config
            List<DimensionConfig> dimensions = BCConfigLoader.getAllDimensions();

            if (dimensions.isEmpty()) {
                BCLogger.info("No custom dimensions configured");
                return;
            }

            BCLogger.debug("Found {} enabled dimensions in config", dimensions.size());

            int successCount = 0;
            int failCount = 0;

            // Register each dimension
            for (DimensionConfig config : dimensions) {
                try {
                    if (registerDimension(config)) {  // ← Pass config, not config.id
                        successCount++;
                    } else {
                        failCount++;
                    }
                } catch (Exception e) {
                    BCLogger.error("Failed to register dimension '{}': {}", config.id, e.getMessage());
                    e.printStackTrace();
                    failCount++;
                }
            }

            // NOW lock the API after all dimensions registered
            BCDimensionsAPI.completeInitialization();
            BCLogger.info("BC Dimensions API locked - no more registrations allowed");

            BCLogger.info("Dimension registration complete: {}/{} successful",
                    successCount, successCount + failCount);

            if (failCount > 0) {
                BCLogger.warn("{} dimension(s) failed to register - check logs for details", failCount);
            }

        } catch (Exception e) {
            BCLogger.error("Critical error during dimension registration: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Registers a single dimension.
     *
     * @param dimensionId the dimension ID (e.g., "avalon", "terra")
     * @return true if registration succeeded, false otherwise
     */
    private static boolean registerDimension(DimensionConfig config) {
        BCLogger.debug("Registering dimension: {}", config.id);

        // Validate configuration (already validated in BCConfigLoader, but double-check)
        ValidationResult validationResult = VALIDATOR.validate(config);
        if (!validationResult.isValid()) {
            BCLogger.error("Validation failed for dimension '{}':", config.id);
            for (String error : validationResult.getErrors()) {
                BCLogger.error("  - {}", error);
            }
            return false;
        }

        // Log warnings if any
        if (validationResult.hasWarnings()) {
            BCLogger.warn("Validation warnings for dimension '{}':", config.id);
            for (String warning : validationResult.getWarnings()) {
                BCLogger.warn("  - {}", warning);
            }
        }

        // Create dimension resource key
        ResourceKey<Level> dimensionKey = ResourceKey.create(
                Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(BCDimensions.MODID, config.id)
        );

        // Check if already registered (from datapack)
        if (REGISTERED_DIMENSIONS.containsKey(dimensionKey)) {
            BCLogger.warn("Dimension '{}' is already registered, skipping", config.id);
            return false;
        }

        // Register with BC API
        try {
            BCDimensionsAPI.registerDimension(dimensionKey, config);
        } catch (IllegalStateException e) {
            BCLogger.error("Cannot register dimension '{}' - API initialization complete", config.id);
            return false;
        } catch (IllegalArgumentException e) {
            BCLogger.error("Cannot register dimension '{}': {}", config.id, e.getMessage());
            return false;
        }

        // Store in local registry
        REGISTERED_DIMENSIONS.put(dimensionKey, config);

        // Set up biome distribution based on mode
        setupBiomeDistribution(dimensionKey, config);

        // Fire dimension registered event
        BCEvents.postDimensionRegistered(dimensionKey, config);

        BCLogger.info("Registered dimension: {} ({})", config.displayName, config.id);
        BCLogger.debug("  Mode: {}", config.biomes.mode);
        BCLogger.debug("  Portal: {}", config.portal.frameBlock);
        BCLogger.debug("  Border: {} chunks", config.worldBorder.radius);

        return true;
    }

    /**
     * Sets up biome distribution for a dimension based on its mode.
     *
     * <p>This is called after dimension registration to initialize the
     * biome distribution system (Region Mode, Climate Grid, or Layered).
     *
     * @param dimensionKey the dimension resource key
     * @param config the dimension configuration
     */
    private static void setupBiomeDistribution(ResourceKey<Level> dimensionKey, DimensionConfig config) {
        String mode = config.biomes.mode;

        BCLogger.debug("Setting up biome distribution for {} using {} mode",
                dimensionKey.location(), mode);

        switch (mode) {
            case "region":
                setupRegionMode(dimensionKey, config);
                break;

            case "climate_grid":
                BCLogger.debug("Climate Grid Mode will be initialized during world generation");
                // Climate Grid Mode is handled by custom BiomeSource during world gen
                // No setup needed during dimension registration
                break;

            case "layered":
                BCLogger.debug("Layered Mode will be initialized during world generation");
                // Layered Mode is handled by custom BiomeSource during world gen
                // No setup needed during dimension registration
                break;

            default:
                BCLogger.warn("Unknown biome mode '{}' for dimension {}. Biomes may not generate correctly.",
                        mode, dimensionKey.location());
                break;
        }
    }

    /**
     * Sets up Region Mode for a dimension.
     *
     * <p>This creates a Terrablender region for the dimension with the
     * configured biomes and registers it with Terrablender.
     *
     * @param dimensionKey the dimension resource key
     * @param config the dimension configuration
     */
    private static void setupRegionMode(ResourceKey<Level> dimensionKey, DimensionConfig config) {
        BCLogger.debug("Setting up Region Mode for {}", dimensionKey.location());

        try {
            // Build biome pool from config
            Set<net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>> biomePool =
                    BiomePoolBuilder.build(config);

            if (biomePool.isEmpty()) {
                BCLogger.error("Cannot set up Region Mode for {}: biome pool is empty",
                        dimensionKey.location());
                return;
            }

            BCLogger.debug("Built biome pool with {} biomes for {}",
                    biomePool.size(), dimensionKey.location());

            // Register Terrablender region
            RegionRegistry.registerRegion(dimensionKey, config, biomePool);

            BCLogger.info("Region Mode setup complete for {}", dimensionKey.location());

        } catch (Exception e) {
            BCLogger.error("Failed to set up Region Mode for {}: {}",
                    dimensionKey.location(), e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gets a registered dimension's configuration.
     *
     * @param dimensionKey the dimension resource key
     * @return the dimension config, or null if not registered
     */
    public static DimensionConfig getDimensionConfig(ResourceKey<Level> dimensionKey) {
        return REGISTERED_DIMENSIONS.get(dimensionKey);
    }

    /**
     * Checks if a dimension is registered.
     *
     * @param dimensionKey the dimension resource key
     * @return true if registered
     */
    public static boolean isDimensionRegistered(ResourceKey<Level> dimensionKey) {
        return REGISTERED_DIMENSIONS.containsKey(dimensionKey);
    }

    /**
     * Gets all registered dimension keys.
     *
     * @return map of dimension keys to configs
     */
    public static Map<ResourceKey<Level>, DimensionConfig> getAllDimensions() {
        return new HashMap<>(REGISTERED_DIMENSIONS);
    }

    /**
     * Gets the number of registered dimensions.
     *
     * @return dimension count
     */
    public static int getDimensionCount() {
        return REGISTERED_DIMENSIONS.size();
    }

    /**
     * Clears all registered dimensions.
     *
     * <p><b>Internal use only.</b> Used for server shutdown and reload.
     */
    public static void clear() {
        REGISTERED_DIMENSIONS.clear();
        BCLogger.debug("Cleared dimension registry");
    }
}
