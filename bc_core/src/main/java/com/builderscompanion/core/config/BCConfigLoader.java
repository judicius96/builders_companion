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
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multi-file YAML configuration loader for Builders Companion.
 *
 * <p>This loader handles:
 * <ul>
 *   <li>Loading {@code dimensions.yml} to get enabled dimension list</li>
 *   <li>Loading {@code global.yml} for default settings</li>
 *   <li>Loading individual dimension configs from {@code dimensions/} directory</li>
 *   <li>Validating all configs with helpful error messages</li>
 *   <li>Hot reload via {@link #reloadAll()}</li>
 * </ul>
 *
 * <p>Configuration files are located at:
 * <pre>
 * config/builderscompanion/
 * ├─ dimensions.yml          # Main config
 * ├─ global.yml              # Global defaults
 * └─ dimensions/             # Individual dimension configs
 *    ├─ avalon.yml
 *    ├─ terra.yml
 *    └─ ...
 * </pre>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Load global config
 * GlobalConfig global = BCConfigLoader.loadGlobalConfig();
 *
 * // Get enabled dimensions
 * List<String> enabled = BCConfigLoader.getEnabledDimensions();
 *
 * // Load specific dimension
 * DimensionConfig config = BCConfigLoader.loadDimensionConfig("avalon");
 * if (config == null) {
 *     LOGGER.error("Failed to load avalon config");
 * }
 *
 * // Reload all configs
 * BCConfigLoader.reloadAll();
 * }</pre>
 *
 * @since 1.0.0
 */
public class BCConfigLoader {

    private static final Path CONFIG_DIR = Paths.get("config", "builderscompanion");
    private static final Path DIMENSIONS_FILE = CONFIG_DIR.resolve("dimensions.yml");
    private static final Path GLOBAL_FILE = CONFIG_DIR.resolve("global.yml");

    private static final LoaderOptions LOADER_OPTIONS = new LoaderOptions();
    private static final Yaml YAML = new Yaml(LOADER_OPTIONS);
    private static final ConfigValidator VALIDATOR = new ConfigValidator();

    // Cache for loaded configs
    private static GlobalConfig cachedGlobalConfig = null;
    private static final Map<String, DimensionConfig> cachedDimensionConfigs = new ConcurrentHashMap<>();
    private static List<String> cachedEnabledDimensions = null;

    /**
     * Loads the global configuration.
     *
     * <p>If the file doesn't exist, creates a default configuration and saves it.
     * The loaded config is cached for performance.
     *
     * @return the global configuration, or a default config if loading fails
     */
    public static GlobalConfig loadGlobalConfig() {
        // Return cached if available
        if (cachedGlobalConfig != null) {
            return cachedGlobalConfig;
        }

        BCLogger.debug("Loading global config from {}", GLOBAL_FILE);

        try {
            ensureConfigDirExists();

            File file = GLOBAL_FILE.toFile();
            if (!file.exists()) {
                BCLogger.warn("Global config not found, creating default at {}", GLOBAL_FILE);
                GlobalConfig defaultConfig = new GlobalConfig();
                saveGlobalConfig(defaultConfig);
                cachedGlobalConfig = defaultConfig;
                return defaultConfig;
            }

            try (InputStream input = new FileInputStream(file)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) YAML.load(input);
                GlobalConfig config = mapToGlobalConfig(data);
                cachedGlobalConfig = config;
                BCLogger.info("Loaded global config successfully");
                return config;
            }
        } catch (IOException e) {
            BCLogger.error("Failed to load global config: {}", e.getMessage());
            return new GlobalConfig();  // Return defaults on error
        } catch (YAMLException e) {
            BCLogger.error("YAML parse error in global.yml: {}", e.getMessage());
            return new GlobalConfig();
        }
    }

    /**
     * Gets all dimension configurations from the list-based format.
     *
     * <p>Reads from {@code dimensions.yml} which now contains inline dimension definitions.
     *
     * @return list of dimension configs, or empty list if file doesn't exist
     */
    public static List<DimensionConfig> getAllDimensions() {
        BCLogger.debug("Loading dimensions from {}", DIMENSIONS_FILE);

        try {
            ensureConfigDirExists();

            File file = DIMENSIONS_FILE.toFile();
            if (!file.exists()) {
                BCLogger.warn("dimensions.yml not found, creating default at {}", DIMENSIONS_FILE);
                generateDefaultDimensionsYml();
                return new ArrayList<>();
            }

            try (InputStream input = new FileInputStream(file)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = YAML.load(input);

                // Get the "dimensions" list
                Object dimensionsObj = data.get("dimensions");
                if (!(dimensionsObj instanceof List)) {
                    BCLogger.error("dimensions.yml must contain a 'dimensions' list");
                    return new ArrayList<>();
                }

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> dimensionsList = (List<Map<String, Object>>) dimensionsObj;

                List<DimensionConfig> configs = new ArrayList<>();

                for (Map<String, Object> dimData : dimensionsList) {
                    // Check for external file reference
                    if (dimData.containsKey("include")) {
                        String includePath = getString(dimData, "include", null);
                        if (includePath != null) {
                            DimensionConfig config = loadExternalDimensionConfig(includePath);
                            if (config != null) {
                                configs.add(config);
                            }
                        }
                        continue;  // Skip rest of processing for includes
                    }
                    // Check if enabled (default true)
                    boolean enabled = getBoolean(dimData, "enabled", true);
                    if (!enabled) {
                        String id = getString(dimData, "id", "unknown");
                        BCLogger.debug("Skipping disabled dimension: {}", id);
                        continue;
                    }

                    // Parse dimension config
                    DimensionConfig config = mapToDimensionConfig(dimData);

                    // Validate
                    ValidationResult result = VALIDATOR.validate(config);

                    if (!result.isValid()) {
                        BCLogger.error("Configuration validation failed for dimension '{}':", config.id);
                        for (String error : result.getErrors()) {
                            BCLogger.error("  - {}", error);
                        }
                        continue;
                    }

                    if (result.hasWarnings()) {
                        BCLogger.warn("Configuration warnings for dimension '{}':", config.id);
                        for (String warning : result.getWarnings()) {
                            BCLogger.warn("  - {}", warning);
                        }
                    }

                    configs.add(config);
                    BCLogger.info("Loaded dimension config: {} ({})", config.displayName, config.id);
                }

                BCLogger.info("Loaded {} enabled dimensions", configs.size());
                return configs;

            }
        } catch (IOException e) {
            BCLogger.error("Failed to load dimensions.yml: {}", e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            BCLogger.error("Error parsing dimensions.yml: {}", e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Loads a dimension configuration from an external YAML file.
     *
     * @param relativePath the path relative to config/builderscompanion (e.g., "dimensions/mining_world.yml")
     * @return the dimension config, or null if loading fails
     */
    private static DimensionConfig loadExternalDimensionConfig(String relativePath) {
        try {
            Path filePath = CONFIG_DIR.resolve(relativePath);

            if (!filePath.startsWith(CONFIG_DIR)) {
                BCLogger.error("Path traversal attempt blocked: {}", relativePath);
                return null;
            }

            File file = filePath.toFile();
            if (!file.exists()) {
                BCLogger.error("External dimension file not found: {}", filePath);
                return null;
            }

            try (InputStream input = new FileInputStream(file)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = YAML.load(input);

                DimensionConfig config = mapToDimensionConfig(data);

                // Validate
                ValidationResult result = VALIDATOR.validate(config);

                if (!result.isValid()) {
                    BCLogger.error("Configuration validation failed for external file '{}':", relativePath);
                    for (String error : result.getErrors()) {
                        BCLogger.error("  - {}", error);
                    }
                    return null;
                }

                if (result.hasWarnings()) {
                    BCLogger.warn("Configuration warnings for external file '{}':", relativePath);
                    for (String warning : result.getWarnings()) {
                        BCLogger.warn("  - {}", warning);
                    }
                }

                BCLogger.info("Loaded external dimension config: {} ({})", config.displayName, config.id);
                return config;
            }

        } catch (IOException e) {
            BCLogger.error("Failed to load external dimension file '{}': {}", relativePath, e.getMessage());
            return null;
        } catch (Exception e) {
            BCLogger.error("Error parsing external dimension file '{}': {}", relativePath, e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Reloads all configurations.
     *
     * <p>Clears all caches and reloads global config and all enabled dimensions.
     * This is called by the {@code /bcreload} command.
     *
     * @return the number of dimensions successfully reloaded
     */
    public static int reloadAll() {
        BCLogger.info("Reloading all Builders Companion configurations...");

        // Clear caches
        cachedGlobalConfig = null;
        cachedEnabledDimensions = null;
        cachedDimensionConfigs.clear();

        // Reload global config
        GlobalConfig global = loadGlobalConfig();
        BCLogger.setDebugMode(global.debugMode);

        // Reload all dimensions (new list-based format)
        List<DimensionConfig> dimensions = getAllDimensions();
        int successCount = dimensions.size();

        BCLogger.info("Reload complete: {} dimensions loaded successfully", successCount);
        return successCount;
    }

    /**
     * Validates a dimension ID to prevent path traversal attacks.
     *
     * @param dimensionId the dimension ID to validate
     * @return true if valid, false otherwise
     */
    private static boolean isValidDimensionId(String dimensionId) {
        // Only allow alphanumeric, underscore, and hyphen
        return dimensionId.matches("^[a-zA-Z0-9_-]+$");
    }

    /**
     * Ensures the config directory structure exists.
     */
    private static void ensureConfigDirExists() throws IOException {
        Files.createDirectories(CONFIG_DIR);
        Files.createDirectories(CONFIG_DIR.resolve("dimensions"));

        // Generate example files if they don't exist
        generateExampleMiningWorld();
    }

    /**
     * Saves the global configuration to file.
     *
     * @param config the global config to save
     */
    private static void saveGlobalConfig(GlobalConfig config) {
        // TODO: Implement YAML serialization
        // For now, this is a placeholder for creating default configs
        BCLogger.warn("Auto-generation of global.yml not yet implemented");
    }

    private static void generateDefaultDimensionsYml() {
        try {
            Files.createDirectories(DIMENSIONS_FILE.getParent());

            String defaultContent = """
            # ============================================================================
            # BUILDERS COMPANION - DIMENSIONS CONFIGURATION
            # ============================================================================
            #
            # This file defines all custom dimensions for your world.
            # Each dimension becomes: bc_dimensions:<id>
            #
            # HOW TO ADD A DIMENSION:
            # 1. Copy the example below
            # 2. Change 'id' to something unique (lowercase, no spaces)
            # 3. Set display_name (what players see)
            # 4. Configure portal block and biomes
            # 5. Save and restart the game
            #
            # ============================================================================
            
            dimensions:
              # ========================================================================
              # EXAMPLE 1: Simple Inline Dimension (Sand World)
              # ========================================================================
              - id: sand_world
                enabled: true
                display_name: "Endless Dunes"
            
                portal:
                  frame_block: "minecraft:sandstone"
                  ignition_item: "minecraft:flint_and_steel"
            
                world_border:
                  radius: 2000  # chunks
            
                biomes:
                  mode: "region"
                  include_vanilla: false
                  include_biomes:
                    - "minecraft:desert"
                    - "minecraft:badlands"
                    - "minecraft:eroded_badlands"
                    - "minecraft:wooded_badlands"
            
              # ========================================================================
              # EXAMPLE 2: Complex Dimension from External File
              # ========================================================================
              # For complex configs (layered mode, many biomes), use external files
              #
              # Uncomment the line below to enable the mining dimension example:
              
              # - include: "dimensions/mining_world.yml"
              
              # The file demonstrates layered mode with The Hollow cavern layer
            
            # ============================================================================
            # More examples (commented out - remove # to enable):
            # ============================================================================
            
              # Example: Magic dimension (combine multiple mods)
              # - id: avalon
              #   enabled: false
              #   display_name: "Avalon - Realm of Magic"
              #   portal:
              #     frame_block: "minecraft:polished_diorite"
              #   biomes:
              #     mode: "region"
              #     include_vanilla: true
              #     include_mods:
              #       - "biomesoplenty:*"
              #       - "hexerei:*"
            
            # ============================================================================
            # CONFIGURATION FIELDS EXPLAINED:
            # ============================================================================
            #
            # id: Unique identifier (lowercase, underscores okay)
            # enabled: true/false - whether to load this dimension
            # display_name: What players see in chat/UI
            #
            # portal:
            #   frame_block: Block used to build portal frame
            #   ignition_item: Item used to activate portal (usually flint_and_steel)
            #
            # biomes.mode: "region" (Terrablender), "climate_grid" (geographic), or "layered" (vertical)
            #
            # For mode="region":
            #   include_vanilla: true/false - include vanilla Minecraft biomes
            #   vanilla_dimension: "overworld", "nether", or "end"
            #   include_mods: List of mod wildcards (e.g., "biomesoplenty:*")
            #   include_biomes: List of specific biome IDs (e.g., "minecraft:desert")
            #   source_dimension: Copy biomes from "minecraft:overworld", "minecraft:the_nether", or "minecraft:the_end"
            #
            # You must provide at least ONE of: include_vanilla, include_mods, include_biomes, or source_dimension
            #
            # ============================================================================
            """;

            Files.writeString(DIMENSIONS_FILE, defaultContent);
            BCLogger.info("Created default dimensions.yml with examples and instructions");

        } catch (IOException e) {
            BCLogger.error("Failed to create default dimensions.yml: {}", e.getMessage());
        }
    }

    /**
     * Maps a YAML data map to a GlobalConfig object.
     *
     * @param data the YAML data map
     * @return the GlobalConfig object
     */
    private static GlobalConfig mapToGlobalConfig(Map<String, Object> data) {
        GlobalConfig config = new GlobalConfig();

        // Performance & Safety
        config.maxDimensions = getInt(data, "max_dimensions", 10);
        config.chunkLoadRadius = getInt(data, "chunk_load_radius", 8);

        // Default World Border
        Map<String, Object> borderData = getMap(data, "default_world_border");
        if (borderData != null) {
            config.defaultWorldBorder.centerX = getInt(borderData, "center_x", 0);
            config.defaultWorldBorder.centerZ = getInt(borderData, "center_z", 0);
            config.defaultWorldBorder.radius = getInt(borderData, "radius", 10000);
            config.defaultWorldBorder.damagePerBlock = getDouble(borderData, "damage_per_block", 0.2);
            config.defaultWorldBorder.warningDistance = getInt(borderData, "warning_distance", 100);
            config.defaultWorldBorder.warningTime = getInt(borderData, "warning_time", 15);
        }

        // Portal Defaults
        config.defaultPortalOffset = getInt(data, "default_portal_offset", 1);

        // Cave System Defaults
        config.defaultCarver = getString(data, "default_carver", "minecraft:default");

        // Debugging
        config.debugMode = getBoolean(data, "debug_mode", false);
        config.logDimensionCreation = getBoolean(data, "log_dimension_creation", true);
        config.logBiomePlacement = getBoolean(data, "log_biome_placement", false);
        config.logRegionCompetition = getBoolean(data, "log_region_competition", false);

        // Auto-Generation Settings
        config.generateRegionReports = getBoolean(data, "generate_region_reports", true);
        config.generateBiomeWeightReports = getBoolean(data, "generate_biome_weight_reports", true);
        config.generateDistributionReports = getBoolean(data, "generate_distribution_reports", true);

        return config;
    }

    /**
     * Maps a YAML data map to a DimensionConfig object.
     *
     * <p>This is a complex method that recursively maps nested structures.
     *
     * @param data the YAML data map
     * @return the DimensionConfig object
     */
    private static DimensionConfig mapToDimensionConfig(Map<String, Object> data) {
        DimensionConfig config = new DimensionConfig();

        config.id = getString(data, "id", "unnamed");
        config.displayName = getString(data, "display_name", "Unnamed Dimension");

        // Portal
        Map<String, Object> portalData = getMap(data, "portal");
        if (portalData != null) {
            config.portal.frameBlock = getString(portalData, "frame_block", "minecraft:obsidian");
            config.portal.ignitionItem = getString(portalData, "ignition_item", "minecraft:flint_and_steel");
            config.portal.portalOffset = getInt(portalData, "portal_offset", 1);
            config.portal.linkMode = getString(portalData, "link_mode", "coordinate");
            config.portal.ambientSound = getString(portalData, "ambient_sound", "minecraft:block.portal.ambient");
            config.portal.travelSound = getString(portalData, "travel_sound", "minecraft:block.portal.travel");
        }

        // World Border
        Map<String, Object> borderData = getMap(data, "world_border");
        if (borderData != null) {
            config.worldBorder.centerX = getInt(borderData, "center_x", 0);
            config.worldBorder.centerZ = getInt(borderData, "center_z", 0);
            config.worldBorder.radius = getInt(borderData, "radius", 10000);
            config.worldBorder.damagePerBlock = getDouble(borderData, "damage_per_block", 0.2);
            config.worldBorder.warningDistance = getInt(borderData, "warning_distance", 100);
            config.worldBorder.warningTime = getInt(borderData, "warning_time", 15);
        }

        // Biomes (complex nested structure)
        Map<String, Object> biomesData = getMap(data, "biomes");
        if (biomesData != null) {
            mapBiomeConfig(biomesData, config.biomes);
        }

        // World Generation (complex nested structure)
        Map<String, Object> worldGenData = getMap(data, "world_generation");
        if (worldGenData != null) {
            mapWorldGenerationConfig(worldGenData, config.worldGeneration);
        }

        // Structures
        Map<String, Object> structData = getMap(data, "structures");
        if (structData != null) {
            config.structures.sourceDimension = getString(structData, "source_dimension", null);
            config.structures.limitToParentMods = getBoolean(structData, "limit_to_parent_mods", true);
            config.structures.includeStructures = getStringList(structData, "include_structures");
            config.structures.excludeStructures = getStringList(structData, "exclude_structures");
        }

        // Placed Features
        Map<String, Object> featuresData = getMap(data, "placed_features");
        if (featuresData != null) {
            config.placedFeatures.limitToParentMods = getBoolean(featuresData, "limit_to_parent_mods", true);

            Map<String, Object> densityData = getMap(featuresData, "density");
            if (densityData != null) {
                config.placedFeatures.density.trees = getDouble(densityData, "trees", 1.0);
                config.placedFeatures.density.flowers = getDouble(densityData, "flowers", 1.0);
                config.placedFeatures.density.grass = getDouble(densityData, "grass", 1.0);
                config.placedFeatures.density.ores = getDouble(densityData, "ores", 1.0);
                config.placedFeatures.density.mushrooms = getDouble(densityData, "mushrooms", 1.0);
                config.placedFeatures.density.structures = getDouble(densityData, "structures", 1.0);
            }
        }

        // Mob Spawning
        Map<String, Object> mobData = getMap(data, "mob_spawning");
        if (mobData != null) {
            config.mobSpawning.inheritFromBiomes = getBoolean(mobData, "inherit_from_biomes", true);
            // Map spawn rates if present
            Object ratesObj = mobData.get("spawn_rates");
            if (ratesObj instanceof Map) {
                ((Map<String, Object>) ratesObj).forEach((key, value) -> {
                    if (value instanceof Number) {
                        config.mobSpawning.spawnRates.put(key, ((Number) value).doubleValue());
                    }
                });
            }
        }

        return config;
    }

    /**
     * Maps biome configuration data.
     */
    private static void mapBiomeConfig(Map<String, Object> data, DimensionConfig.BiomeConfig config) {
        config.mode = getString(data, "mode", "region");

        // Region mode
        config.sourceDimension = getString(data, "source_dimension", null);
        config.includeVanilla = getBoolean(data, "include_vanilla", false);
        config.vanillaDimension = getString(data, "vanilla_dimension", "overworld");
        config.includeMods = getStringList(data, "include_mods");
        config.includeBiomes = getStringList(data, "include_biomes");
        config.excludeBiomes = getStringList(data, "exclude_biomes");
        config.excludeRegions = getStringList(data, "exclude_regions");
        config.generateRegionReport = getBoolean(data, "generate_region_report", true);
        config.generateBiomeWeights = getBoolean(data, "generate_biome_weights", true);

        // Map region overrides
        Object regionOverridesObj = data.get("region_overrides");
        if (regionOverridesObj instanceof Map) {
            ((Map<String, Object>) regionOverridesObj).forEach((key, value) -> {
                if (value instanceof Number) {
                    config.regionOverrides.put(key, ((Number) value).intValue());
                }
            });
        }

        // Map weight overrides
        Object weightOverridesObj = data.get("weight_overrides");
        if (weightOverridesObj instanceof Map) {
            ((Map<String, Object>) weightOverridesObj).forEach((key, value) -> {
                if (value instanceof Number) {
                    config.weightOverrides.put(key, ((Number) value).intValue());
                }
            });
        }

        // Climate grid mode
        Map<String, Object> climateData = getMap(data, "climate_grid");
        if (climateData != null) {
            mapClimateGridConfig(climateData, config.climateGrid);
        }

        // Layered mode
        List<Map<String, Object>> layersData = getMapList(data, "layers");
        if (layersData != null) {
            for (Map<String, Object> layerData : layersData) {
                DimensionConfig.BiomeLayer layer = new DimensionConfig.BiomeLayer();
                layer.name = getString(layerData, "name", "Unnamed Layer");
                layer.yMin = getInt(layerData, "y_min", 0);
                layer.yMax = getInt(layerData, "y_max", 64);
                layer.percentage = getInt(layerData, "percentage", 100);
                layer.biomes = getStringList(layerData, "biomes");
                layer.cavePercentage = getInt(layerData, "cave_percentage", 0);
                layer.caveBiome = getString(layerData, "cave_biome", null);
                layer.biomeMode = getString(layerData, "biome_mode", "normal");
                layer.biome = getString(layerData, "biome", null);
                layer.ceilingThickness = getInt(layerData, "ceiling_thickness", 3);
                layer.floorThickness = getInt(layerData, "floor_thickness", 1);
                config.layers.add(layer);
            }
        }
    }

    /**
     * Maps climate grid configuration data.
     */
    private static void mapClimateGridConfig(Map<String, Object> data, DimensionConfig.ClimateGridConfig config) {
        // Available biomes
        Map<String, Object> availBiomesData = getMap(data, "available_biomes");
        if (availBiomesData != null) {
            config.availableBiomes.includeVanilla = getBoolean(availBiomesData, "include_vanilla", true);
            config.availableBiomes.includeMods = getStringList(availBiomesData, "include_mods");
            config.availableBiomes.excludeBiomes = getStringList(availBiomesData, "exclude_biomes");
        }

        // Spawn location
        List<Integer> spawnLoc = getIntList(data, "spawn_location");
        if (spawnLoc != null && spawnLoc.size() >= 2) {
            config.spawnLocation = new int[]{spawnLoc.get(0), spawnLoc.get(1)};
        }

        config.boundaryChunks = getInt(data, "boundary_chunks", 2500);

        // Temperature
        Map<String, Object> tempData = getMap(data, "temperature");
        if (tempData != null) {
            config.temperature.north = getDouble(tempData, "north", -1.0);
            config.temperature.south = getDouble(tempData, "south", 1.0);
            config.temperature.spawn = getDouble(tempData, "spawn", 0.0);
        }

        // Moisture
        Map<String, Object> moistData = getMap(data, "moisture");
        if (moistData != null) {
            config.moisture.west = getDouble(moistData, "west", 1.0);
            config.moisture.east = getDouble(moistData, "east", -1.0);
            config.moisture.spawn = getDouble(moistData, "spawn", 0.0);
        }

        config.reversal = getBoolean(data, "reversal", true);

        // Blob control
        config.minBiomeSizeChunks = getInt(data, "min_biome_size_chunks", 64);
        config.blobIrregularity = getDouble(data, "blob_irregularity", 0.7);
        config.blobNoiseScale = getDouble(data, "blob_noise_scale", 0.08);
        config.blobCoherence = getDouble(data, "blob_coherence", 0.6);
        config.transitionWidthChunks = getInt(data, "transition_width_chunks", 4);
        config.climateTolerance = getDouble(data, "climate_tolerance", 0.2);
    }

    /**
     * Maps world generation configuration data.
     */
    private static void mapWorldGenerationConfig(Map<String, Object> data, DimensionConfig.WorldGenerationConfig config) {
        config.dimensionType = getString(data, "dimension_type", "overworld");

        // Seed
        Map<String, Object> seedData = getMap(data, "seed");
        if (seedData != null) {
            config.seed.mirrorOverworld = getBoolean(seedData, "mirror_overworld", false);
            config.seed.mirrorDimension = getString(seedData, "mirror_dimension", null);
            Object customSeedObj = seedData.get("custom_seed");
            if (customSeedObj instanceof Number) {
                config.seed.customSeed = ((Number) customSeedObj).longValue();
            }
        }

        // Dimension-specific settings
        config.ceilingBedrock = getBoolean(data, "ceiling_bedrock", false);
        config.lavaLevel = getInt(data, "lava_level", 32);
        config.ambientLight = getDouble(data, "ambient_light", 1.0);
        config.voidLevel = getInt(data, "void_level", 0);
        config.islandGeneration = getBoolean(data, "island_generation", false);
        config.centralIsland = getBoolean(data, "central_island", false);

        // TODO: Add noise, carver, aquifers, and ore generation mapping
    }

    // ===== Helper methods for extracting data from YAML maps =====

    private static String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private static int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).intValue() : defaultValue;
    }

    private static double getDouble(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : defaultValue;
    }

    private static boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    @SuppressWarnings("unchecked")
    private static List<String> getStringList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                result.add(item.toString());
            }
            return result;
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private static List<Integer> getIntList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            List<Integer> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                if (item instanceof Number) {
                    result.add(((Number) item).intValue());
                }
            }
            return result;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> getMapList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                if (item instanceof Map) {
                    result.add((Map<String, Object>) item);
                }
            }
            return result;
        }
        return null;
    }

    /**
     * Generates the example mining_world.yml file if it doesn't exist.
     */
    private static void generateExampleMiningWorld() {
        try {
            Path dimensionsDir = CONFIG_DIR.resolve("dimensions");
            Files.createDirectories(dimensionsDir);

            Path miningWorldFile = dimensionsDir.resolve("mining_world.yml");

            if (miningWorldFile.toFile().exists()) {
                BCLogger.debug("mining_world.yml already exists, skipping generation");
                return;
            }

            String miningWorldContent = """
            # ============================================================================
            # MINING WORLD - LAYERED DIMENSION EXAMPLE
            # ============================================================================
            # This demonstrates layered mode with The Hollow
            #
            # To enable: In dimensions.yml, uncomment:
            #   - include: "dimensions/mining_world.yml"
            # ============================================================================
            
            id: mining_world
            enabled: false  # Change to true to enable
            display_name: "The Mines"
            
            portal:
              frame_block: "minecraft:crying_obsidian"
              ignition_item: "minecraft:flint_and_steel"
              portal_offset: 1
            
            world_border:
              radius: 3000
            
            biomes:
              mode: "layered"
              
              layers:
                - name: "Surface"
                  y_min: 64
                  y_max: 320
                  percentage: 100
                  biomes:
                    - "minecraft:plains"
                  cave_percentage: 0
                
                - name: "Upper Mines"
                  y_min: 0
                  y_max: 64
                  percentage: 70
                  biomes:
                    - "minecraft:stone"
                  cave_percentage: 30
                  cave_biome: "minecraft:dripstone_caves"
                
                - name: "The Hollow"
                  y_min: -64
                  y_max: -32
                  percentage: 0
                  biome_mode: "single"
                  biome: "minecraft:lush_caves"
                  ceiling_thickness: 3
                  floor_thickness: 1
            
            world_generation:
              dimension_type: "overworld"
            """;

            Files.writeString(miningWorldFile, miningWorldContent);
            BCLogger.info("Created example mining_world.yml in dimensions/ folder");

        } catch (IOException e) {
            BCLogger.error("Failed to create mining_world.yml example: {}", e.getMessage());
        }
    }
}