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
package com.builderscompanion.dimensions.worldgen;

import com.builderscompanion.core.config.DimensionConfig;
import com.builderscompanion.core.util.BCLogger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Generates Minecraft dimension JSON files from DimensionConfig.
 *
 * <p>This generator creates two JSON files for each dimension:
 * <ul>
 *   <li>{@code dimension_type/<id>.json} - Dimension properties (physics, mechanics)</li>
 *   <li>{@code dimension/<id>.json} - World generation settings (biomes, terrain)</li>
 * </ul>
 *
 * <p>Generated files are placed in:
 * {@code config/builderscompanion/generated/data/bc_dimensions/}
 *
 * @since 1.0.0
 */
public class DimensionJsonGenerator {

    private static final Path GENERATED_DIR = Paths.get("config", "builderscompanion", "generated", "data", "bc_dimensions");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Generates both dimension type and dimension JSON files for a dimension.
     *
     * @param config the dimension configuration
     * @param dimensionId the dimension ID (e.g., "sand_world")
     */
    public static void generateDimensionFiles(DimensionConfig config, String dimensionId) {
        try {
            // Create directories
            Files.createDirectories(GENERATED_DIR.resolve("dimension_type"));
            Files.createDirectories(GENERATED_DIR.resolve("dimension"));

            // Generate dimension type JSON
            generateDimensionType(config, dimensionId);

            // Generate dimension JSON
            generateDimension(config, dimensionId);

            BCLogger.info("Generated dimension files for: {}", dimensionId);

        } catch (IOException e) {
            BCLogger.error("Failed to generate dimension files for {}: {}", dimensionId, e.getMessage());
        }
    }

    /**
     * Generates the dimension_type JSON file.
     *
     * <p>This defines the dimension's physics and mechanics:
     * <ul>
     *   <li>Coordinate scaling (portal offset)</li>
     *   <li>World height and boundaries</li>
     *   <li>Environmental properties (skylight, ceiling, ambient light)</li>
     *   <li>Gameplay mechanics (beds work, respawn anchors, raids)</li>
     * </ul>
     *
     * @param config the dimension configuration
     * @param dimensionId the dimension ID
     */
    private static void generateDimensionType(DimensionConfig config, String dimensionId) throws IOException {
        Path outputPath = GENERATED_DIR.resolve("dimension_type").resolve(dimensionId + ".json");

        JsonObject dimensionType = new JsonObject();

        // Determine dimension type properties based on config
        boolean isNetherLike = config.worldGeneration.dimensionType.equals("nether");
        boolean isEndLike = config.worldGeneration.dimensionType.equals("end");

        // Basic properties
        dimensionType.addProperty("ultrawarm", isNetherLike);
        dimensionType.addProperty("natural", true);
        dimensionType.addProperty("piglin_safe", isNetherLike);
        dimensionType.addProperty("respawn_anchor_works", isNetherLike);
        dimensionType.addProperty("bed_works", !isNetherLike && !isEndLike);
        dimensionType.addProperty("has_raids", !isNetherLike && !isEndLike);
        dimensionType.addProperty("has_skylight", !isNetherLike);
        dimensionType.addProperty("has_ceiling", config.worldGeneration.ceilingBedrock);

        // Coordinate scaling for portals
        dimensionType.addProperty("coordinate_scale", config.portal.portalOffset);

        // Ambient light (0.0 = normal, 0.1 = dim like nether, 1.0 = bright)
        dimensionType.addProperty("ambient_light", config.worldGeneration.ambientLight);

        // World height
        dimensionType.addProperty("min_y", -64);
        dimensionType.addProperty("height", 384);
        dimensionType.addProperty("logical_height", 384);

        // Infiniburn tag
        String infiniburnTag = isNetherLike ? "#minecraft:infiniburn_nether" :
                isEndLike ? "#minecraft:infiniburn_end" :
                        "#minecraft:infiniburn_overworld";
        dimensionType.addProperty("infiniburn", infiniburnTag);

        // Visual effects
        String effects = isNetherLike ? "minecraft:the_nether" :
                isEndLike ? "minecraft:the_end" :
                        "minecraft:overworld";
        dimensionType.addProperty("effects", effects);

        // Monster spawning
        dimensionType.addProperty("monster_spawn_light_level", 0);
        dimensionType.addProperty("monster_spawn_block_light_limit", 7);

        // Write to file
        writeJson(outputPath, dimensionType);
        BCLogger.debug("Generated dimension_type for: {}", dimensionId);
    }

    /**
     * Generates the dimension JSON file.
     *
     * <p>This defines the dimension's world generation:
     * <ul>
     *   <li>Biome distribution (BiomeSource)</li>
     *   <li>Terrain shape (noise settings)</li>
     *   <li>Seed</li>
     * </ul>
     *
     * @param config the dimension configuration
     * @param dimensionId the dimension ID
     */
    private static void generateDimension(DimensionConfig config, String dimensionId) throws IOException {
        Path outputPath = GENERATED_DIR.resolve("dimension").resolve(dimensionId + ".json");

        JsonObject dimension = new JsonObject();

        // Reference to dimension type
        dimension.addProperty("type", "bc_dimensions:" + dimensionId);

        // Generator
        JsonObject generator = new JsonObject();
        generator.addProperty("type", "minecraft:noise");
        generator.addProperty("seed", 0); // Will be set at runtime

        // Noise settings (terrain shape)
        String noiseSettings = getNoiseSettings(config);
        generator.addProperty("settings", noiseSettings);

        // Biome source
        JsonObject biomeSource = new JsonObject();
        String biomeSourceType = getBiomeSourceType(config);
        biomeSource.addProperty("type", biomeSourceType);
        biomeSource.addProperty("seed", 0); // Will be set at runtime

        generator.add("biome_source", biomeSource);
        dimension.add("generator", generator);

        // Write to file
        writeJson(outputPath, dimension);
        BCLogger.debug("Generated dimension for: {}", dimensionId);
    }

    /**
     * Determines the noise settings to use based on dimension type.
     *
     * @param config the dimension configuration
     * @return the noise settings resource location
     */
    private static String getNoiseSettings(DimensionConfig config) {
        return switch (config.worldGeneration.dimensionType) {
            case "nether" -> "minecraft:nether";
            case "end" -> "minecraft:end";
            case "custom" -> "bc_dimensions:" + config.id; // Custom noise will be generated separately
            default -> "minecraft:overworld";
        };
    }

    /**
     * Determines the biome source type based on biome mode.
     *
     * @param config the dimension configuration
     * @return the biome source type
     */
    private static String getBiomeSourceType(DimensionConfig config) {
        return switch (config.biomes.mode) {
            case "region" -> "bc_dimensions:region";
            case "climate_grid" -> "bc_dimensions:climate_grid";
            case "layered" -> "bc_dimensions:layered";
            default -> "minecraft:multi_noise"; // Fallback
        };
    }

    /**
     * Writes a JsonObject to a file with pretty printing.
     *
     * @param path the output file path
     * @param json the JSON object to write
     */
    private static void writeJson(Path path, JsonObject json) throws IOException {
        Files.createDirectories(path.getParent());
        String jsonString = GSON.toJson(json);
        Files.writeString(path, jsonString);
    }
}