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
package com.builderscompanion.core.api;

import com.builderscompanion.core.config.DimensionConfig;
import com.builderscompanion.core.util.BCLogger;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Main API for Builders Companion: Dimensions.
 *
 * <p>This API allows other mods to:
 * <ul>
 *   <li>Query registered custom dimensions</li>
 *   <li>Access dimension configurations</li>
 *   <li>Register portal providers (for BC:Portals)</li>
 *   <li>Register biome/geode/fluid modifiers</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * // Query dimension configuration
 * ResourceKey<Level> avalon = ResourceKey.create(Registry.DIMENSION_REGISTRY,
 *     new ResourceLocation("builderscompanion", "avalon"));
 * DimensionConfig config = BCDimensionsAPI.getDimensionConfig(avalon);
 *
 * // Register a custom portal provider
 * BCDimensionsAPI.registerPortalProvider(new MyPortalProvider());
 *
 * // Register a biome modifier
 * BCDimensionsAPI.registerBiomeModifier(new MyBiomeModifier());
 * }</pre>
 *
 * <p><b>Thread Safety:</b> All methods in this API are thread-safe and can be called
 * from any thread. However, registration methods should only be called during mod
 * initialization (FMLCommonSetupEvent).
 *
 * @since 1.0.0
 */
public class BCDimensionsAPI {

    // Registry of all custom dimensions
    private static final ConcurrentMap<ResourceKey<Level>, DimensionConfig> DIMENSION_REGISTRY = new ConcurrentHashMap<>();

    // Portal provider (only one can be registered, for BC:Portals)
    private static volatile IPortalProvider portalProvider = null;

    // Biome modifiers (for BC:Fluids and other mods)
    private static final List<IBiomeModifier> biomeModifiers = Collections.synchronizedList(new ArrayList<>());

    // Geode providers (for BC:Geodes)
    private static final List<IGeodeProvider> geodeProviders = Collections.synchronizedList(new ArrayList<>());

    // Initialization phase tracking
    private static volatile boolean initializationComplete = false;

    /**
     * Registers a custom dimension with BC:Dimensions.
     *
     * <p>This should be called during mod initialization, typically in the
     * FMLCommonSetupEvent handler.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * ResourceKey<Level> myDim = ResourceKey.create(Registry.DIMENSION_REGISTRY,
     *     new ResourceLocation("mymod", "mydimension"));
     * DimensionConfig config = loadMyConfig();
     * BCDimensionsAPI.registerDimension(myDim, config);
     * }</pre>
     *
     * @param dimension the dimension resource key
     * @param config the dimension configuration
     * @throws IllegalArgumentException if dimension is null or already registered
     * @throws IllegalStateException if called after initialization phase
     */
    public static void registerDimension(ResourceKey<Level> dimension, DimensionConfig config) {
        if (dimension == null) {
            throw new IllegalArgumentException("Dimension key cannot be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("Dimension config cannot be null");
        }
        if (initializationComplete) {
            throw new IllegalStateException("Cannot register dimensions after initialization is complete");
        }
        if (DIMENSION_REGISTRY.containsKey(dimension)) {
            throw new IllegalArgumentException("Dimension " + dimension + " is already registered");
        }

        DIMENSION_REGISTRY.put(dimension, config);
        BCLogger.info("Registered dimension via API: {}", dimension.location());
    }

    /**
     * Gets the configuration for a registered dimension.
     *
     * <p>This can be called at any time to query dimension settings.
     *
     * @param dimension the dimension resource key
     * @return the dimension config, or null if not registered
     */
    @Nullable
    public static DimensionConfig getDimensionConfig(ResourceKey<Level> dimension) {
        if (dimension == null) {
            return null;
        }
        return DIMENSION_REGISTRY.get(dimension);
    }

    /**
     * Gets all custom dimensions registered by BC:Dimensions.
     *
     * <p>The returned list is unmodifiable and contains all dimensions registered
     * via configuration files or the API.
     *
     * @return unmodifiable list of dimension keys
     */
    public static List<ResourceKey<Level>> getAllCustomDimensions() {
        return Collections.unmodifiableList(new ArrayList<>(DIMENSION_REGISTRY.keySet()));
    }

    /**
     * Checks if a dimension is registered with BC:Dimensions.
     *
     * @param dimension the dimension resource key
     * @return true if the dimension is registered, false otherwise
     */
    public static boolean isDimensionRegistered(ResourceKey<Level> dimension) {
        return dimension != null && DIMENSION_REGISTRY.containsKey(dimension);
    }

    /**
     * Registers a portal provider.
     *
     * <p>Only one portal provider can be registered at a time. This is intended
     * for BC:Portals to override the default portal behavior.
     *
     * <p>If a portal provider is already registered, this will replace it and log a warning.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * public class MyPortalProvider implements IPortalProvider {
     *     // Implementation
     * }
     *
     * BCDimensionsAPI.registerPortalProvider(new MyPortalProvider());
     * }</pre>
     *
     * @param provider the portal provider to register
     * @throws IllegalArgumentException if provider is null
     * @throws IllegalStateException if called after initialization phase
     * @see IPortalProvider
     */
    public static void registerPortalProvider(IPortalProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("Portal provider cannot be null");
        }
        if (initializationComplete) {
            throw new IllegalStateException("Cannot register portal provider after initialization is complete");
        }

        if (portalProvider != null) {
            BCLogger.warn("Replacing existing portal provider {} with {}",
                    portalProvider.getClass().getName(), provider.getClass().getName());
        }

        portalProvider = provider;
        BCLogger.info("Registered portal provider: {}", provider.getClass().getSimpleName());
    }

    /**
     * Gets the currently registered portal provider.
     *
     * @return the portal provider, or null if none is registered
     */
    @Nullable
    public static IPortalProvider getPortalProvider() {
        return portalProvider;
    }

    /**
     * Registers a biome modifier.
     *
     * <p>Biome modifiers can change which biome is placed at a location during
     * chunk generation. This is useful for mods like BC:Fluids that want to swap
     * biomes based on water color.
     *
     * <p>Multiple biome modifiers can be registered and will be called in registration order.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * public class MyBiomeModifier implements IBiomeModifier {
     *     public Holder<Biome> modifyBiome(Holder<Biome> original, BiomeContext context) {
     *         // Custom logic to swap biomes
     *         return original;
     *     }
     * }
     *
     * BCDimensionsAPI.registerBiomeModifier(new MyBiomeModifier());
     * }</pre>
     *
     * @param modifier the biome modifier to register
     * @throws IllegalArgumentException if modifier is null
     * @throws IllegalStateException if called after initialization phase
     * @see IBiomeModifier
     */
    public static void registerBiomeModifier(IBiomeModifier modifier) {
        if (modifier == null) {
            throw new IllegalArgumentException("Biome modifier cannot be null");
        }
        if (initializationComplete) {
            throw new IllegalStateException("Cannot register biome modifier after initialization is complete");
        }

        biomeModifiers.add(modifier);
        BCLogger.info("Registered biome modifier: {}", modifier.getClass().getSimpleName());
    }

    /**
     * Gets all registered biome modifiers.
     *
     * <p>The returned list is unmodifiable.
     *
     * @return unmodifiable list of biome modifiers
     */
    public static List<IBiomeModifier> getBiomeModifiers() {
        return Collections.unmodifiableList(new ArrayList<>(biomeModifiers));
    }

    /**
     * Registers a geode provider.
     *
     * <p>Geode providers add custom geodes to dimensions. This is intended for
     * BC:Geodes to add amethyst-style geodes with custom blocks.
     *
     * <p>Multiple geode providers can be registered.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * public class MyGeodeProvider implements IGeodeProvider {
     *     // Implementation
     * }
     *
     * BCDimensionsAPI.registerGeodeProvider(new MyGeodeProvider());
     * }</pre>
     *
     * @param provider the geode provider to register
     * @throws IllegalArgumentException if provider is null
     * @throws IllegalStateException if called after initialization phase
     * @see IGeodeProvider
     */
    public static void registerGeodeProvider(IGeodeProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("Geode provider cannot be null");
        }
        if (initializationComplete) {
            throw new IllegalStateException("Cannot register geode provider after initialization is complete");
        }

        geodeProviders.add(provider);
        BCLogger.info("Registered geode provider: {}", provider.getClass().getSimpleName());
    }

    /**
     * Gets all registered geode providers.
     *
     * <p>The returned list is unmodifiable.
     *
     * @return unmodifiable list of geode providers
     */
    public static List<IGeodeProvider> getGeodeProviders() {
        return Collections.unmodifiableList(new ArrayList<>(geodeProviders));
    }

    /**
     * Marks the initialization phase as complete.
     *
     * <p>This is called internally by BC-Core after all mods have had a chance to register.
     * After this is called, no more registrations are allowed.
     *
     * <p><b>Internal use only.</b> Do not call this from external mods.
     */
    public static void completeInitialization() {
        initializationComplete = true;
        BCLogger.info("BC Dimensions API initialization complete: {} dimensions, {} biome modifiers, {} geode providers",
                DIMENSION_REGISTRY.size(), biomeModifiers.size(), geodeProviders.size());
    }

    /**
     * Clears all registrations.
     *
     * <p>This is used for testing and hot reload. Resets the initialization state.
     *
     * <p><b>Internal use only.</b> Do not call this from external mods.
     */
    public static void clearRegistrations() {
        DIMENSION_REGISTRY.clear();
        portalProvider = null;
        biomeModifiers.clear();
        geodeProviders.clear();
        initializationComplete = false;
        BCLogger.debug("Cleared all BC Dimensions API registrations");
    }
}
