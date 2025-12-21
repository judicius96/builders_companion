/*
 * MIT License
 * Copyright (c) 2025 Builders Companion
 */
package com.builderscompanion.core.api;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Portal color registry API for BC mod suite.
 *
 * BC:Dimensions registers dimension colors here.
 * BC:Portals reads from here to auto-link portals.
 */
public class BCPortalAPI {

    private static final Map<Integer, ResourceKey<Level>> colorToDimension = new ConcurrentHashMap<>();
    private static boolean locked = false;

    /**
     * Registers a color to dimension mapping.
     *
     * @param hexColor The hex color (e.g., 0x3C44AA for blue)
     * @param dimension The dimension this color maps to
     * @throws IllegalStateException if API is locked
     */
    public static void registerColorMapping(int hexColor, ResourceKey<Level> dimension) {
        if (locked) {
            throw new IllegalStateException("Cannot register colors after initialization");
        }

        if (colorToDimension.containsKey(hexColor)) {
            throw new IllegalArgumentException(
                    "Color #" + Integer.toHexString(hexColor) + " already registered to " +
                            colorToDimension.get(hexColor).location()
            );
        }

        colorToDimension.put(hexColor, dimension);
    }

    /**
     * Gets the dimension mapped to a color.
     *
     * @param hexColor The hex color
     * @return The dimension, or null if not registered
     */
    public static ResourceKey<Level> getDestinationByColor(int hexColor) {
        return colorToDimension.get(hexColor);
    }

    /**
     * Gets all registered color mappings.
     *
     * @return Unmodifiable map of colors to dimensions
     */
    public static Map<Integer, ResourceKey<Level>> getAllColorMappings() {
        return new HashMap<>(colorToDimension);
    }

    /**
     * Checks if a color is already registered.
     *
     * @param hexColor The hex color
     * @return true if registered
     */
    public static boolean isColorRegistered(int hexColor) {
        return colorToDimension.containsKey(hexColor);
    }

    /**
     * Locks the API - no more registrations allowed.
     * Called by BC:Core after initialization.
     */
    public static void lock() {
        locked = true;
    }
}