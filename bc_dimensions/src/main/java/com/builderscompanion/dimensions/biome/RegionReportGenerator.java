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
package com.builderscompanion.dimensions.biome;

import com.builderscompanion.core.util.BCLogger;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates transparency reports for Terrablender regions.
 *
 * <p>This creates human-readable YAML files showing:
 * <ul>
 *   <li>All BC regions registered for each dimension</li>
 *   <li>Region weights (default and overridden)</li>
 *   <li>Biome counts and percentages</li>
 *   <li>Mod influence analysis</li>
 * </ul>
 *
 * <p><b>Output Location:</b>
 * <pre>
 * config/builderscompanion/bc_dimensions/
 *   ├─ avalon_regions.yml
 *   ├─ terra_regions.yml
 *   └─ ...
 * </pre>
 *
 * <p><b>Purpose:</b>
 * <p>These reports provide transparency into how biomes are distributed,
 * helping users understand and tune their dimension configurations.
 *
 * <p><b>When Reports Are Generated:</b>
 * <ul>
 *   <li>Automatically when server starts</li>
 *   <li>Via {@code /bc reload} command</li>
 *   <li>Manually via {@code /bc region report <dimension>} command</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class RegionReportGenerator {

    private static final Path REPORTS_DIR = Paths.get("config/builderscompanion/bc_dimensions");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Generates reports for all dimensions with BC regions.
     *
     * <p>This creates a separate YAML file for each dimension.
     */
    public static void generateAllReports() {
        // Ensure reports directory exists
        try {
            Files.createDirectories(REPORTS_DIR);
        } catch (IOException e) {
            BCLogger.error("Failed to create reports directory: {}", e.getMessage());
            return;
        }

        // Get all dimensions that have BC regions
        List<ResourceKey<Level>> dimensions = RegionMetadataDB.getAllRegions().stream()
                .map(region -> region.dimension)
                .distinct()
                .collect(Collectors.toList());

        if (dimensions.isEmpty()) {
            BCLogger.debug("No BC regions to report on");
            return;
        }

        BCLogger.info("Generating region reports for {} dimensions...", dimensions.size());

        int successCount = 0;
        for (ResourceKey<Level> dimension : dimensions) {
            if (generateReport(dimension)) {
                successCount++;
            }
        }

        BCLogger.info("Generated {} region reports in {}", successCount, REPORTS_DIR);
    }

    /**
     * Generates a report for a specific dimension.
     *
     * @param dimension the dimension
     * @return true if report was generated successfully
     */
    public static boolean generateReport(ResourceKey<Level> dimension) {
        List<RegionMetadata> regions = RegionMetadataDB.getRegionsForDimension(dimension);

        if (regions.isEmpty()) {
            BCLogger.debug("No BC regions for dimension: {}", dimension.location());
            return false;
        }

        // Create filename from dimension ID
        String filename = dimension.location().getPath() + "_regions.yml";
        Path reportFile = REPORTS_DIR.resolve(filename);

        try (BufferedWriter writer = Files.newBufferedWriter(reportFile)) {
            writeReport(writer, dimension, regions);
            BCLogger.debug("Generated region report: {}", reportFile);
            return true;
        } catch (IOException e) {
            BCLogger.error("Failed to write region report for {}: {}",
                    dimension.location(), e.getMessage());
            return false;
        }
    }

    /**
     * Writes the report content to a file.
     *
     * @param writer the file writer
     * @param dimension the dimension
     * @param regions the regions for this dimension
     * @throws IOException if write fails
     */
    private static void writeReport(BufferedWriter writer, ResourceKey<Level> dimension,
                                    List<RegionMetadata> regions) throws IOException {
        // Header
        writer.write("# BC:Dimensions Region Report\n");
        writer.write("# Auto-generated by Builders Companion: Dimensions\n");
        writer.write("# DO NOT EDIT - This file will be regenerated\n");
        writer.write("# Generated: " + LocalDateTime.now().format(DATE_FORMAT) + "\n");
        writer.write("\n");

        // Dimension info
        writer.write("dimension: \"" + dimension.location() + "\"\n");
        writer.write("total_regions: " + regions.size() + "\n");

        int totalWeight = regions.stream()
                .mapToInt(RegionMetadata::getCurrentWeight)
                .sum();
        writer.write("total_weight: " + totalWeight + "\n");
        writer.write("\n");

        // Regions section
        writer.write("regions:\n");
        for (RegionMetadata region : regions) {
            writeRegionSection(writer, region, totalWeight);
        }

        // Analysis section
        writer.write("\n");
        writer.write("analysis:\n");
        writeAnalysisSection(writer, regions, totalWeight);
    }

    /**
     * Writes information about a single region.
     *
     * @param writer the file writer
     * @param region the region
     * @param totalWeight total weight of all regions
     * @throws IOException if write fails
     */
    private static void writeRegionSection(BufferedWriter writer, RegionMetadata region,
                                          int totalWeight) throws IOException {
        double influence = (region.getCurrentWeight() / (double) totalWeight) * 100.0;

        writer.write("  \"" + region.name + "\":\n");
        writer.write("    mod: \"" + region.modId + "\"\n");
        writer.write("    weight: " + region.getCurrentWeight() + "\n");

        if (region.isWeightOverridden()) {
            writer.write("    default_weight: " + region.defaultWeight + "\n");
            writer.write("    override_applied: true\n");
        } else {
            writer.write("    override_applied: false\n");
        }

        writer.write(String.format("    influence: \"%.1f%%\"\n", influence));
        writer.write("    biome_count: " + region.getBiomeCount() + "\n");
        writer.write("    biomes:\n");

        // List first 10 biomes (to keep report manageable)
        int biomeCount = 0;
        for (ResourceLocation biomeId : region.biomes) {
            if (biomeCount >= 10) {
                writer.write("      # ... and " + (region.biomes.size() - 10) + " more\n");
                break;
            }
            writer.write("      - \"" + biomeId + "\"\n");
            biomeCount++;
        }

        writer.write("\n");
    }

    /**
     * Writes analysis section showing regional influence and recommendations.
     *
     * @param writer the file writer
     * @param regions the regions
     * @param totalWeight total weight
     * @throws IOException if write fails
     */
    private static void writeAnalysisSection(BufferedWriter writer, List<RegionMetadata> regions,
                                            int totalWeight) throws IOException {
        // Calculate influence percentages
        writer.write("  influence_breakdown:\n");
        for (RegionMetadata region : regions) {
            double influence = (region.getCurrentWeight() / (double) totalWeight) * 100.0;
            writer.write(String.format("    \"%s\": \"%.1f%%\"\n", region.name, influence));
        }

        writer.write("\n");

        // Recommendations
        writer.write("  recommendations:\n");

        // Find dominant region (if any has >50% influence)
        for (RegionMetadata region : regions) {
            double influence = (region.getCurrentWeight() / (double) totalWeight) * 100.0;
            if (influence > 50.0) {
                writer.write("    - \"⚠ Region '" + region.name + "' has " +
                        String.format("%.1f%%", influence) +
                        " influence - very dominant\"\n");
                writer.write("    - \"💡 To reduce, lower weight in region_overrides config\"\n");
            }
        }

        // Check for unused overrides
        boolean hasOverrides = regions.stream().anyMatch(RegionMetadata::isWeightOverridden);
        if (!hasOverrides) {
            writer.write("    - \"💡 No weight overrides active - all regions using defaults\"\n");
            writer.write("    - \"💡 Use region_overrides in config to adjust influence\"\n");
        }

        if (regions.size() == 1) {
            writer.write("    - \"ℹ Only one BC region registered for this dimension\"\n");
        }
    }

    /**
     * Deletes all generated reports.
     *
     * <p>Used during shutdown or reload to clean up old reports.
     */
    public static void cleanupReports() {
        try {
            if (Files.exists(REPORTS_DIR)) {
                Files.list(REPORTS_DIR)
                        .filter(path -> path.toString().endsWith("_regions.yml"))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                                BCLogger.debug("Deleted report: {}", path.getFileName());
                            } catch (IOException e) {
                                BCLogger.warn("Failed to delete report {}: {}",
                                        path.getFileName(), e.getMessage());
                            }
                        });
            }
        } catch (IOException e) {
            BCLogger.warn("Failed to cleanup region reports: {}", e.getMessage());
        }
    }
}
