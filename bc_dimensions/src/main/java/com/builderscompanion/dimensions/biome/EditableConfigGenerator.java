/*
 * MIT License
 * Copyright (c) 2025 Builders Companion
 */
package com.builderscompanion.dimensions.biome;

import com.builderscompanion.core.config.DimensionConfig;
import com.builderscompanion.core.util.BCLogger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Generates EDITABLE configuration files for dimensions.
 * Users can modify these files and reload with /bc reload.
 */
public class EditableConfigGenerator {

    /**
     * Generates editable config for a Region Mode dimension.
     *
     * @param config The dimension config
     * @param regions The regions in this dimension
     */
    public static void generateRegionConfig(DimensionConfig config, List<RegionMetadata> regions) {
        String dimName = config.displayName.toLowerCase().replace(" ", "_");
        Path outputDir = Paths.get("config/builderscompanion/bc_dimensions/" + dimName);
        Path outputFile = outputDir.resolve("biome_regions.yml");

        try {
            Files.createDirectories(outputDir);

            try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
                writer.write("# ============================================================================\n");
                writer.write("# AUTO-GENERATED: Region Configuration for '" + dimName + "'\n");
                writer.write("# ============================================================================\n");
                writer.write("# You CAN edit this file! Changes apply on /bc reload\n");
                writer.write("#\n");
                writer.write("# This file shows all Terrablender regions competing in this dimension.\n");
                writer.write("# Adjust 'weight' values to control how common each mod's biomes are.\n");
                writer.write("#\n");
                writer.write("# Higher weight = more presence\n");
                writer.write("# - 10 = standard\n");
                writer.write("# - 20 = dominant\n");
                writer.write("# - 5 = rare\n");
                writer.write("# ============================================================================\n");
                writer.write("\n");
                writer.write("regions:\n");

                for (RegionMetadata region : regions) {
                    writer.write("  \"" + region.name + "\":\n");
                    writer.write("    weight: " + region.getCurrentWeight() + "              # EDIT THIS to change presence\n");
                    writer.write("    default_weight: " + region.defaultWeight + "\n");
                    writer.write("    mod: \"" + region.modId + "\"\n");
                    writer.write("    biome_count: " + region.getBiomeCount() + "\n");
                    writer.write("\n");
                }
            }

            BCLogger.info("Generated editable region config: {}", outputFile);

        } catch (IOException e) {
            BCLogger.error("Failed to generate editable region config for {}: {}",
                    dimName, e.getMessage());
        }
    }
}