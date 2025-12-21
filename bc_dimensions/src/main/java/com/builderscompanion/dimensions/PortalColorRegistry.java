/*
 * MIT License
 * Copyright (c) 2025 Builders Companion
 */
package com.builderscompanion.dimensions;

import com.builderscompanion.core.api.BCPortalAPI;
import com.builderscompanion.core.config.DimensionConfig;
import com.builderscompanion.core.util.BCLogger;
import com.builderscompanion.core.util.ColorUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registers portal colors from dimension configs with BC:Core API.
 */
public class PortalColorRegistry {

    /**
     * Registers all dimension portal colors with BCPortalAPI.
     *
     * @param dimensions List of dimension configs to register
     */
    public static void registerPortalColors(List<DimensionConfig> dimensions) {
        Map<Integer, String> colorConflicts = new HashMap<>();

        for (DimensionConfig config : dimensions) {
            // Get effective color from config
            int color = ColorUtil.getEffectiveColor(config.portal.color);

            if (color == -1) {
                BCLogger.error("Invalid portal color for dimension {}, skipping registration", config.id);
                continue;
            }

            // Check for conflicts
            if (colorConflicts.containsKey(color)) {
                BCLogger.warn("⚠ Portal color conflict: {} and {} both use hex #{}",
                        config.id, colorConflicts.get(color), String.format("%06X", color));
                BCLogger.warn("→ First registered dimension will receive portals of this color");
            } else {
                colorConflicts.put(color, config.id);
            }

            // Create dimension key
            ResourceKey<Level> dimensionKey = ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION,
                    new ResourceLocation(BCDimensions.MODID, config.id)
            );

            // Register with API
            try {
                BCPortalAPI.registerColorMapping(color, dimensionKey);
                BCLogger.info("Registered portal color #{} for dimension {}",
                        String.format("%06X", color), config.id);
            } catch (IllegalArgumentException e) {
                BCLogger.error("Failed to register portal color for {}: {}", config.id, e.getMessage());
            }
        }
    }
}
