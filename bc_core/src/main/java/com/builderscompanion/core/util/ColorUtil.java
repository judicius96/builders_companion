/*
 * MIT License
 * Copyright (c) 2025 Builders Companion
 */
package com.builderscompanion.core.util;

import com.builderscompanion.core.config.DimensionConfig;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Utilities for parsing portal colors from config.
 */
public class ColorUtil {

    /**
     * Gets the effective hex color for a portal from config.
     * Uses custom hex if provided, otherwise natural dye color.
     *
     * @param config The portal color config
     * @return Hex color as int, or -1 if invalid
     */
    public static int getEffectiveColor(DimensionConfig.PortalColorConfig config) {
        // Use custom hex if provided
        if (config.customHex != null && !config.customHex.isEmpty()) {
            return parseHex(config.customHex);
        }

        // Otherwise get natural dye color
        return getNaturalDyeColor(config.dyeItem);
    }

    /**
     * Parses a hex color string to int.
     *
     * @param hex Hex string (e.g., "#3C44AA" or "3C44AA")
     * @return Color as int, or -1 if invalid
     */
    public static int parseHex(String hex) {
        try {
            String clean = hex.startsWith("#") ? hex.substring(1) : hex;
            return Integer.parseInt(clean, 16);
        } catch (NumberFormatException e) {
            BCLogger.error("Invalid hex color: {}", hex);
            return -1;
        }
    }

    /**
     * Gets the natural color of a dye item.
     *
     * @param dyeItemId The dye item ID (e.g., "minecraft:blue_dye")
     * @return Color as int, or -1 if not found or not a dye
     */
    public static int getNaturalDyeColor(String dyeItemId) {
        try {
            ResourceLocation id = new ResourceLocation(dyeItemId);
            Item item = ForgeRegistries.ITEMS.getValue(id);

            if (item instanceof DyeItem dyeItem) {
                return dyeItem.getDyeColor().getTextColor();
            } else {
                BCLogger.error("Item {} is not a dye", dyeItemId);
                return -1;
            }
        } catch (Exception e) {
            BCLogger.error("Failed to get dye color for {}: {}", dyeItemId, e.getMessage());
            return -1;
        }
    }
}
