package com.builderscompanion.core.registry;

import com.builderscompanion.core.util.BCLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Registry for water colors from biomes and dyes.
 * Loads from classpath resource: /data/bccore/water_colors/registered_colors.txt
 */
public class WaterColorRegistry {

    private static final Map<String, Integer> biomeColors = new HashMap<>();
    private static final Map<String, Integer> dyeColors = new HashMap<>();
    private static final Map<Integer, ColorEntry> uniqueColors = new LinkedHashMap<>();

    private static boolean loaded = false;

    /**
     * Load water colors from classpath resource
     */
    public static void loadFromClasspath() {
        if (loaded) return;

        String resourcePath = "/data/bccore/water_colors/registered_colors.txt";
        InputStream stream = WaterColorRegistry.class.getResourceAsStream(resourcePath);

        if (stream == null) {
            BCLogger.error("Failed to find water color registry at: {}", resourcePath);
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            int lineNum = 0;
            int totalEntries = 0;
            int defaultColorCount = 0;

            // Skip header
            reader.readLine();
            lineNum++;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\|");
                if (parts.length < 3) {
                    BCLogger.warn("Skipping malformed line {}: {}", lineNum, line);
                    continue;
                }

                String sourceId = parts[0].trim();
                String hexColor = parts[1].trim();
                String category = parts[2].trim();

                // Parse hex color
                int color;
                try {
                    color = Integer.parseInt(hexColor.replace("0x", ""), 16);
                } catch (NumberFormatException e) {
                    BCLogger.warn("Invalid hex color '{}' on line {}", hexColor, lineNum);
                    continue;
                }

                totalEntries++;

                // Filter out default vanilla color
                if (color == 0x3F76E4) {
                    defaultColorCount++;
                    continue;
                }

                // Store in category map
                if (category.equals("biome")) {
                    biomeColors.put(sourceId, color);
                } else if (category.equals("dye")) {
                    dyeColors.put(sourceId, color);
                }

                // Add to unique colors map
                if (!uniqueColors.containsKey(color)) {
                    int typeId = uniqueColors.size();
                    uniqueColors.put(color, new ColorEntry(typeId, color, category));

                    BCLogger.debug("Added unique color: typeId={}, rgb=0x{}, category={}",
                            typeId, Integer.toHexString(color), category);
                }
            }

            loaded = true;

            BCLogger.info("Water color registry loaded:");
            BCLogger.info("  Total entries: {}", totalEntries);
            BCLogger.info("  Filtered (default color): {}", defaultColorCount);
            BCLogger.info("  Biome colors: {}", biomeColors.size());
            BCLogger.info("  Dye colors: {}", dyeColors.size());
            BCLogger.info("  Unique colors to register: {}", uniqueColors.size());

        } catch (IOException e) {
            BCLogger.error("Failed to load water color registry: {}", e.getMessage());
        }
        BCLogger.info("Unique colors breakdown:");
        for (ColorEntry entry : uniqueColors.values()) {
            BCLogger.info("  typeId={} rgb=0x{} category={}",
                    entry.typeId, Integer.toHexString(entry.rgb), entry.category);
        }
    }

    /**
     * Get color for a biome
     */
    public static Integer getBiomeColor(String biomeId) {
        return biomeColors.get(biomeId);
    }

    /**
     * Get color for a dye
     */
    public static Integer getDyeColor(String dyeId) {
        return dyeColors.get(dyeId);
    }

    /**
     * Get all unique colors for fluid registration
     */
    public static List<ColorEntry> getUniqueColors() {
        return new ArrayList<>(uniqueColors.values());
    }

    /**
     * Color entry for fluid registration
     */
    public static class ColorEntry {
        public final int typeId;
        public final int rgb;
        public final String category;

        public ColorEntry(int typeId, int rgb, String category) {
            this.typeId = typeId;
            this.rgb = rgb;
            this.category = category;
        }
    }

    /**
     * Get all biome names that have this color
     */
    public static List<String> getBiomesForColor(int rgb) {
        List<String> result = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : biomeColors.entrySet()) {
            if (entry.getValue() == rgb) {
                // Format: minecraft:swamp -> Swamp
                String biomeName = entry.getKey().contains(":")
                        ? entry.getKey().split(":")[1]
                        : entry.getKey();
                result.add(formatBiomeName(biomeName));
            }
        }

        return result;
    }

    private static String formatBiomeName(String name) {
        String[] words = name.split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (builder.length() > 0) builder.append(" ");
            builder.append(word.substring(0, 1).toUpperCase());
            builder.append(word.substring(1));
        }
        return builder.toString();
    }
}