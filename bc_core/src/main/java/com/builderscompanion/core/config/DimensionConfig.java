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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Complete configuration for a custom dimension.
 *
 * <p>Loaded from {@code config/builderscompanion/dimensions/<dimension_id>.yml}.
 *
 * <p>This class mirrors the YAML structure exactly, with sensible defaults for all fields.
 * All nested configuration classes are public static inner classes for easy access.
 *
 * <p>Example usage:
 * <pre>{@code
 * DimensionConfig config = BCConfigLoader.loadDimensionConfig("avalon");
 * String frameBlock = config.portal.frameBlock;  // "minecraft:obsidian"
 * String mode = config.biomes.mode;  // "region"
 * }</pre>
 *
 * @since 1.0.0
 */
public class DimensionConfig {

    public String id;
    public String displayName = "Unnamed Dimension";
    public PortalConfig portal = new PortalConfig();
    public WorldBorderConfig worldBorder = new WorldBorderConfig();
    public BiomeConfig biomes = new BiomeConfig();
    public WorldGenerationConfig worldGeneration = new WorldGenerationConfig();
    public StructureConfig structures = new StructureConfig();
    public PlacedFeatureConfig placedFeatures = new PlacedFeatureConfig();
    public MobSpawningConfig mobSpawning = new MobSpawningConfig();

    /**
     * Portal configuration.
     *
     * <p>Defines how portals to this dimension work, including frame block,
     * ignition method, coordinate scaling, and visual/audio effects.
     */
    public static class PortalConfig {
        public String frameBlock = "minecraft:obsidian";
        public String ignitionItem = "minecraft:flint_and_steel";
        public int portalOffset = 1;
        public String linkMode = "coordinate";
        public String ambientSound = "minecraft:block.portal.ambient";
        public String travelSound = "minecraft:block.portal.travel";
        public PortalColorConfig color = new PortalColorConfig();
    }

    public static class PortalColorConfig {
        public String dyeItem = "minecraft:purple_dye";
        public String customHex = null;  // Optional hex override
    }

    /**
     * World border configuration.
     *
     * <p>Defines the world border size, damage, and warning behavior.
     */
    public static class WorldBorderConfig {
        public int centerX = 0;
        public int centerZ = 0;
        public int radius = 10000;  // Chunks
        public double damagePerBlock = 0.2;
        public int warningDistance = 100;  // Blocks
        public int warningTime = 15;  // Seconds
    }

    /**
     * Biome configuration.
     *
     * <p>Supports three modes:
     * <ul>
     *   <li><b>region</b> - Use Terrablender regions for biome distribution</li>
     *   <li><b>climate_grid</b> - Geographic climate-based distribution</li>
     *   <li><b>layered</b> - Vertical biome layers (for mining dimensions)</li>
     * </ul>
     */
    public static class BiomeConfig {
        public String mode = "region";  // "region", "climate_grid", or "layered"

        // ===== REGION MODE =====
        public String sourceDimension = null;
        public boolean includeVanilla = false;
        public String vanillaDimension = "overworld";  // "overworld", "nether", or "end"
        public List<String> includeMods = new ArrayList<>();
        public List<String> excludeBiomes = new ArrayList<>();
        public Map<String, Integer> regionOverrides = new HashMap<>();
        public List<String> includeBiomes = new ArrayList<>();
        public List<String> excludeRegions = new ArrayList<>();
        public Map<String, Integer> weightOverrides = new HashMap<>();
        public boolean generateRegionReport = true;
        public boolean generateBiomeWeights = true;

        // ===== CLIMATE GRID MODE =====
        public ClimateGridConfig climateGrid = new ClimateGridConfig();

        // ===== LAYERED MODE =====
        public List<BiomeLayer> layers = new ArrayList<>();
    }

    /**
     * Climate grid configuration.
     *
     * <p>Used when {@code biomes.mode = "climate_grid"}.
     *
     * <p>Defines geographic climate gradients (temperature north-south, moisture east-west)
     * with organic blob shapes enforcing minimum biome sizes.
     */
    public static class ClimateGridConfig {
        public AvailableBiomesConfig availableBiomes = new AvailableBiomesConfig();
        public int[] spawnLocation = {0, 0};  // [X, Z]
        public int boundaryChunks = 2500;

        public TemperatureConfig temperature = new TemperatureConfig();
        public MoistureConfig moisture = new MoistureConfig();
        public boolean reversal = true;

        // Blob control
        public int minBiomeSizeChunks = 64;
        public double blobIrregularity = 0.7;
        public double blobNoiseScale = 0.08;
        public double blobCoherence = 0.6;
        public int transitionWidthChunks = 4;
        public double climateTolerance = 0.2;

        /**
         * Available biomes for climate matching.
         */
        public static class AvailableBiomesConfig {
            public boolean includeVanilla = true;
            public List<String> includeMods = new ArrayList<>();
            public List<String> excludeBiomes = new ArrayList<>();
        }

        /**
         * Temperature gradient configuration (north-south).
         */
        public static class TemperatureConfig {
            public double north = -1.0;  // Coldest
            public double south = 1.0;  // Hottest
            public double spawn = 0.0;  // Moderate
        }

        /**
         * Moisture gradient configuration (east-west).
         */
        public static class MoistureConfig {
            public double west = 1.0;  // Wettest
            public double east = -1.0;  // Driest
            public double spawn = 0.0;  // Moderate
        }
    }

    /**
     * Biome layer for layered mode.
     *
     * <p>Defines a vertical layer with specific biomes and cave percentage.
     */
    public static class BiomeLayer {
        public String name = "Unnamed Layer";
        public int yMin = 0;
        public int yMax = 64;
        public int percentage = 100;  // % of layer that's solid

        public List<String> biomes = new ArrayList<>();
        public int cavePercentage = 0;
        public String caveBiome = null;

        // For hollow layers (percentage = 0)
        public String biomeMode = "normal";  // "normal" or "single"
        public String biome = null;  // Single biome for entire layer
        public int ceilingThickness = 3;
        public int floorThickness = 1;
    }

    /**
     * World generation configuration.
     *
     * <p>Controls terrain generation, noise settings, caves, and dimension-specific mechanics.
     */
    public static class WorldGenerationConfig {
        public String dimensionType = "overworld";  // "overworld", "nether", "end", "custom"

        public SeedConfig seed = new SeedConfig();
        public NoiseConfig noise = new NoiseConfig();
        public CarverConfig carver = new CarverConfig();
        public AquiferConfig aquifers = new AquiferConfig();
        public OreGenerationConfig oreGeneration = new OreGenerationConfig();

        // Nether-type dimensions
        public boolean ceilingBedrock = false;
        public int lavaLevel = 32;
        public double ambientLight = 1.0;

        // End-type dimensions
        public int voidLevel = 0;
        public boolean islandGeneration = false;
        public boolean centralIsland = false;

        /**
         * Seed configuration.
         */
        public static class SeedConfig {
            public boolean mirrorOverworld = false;
            public String mirrorDimension = null;
            public Long customSeed = null;
        }

        /**
         * Noise configuration.
         */
        public static class NoiseConfig {
            public boolean mirrorOverworld = false;
            public String mirrorDimension = null;
            public String preset = "default";
            public CustomNoiseSettings customSettings = new CustomNoiseSettings();

            /**
             * Custom noise settings.
             */
            public static class CustomNoiseSettings {
                public TerrainSettings terrain = new TerrainSettings();
                public NoiseRouterSettings noiseRouter = new NoiseRouterSettings();

                /**
                 * Terrain settings.
                 */
                public static class TerrainSettings {
                    public int seaLevel = 63;
                    public int minY = -64;
                    public int maxY = 320;
                    public double verticalScale = 1.0;
                    public double horizontalScale = 1.0;
                }

                /**
                 * Noise router settings.
                 */
                public static class NoiseRouterSettings {
                    public NoiseParam temperature = new NoiseParam();
                    public NoiseParam vegetation = new NoiseParam();
                    public NoiseParam continentalness = new NoiseParam();
                    public NoiseParam erosion = new NoiseParam();
                    public NoiseParam depth = new NoiseParam();
                    public NoiseParam weirdness = new NoiseParam();

                    /**
                     * Individual noise parameter.
                     */
                    public static class NoiseParam {
                        public double amplitude = 1.0;
                        public double frequency = 0.5;
                    }
                }
            }
        }

        /**
         * Cave carver configuration.
         */
        public static class CarverConfig {
            public String type = "minecraft:default";
            public Map<String, Object> config = new HashMap<>();
        }

        /**
         * Aquifer configuration.
         */
        public static class AquiferConfig {
            public boolean enabled = true;
            public int lavaLevel = -54;
            public int waterLevel = 0;
        }

        /**
         * Ore generation configuration.
         */
        public static class OreGenerationConfig {
            public boolean mirrorOverworld = true;
            public Map<String, Double> multipliers = new HashMap<>();
            public Map<String, Map<String, Double>> perLayer = new HashMap<>();
        }
    }

    /**
     * Structure configuration.
     *
     * <p>Controls which structures generate in this dimension.
     */
    public static class StructureConfig {
        public String sourceDimension = null;
        public boolean limitToParentMods = true;
        public List<String> includeStructures = new ArrayList<>();
        public List<String> excludeStructures = new ArrayList<>();
    }

    /**
     * Placed feature configuration.
     *
     * <p>Controls decorations like trees, flowers, grass, etc.
     */
    public static class PlacedFeatureConfig {
        public boolean limitToParentMods = true;
        public DensityConfig density = new DensityConfig();

        /**
         * Density multipliers for features.
         */
        public static class DensityConfig {
            public double trees = 1.0;
            public double flowers = 1.0;
            public double grass = 1.0;
            public double ores = 1.0;
            public double mushrooms = 1.0;
            public double structures = 1.0;
        }
    }

    /**
     * Mob spawning configuration.
     *
     * <p>Controls mob spawn rates in this dimension.
     */
    public static class MobSpawningConfig {
        public boolean inheritFromBiomes = true;
        public Map<String, Double> spawnRates = new HashMap<>();
    }
}
