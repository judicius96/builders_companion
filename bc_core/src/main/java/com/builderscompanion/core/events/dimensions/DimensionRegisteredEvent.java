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
package com.builderscompanion.core.events.dimensions;

import com.builderscompanion.core.config.DimensionConfig;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired when a dimension is registered with BC:Dimensions.
 *
 * <p>Other mods can listen to this event to know when a custom dimension becomes
 * available and to access its configuration.
 *
 * <p><b>When is this fired?</b> This event is posted on the Forge event bus during
 * dimension registration, after configuration validation but before the dimension
 * is actually created in the world.
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * @SubscribeEvent
 * public static void onDimensionRegistered(DimensionRegisteredEvent event) {
 *     ResourceKey<Level> dimension = event.getDimension();
 *     DimensionConfig config = event.getConfig();
 *
 *     LOGGER.info("BC Dimension registered: {} with mode {}",
 *         dimension.location(), config.biomes.mode);
 *
 *     // Add custom structures to this dimension
 *     if (config.biomes.mode.equals("climate_grid")) {
 *         registerCustomStructures(dimension);
 *     }
 * }
 * }</pre>
 *
 * <p><b>Event Bus:</b> This event is posted on {@link net.minecraftforge.common.MinecraftForge#EVENT_BUS}.
 *
 * <p><b>Cancellable:</b> No, this event cannot be cancelled.
 *
 * @since 1.0.0
 */
public class DimensionRegisteredEvent extends Event {

    private final ResourceKey<Level> dimension;
    private final DimensionConfig config;

    /**
     * Creates a new dimension registered event.
     *
     * @param dimension the dimension resource key
     * @param config the dimension configuration
     */
    public DimensionRegisteredEvent(ResourceKey<Level> dimension, DimensionConfig config) {
        this.dimension = dimension;
        this.config = config;
    }

    /**
     * Gets the dimension that was registered.
     *
     * @return the dimension resource key
     */
    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    /**
     * Gets the configuration for the registered dimension.
     *
     * <p><b>Note:</b> This configuration is read-only. Modifications will not
     * affect the actual dimension generation.
     *
     * @return the dimension configuration
     */
    public DimensionConfig getConfig() {
        return config;
    }

    /**
     * Gets the display name of the dimension.
     *
     * <p>This is a convenience method equivalent to {@code getConfig().displayName}.
     *
     * @return the dimension display name
     */
    public String getDisplayName() {
        return config.displayName;
    }

    /**
     * Gets the biome mode of the dimension.
     *
     * <p>This is a convenience method equivalent to {@code getConfig().biomes.mode}.
     *
     * @return the biome mode ("region", "climate_grid", or "layered")
     */
    public String getBiomeMode() {
        return config.biomes.mode;
    }
}
