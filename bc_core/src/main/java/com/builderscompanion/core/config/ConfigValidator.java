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
package com.builderscompanion.core.config;

import com.builderscompanion.core.util.BCLogger;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * Validates dimension configurations.
 *
 * <p>This validator checks for:
 * <ul>
 *   <li>Invalid block IDs</li>
 *   <li>Invalid biome mode settings</li>
 *   <li>Layered mode conflicts (gaps, overlaps)</li>
 *   <li>Invalid parameter values</li>
 *   <li>Missing required fields</li>
 * </ul>
 *
 * <p>Validation results include both errors (must fix) and warnings (should review).
 *
 * <p>Example usage:
 * <pre>{@code
 * ConfigValidator validator = new ConfigValidator();
 * ValidationResult result = validator.validate(config);
 *
 * if (!result.isValid()) {
 *     for (String error : result.getErrors()) {
 *         LOGGER.error("Config error: {}", error);
 *     }
 *     return null;
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public class ConfigValidator {

    /**
     * Validates a dimension configuration.
     *
     * @param config the dimension configuration to validate
     * @return validation result with errors and warnings
     */
    public ValidationResult validate(DimensionConfig config) {
        ValidationResult result = new ValidationResult();

        if (config == null) {
            result.addError("Configuration is null");
            return result;
        }

        // Validate basic fields
        validateDisplayName(config, result);
        validatePortal(config, result);
        validateWorldBorder(config, result);
        validateBiomes(config, result);
        validateWorldGeneration(config, result);

        return result;
    }

    /**
     * Validates the display name.
     */
    private void validateDisplayName(DimensionConfig config, ValidationResult result) {
        if (config.displayName == null || config.displayName.trim().isEmpty()) {
            result.addError("display_name is required and cannot be empty");
        }
    }

    /**
     * Validates portal configuration.
     */
    private void validatePortal(DimensionConfig config, ValidationResult result) {
        if (config.portal == null) {
            result.addError("portal configuration is required");
            return;
        }

        DimensionConfig.PortalConfig portal = config.portal;

        // Validate frame block
        if (portal.frameBlock == null || portal.frameBlock.trim().isEmpty()) {
            result.addError("portal.frame_block is required");
        } else if (!isValidResourceLocation(portal.frameBlock)) {
            result.addError("portal.frame_block '" + portal.frameBlock + "' is not a valid resource location");
        }

        // Validate ignition item
        if (portal.ignitionItem == null || portal.ignitionItem.trim().isEmpty()) {
            result.addWarning("portal.ignition_item not set, defaulting to minecraft:flint_and_steel");
        } else if (!isValidResourceLocation(portal.ignitionItem)) {
            result.addError("portal.ignition_item '" + portal.ignitionItem + "' is not a valid resource location");
        }

        // Validate portal offset
        if (portal.portalOffset == 0) {
            result.addError("portal.portal_offset cannot be 0 (would cause division by zero)");
        }

        // Validate link mode
        if (portal.linkMode == null) {
            result.addWarning("portal.link_mode not set, defaulting to 'coordinate'");
        } else if (!portal.linkMode.equals("coordinate") && !portal.linkMode.equals("spawn")) {
            result.addError("portal.link_mode must be 'coordinate' or 'spawn', got: " + portal.linkMode);
        }
    }

    /**
     * Validates world border configuration.
     */
    private void validateWorldBorder(DimensionConfig config, ValidationResult result) {
        if (config.worldBorder == null) {
            result.addWarning("world_border not configured, using defaults");
            return;
        }

        DimensionConfig.WorldBorderConfig border = config.worldBorder;

        if (border.radius <= 0) {
            result.addError("world_border.radius must be positive, got: " + border.radius);
        }

        if (border.damagePerBlock < 0) {
            result.addError("world_border.damage_per_block cannot be negative, got: " + border.damagePerBlock);
        }

        if (border.warningDistance < 0) {
            result.addError("world_border.warning_distance cannot be negative, got: " + border.warningDistance);
        }

        if (border.warningTime < 0) {
            result.addError("world_border.warning_time cannot be negative, got: " + border.warningTime);
        }
    }

    /**
     * Validates biome configuration.
     */
    private void validateBiomes(DimensionConfig config, ValidationResult result) {
        if (config.biomes == null) {
            result.addError("biomes configuration is required");
            return;
        }

        DimensionConfig.BiomeConfig biomes = config.biomes;

        // Validate mode
        if (biomes.mode == null) {
            result.addError("biomes.mode is required");
            return;
        }

        switch (biomes.mode.toLowerCase()) {
            case "region":
                validateRegionMode(biomes, result);
                break;
            case "climate_grid":
                validateClimateGridMode(biomes, result);
                break;
            case "layered":
                validateLayeredMode(biomes, result);
                break;
            default:
                result.addError("biomes.mode must be 'region', 'climate_grid', or 'layered', got: " + biomes.mode);
                break;
        }
    }

    /**
     * Validates region mode settings.
     */
    private void validateRegionMode(DimensionConfig.BiomeConfig biomes, ValidationResult result) {
        // Check if any biomes are configured
        if (!biomes.includeVanilla &&
                biomes.includeMods.isEmpty() &&
                biomes.includeBiomes.isEmpty() &&     // ← ADD THIS LINE
                biomes.sourceDimension == null) {

            result.addError("region mode: must set include_vanilla, include_mods, include_biomes, or source_dimension");
        }

        // Warn about climate_grid settings in region mode
        if (biomes.climateGrid.minBiomeSizeChunks != 64) {
            result.addWarning("climate_grid settings are set but mode='region', these will be ignored");
        }

        // Warn about layered settings in region mode
        if (!biomes.layers.isEmpty()) {
            result.addWarning("layers are set but mode='region', these will be ignored");
        }
    }

    /**
     * Validates climate grid mode settings.
     */
    private void validateClimateGridMode(DimensionConfig.BiomeConfig biomes, ValidationResult result) {
        DimensionConfig.ClimateGridConfig climate = biomes.climateGrid;

        if (climate.boundaryChunks <= 0) {
            result.addError("climate_grid.boundary_chunks must be positive, got: " + climate.boundaryChunks);
        }

        if (climate.minBiomeSizeChunks <= 0) {
            result.addError("climate_grid.min_biome_size_chunks must be positive, got: " + climate.minBiomeSizeChunks);
        }

        if (climate.blobIrregularity < 0.0 || climate.blobIrregularity > 1.0) {
            result.addError("climate_grid.blob_irregularity must be between 0.0 and 1.0, got: " + climate.blobIrregularity);
        }

        if (climate.blobCoherence < 0.0 || climate.blobCoherence > 1.0) {
            result.addError("climate_grid.blob_coherence must be between 0.0 and 1.0, got: " + climate.blobCoherence);
        }

        if (climate.climateTolerance <= 0.0) {
            result.addError("climate_grid.climate_tolerance must be positive, got: " + climate.climateTolerance);
        }

        // Check if any biomes are configured
        if (!climate.availableBiomes.includeVanilla && climate.availableBiomes.includeMods.isEmpty()) {
            result.addError("climate_grid: must set available_biomes.include_vanilla or include_mods");
        }

        // Warn about region settings in climate mode
        if (!biomes.regionOverrides.isEmpty()) {
            result.addWarning("region_overrides are set but mode='climate_grid', these will be ignored");
        }
    }

    /**
     * Validates layered mode settings.
     */
    private void validateLayeredMode(DimensionConfig.BiomeConfig biomes, ValidationResult result) {
        if (biomes.layers.isEmpty()) {
            result.addError("layered mode: layers list is required and cannot be empty");
            return;
        }

        // Sort layers by yMin to check for gaps and overlaps
        List<DimensionConfig.BiomeLayer> sortedLayers = new ArrayList<>(biomes.layers);
        sortedLayers.sort(Comparator.comparingInt(l -> l.yMin));

        for (int i = 0; i < sortedLayers.size(); i++) {
            DimensionConfig.BiomeLayer layer = sortedLayers.get(i);

            // Validate basic layer properties
            if (layer.name == null || layer.name.trim().isEmpty()) {
                result.addWarning("Layer " + i + " has no name");
            }

            if (layer.yMin >= layer.yMax) {
                result.addError("Layer '" + layer.name + "': y_min (" + layer.yMin +
                        ") must be less than y_max (" + layer.yMax + ")");
            }

            if (layer.percentage < 0 || layer.percentage > 100) {
                result.addError("Layer '" + layer.name + "': percentage must be 0-100, got: " + layer.percentage);
            }

            // Validate hollow layers
            if (layer.percentage == 0) {
                if (!layer.biomeMode.equals("single")) {
                    result.addError("Layer '" + layer.name + "': percentage=0 requires biome_mode='single'");
                }
                if (layer.biome == null) {
                    result.addError("Layer '" + layer.name + "': percentage=0 requires 'biome' field");
                }

                int layerHeight = layer.yMax - layer.yMin;
                int totalThickness = layer.ceilingThickness + layer.floorThickness;
                if (totalThickness >= layerHeight) {
                    result.addError("Layer '" + layer.name + "': ceiling + floor thickness (" + totalThickness +
                            ") >= layer height (" + layerHeight + ")");
                }
            } else {
                // Non-hollow layer should have biomes
                if (layer.biomes.isEmpty() && !layer.biomeMode.equals("single")) {
                    result.addError("Layer '" + layer.name + "': must specify biomes or set biome_mode='single'");
                }
            }

            // Check for overlaps with next layer
            if (i < sortedLayers.size() - 1) {
                DimensionConfig.BiomeLayer nextLayer = sortedLayers.get(i + 1);

                if (layer.yMax > nextLayer.yMin) {
                    result.addError("Layer '" + layer.name + "' (Y " + layer.yMin + " to " + layer.yMax +
                            ") overlaps with '" + nextLayer.name + "' (Y " + nextLayer.yMin + " to " + nextLayer.yMax + ")");
                } else if (layer.yMax < nextLayer.yMin - 1) {
                    result.addWarning("Gap between layer '" + layer.name + "' (Y " + layer.yMax +
                            ") and '" + nextLayer.name + "' (Y " + nextLayer.yMin + ")");
                }
            }
        }
    }

    /**
     * Validates world generation configuration.
     */
    private void validateWorldGeneration(DimensionConfig config, ValidationResult result) {
        if (config.worldGeneration == null) {
            result.addWarning("world_generation not configured, using defaults");
            return;
        }

        DimensionConfig.WorldGenerationConfig worldGen = config.worldGeneration;

        // Validate dimension type
        if (worldGen.dimensionType != null) {
            String type = worldGen.dimensionType.toLowerCase();
            if (!type.equals("overworld") && !type.equals("nether") && !type.equals("end") && !type.equals("custom")) {
                result.addError("world_generation.dimension_type must be 'overworld', 'nether', 'end', or 'custom', got: " + worldGen.dimensionType);
            }
        }

        // Validate ambient light
        if (worldGen.ambientLight < 0.0 || worldGen.ambientLight > 1.0) {
            result.addError("world_generation.ambient_light must be between 0.0 and 1.0, got: " + worldGen.ambientLight);
        }

        // Validate noise preset
        if (worldGen.noise.preset != null && worldGen.noise.preset.equals("custom") &&
                worldGen.noise.customSettings.terrain.verticalScale <= 0) {
            result.addError("world_generation.noise.custom_settings.terrain.vertical_scale must be positive");
        }
    }

    /**
     * Checks if a string is a valid Minecraft resource location.
     *
     * @param location the resource location string (e.g., "minecraft:stone")
     * @return true if valid, false otherwise
     */
    private boolean isValidResourceLocation(String location) {
        try {
            new ResourceLocation(location);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
