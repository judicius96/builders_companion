package com.builderscompanion.biomewaters.registry;

import com.builderscompanion.core.registry.tintedliquids.TintedIdRanges;
import com.builderscompanion.core.util.BCLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Registry for water colors from biomes and dyes.
 * Loads from classpath resource: /data/biome_waters/biome_colors.txt
 */
public class WaterColorRegistry {

    private static final Map<String, Integer> biomeColors = new LinkedHashMap<>();
    private static final Map<String, Integer> dyeColors = new LinkedHashMap<>();
    private static final Map<Integer, ColorEntry> uniqueColors = new LinkedHashMap<>();
    private static final Map<Integer, ColorEntry> entriesByTypeId = new HashMap<>();
    private static final Map<Integer, String> primaryBiomeForColor = new LinkedHashMap<>();

    private static boolean loaded = false;

    /**
     * Load water colors from classpath resource
     */
    public static void loadFromClasspath() {
        if (loaded) return;

        biomeColors.clear();
        dyeColors.clear();
        uniqueColors.clear();
        entriesByTypeId.clear();
        primaryBiomeForColor.clear();

        String resourcePath = "/data/biome_waters/biome_colors.txt";
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

            // skip header
            reader.readLine();
            lineNum++;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\|");
                if (parts.length < 3) {
                    BCLogger.warn("Skipping malformed line {}: {}", lineNum, line);
                    continue;
                }

                String sourceId = parts[0].trim();
                String hexColor = parts[1].trim();
                String category = parts[2].trim();

                int color;
                try {
                    color = Integer.parseInt(hexColor.replace("0x", ""), 16);
                } catch (NumberFormatException e) {
                    BCLogger.warn("Invalid hex color '{}' on line {}", hexColor, lineNum);
                    continue;
                }

                totalEntries++;

                // Skip vanilla default water color
                if (color == 0x3F76E4) {
                    defaultColorCount++;
                    continue;
                }

                if ("biome".equals(category)) {
                    biomeColors.put(sourceId, color);
                    primaryBiomeForColor.putIfAbsent(
                            color,
                            formatBiomeName(extractPath(sourceId))
                    );
                } else if ("dye".equals(category)) {
                    dyeColors.put(sourceId, color);
                } else {
                    BCLogger.warn(
                            "Unexpected category '{}' in biome_colors.txt on line {}",
                            category, lineNum
                    );
                    continue;
                }

                // Allocate biome TypeIDs strictly below dye start
                if (!uniqueColors.containsKey(color)) {
                    int nextTypeId = uniqueColors.size();

                    if (nextTypeId >= TintedIdRanges.DYE_START) {
                        BCLogger.error(
                                "BiomeWaters exceeded biome tint budget ({}). " +
                                        "Skipping rgb=0x{} sourceId={}",
                                TintedIdRanges.DYE_START,
                                Integer.toHexString(color),
                                sourceId
                        );
                        continue;
                    }

                    ColorEntry ce = new ColorEntry(nextTypeId, color, category);
                    uniqueColors.put(color, ce);
                }
            }

            // Build reverse lookup cache ONCE
            for (ColorEntry entry : uniqueColors.values()) {
                entriesByTypeId.put(entry.typeId, entry);
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
            BCLogger.info(
                    "  typeId={} rgb=0x{} category={}",
                    entry.typeId,
                    Integer.toHexString(entry.rgb),
                    entry.category
            );
        }
    }

    public static ColorEntry getEntryByTypeId(int typeId) {
        return entriesByTypeId.get(typeId);
    }

    /**
     * Get primary/canonical biome display name for a color.
     * Used for deterministic bucket naming.
     */
    public static String getPrimaryBiomeForColor(int rgb) {
        return primaryBiomeForColor.get(rgb);
    }

    public static Integer getBiomeColor(String biomeId) {
        return biomeColors.get(biomeId);
    }

    public static Integer getDyeColor(String dyeId) {
        return dyeColors.get(dyeId);
    }

    public static List<ColorEntry> getUniqueColors() {
        return new ArrayList<>(uniqueColors.values());
    }

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
            Integer v = entry.getValue();
            if (v != null && v == rgb) {
                result.add(formatBiomeName(extractPath(entry.getKey())));
            }
        }
        return result;
    }

    private static String extractPath(String id) {
        int idx = id.indexOf(':');
        return (idx >= 0 && idx + 1 < id.length())
                ? id.substring(idx + 1)
                : id;
    }

    private static String formatBiomeName(String name) {
        String[] words = name.split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (builder.length() > 0) builder.append(" ");
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) builder.append(word.substring(1));
        }
        return builder.toString();
    }
}
