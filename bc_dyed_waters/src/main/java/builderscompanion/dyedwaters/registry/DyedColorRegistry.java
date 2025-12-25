package com.builderscompanion.dyedwaters.registry;

import com.builderscompanion.core.registry.tintedliquids.TintedLiquidsRegistry;
import com.builderscompanion.core.util.BCLogger;
import com.builderscompanion.core.registry.tintedliquids.TintedIdRanges;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Registry for dye water colors.
 * Loads from classpath resource: /data/dyed_waters/dye_colors.txt
 */
public class DyedColorRegistry {

    // Variants (Water, Infused Water, Radiant Water, Infused Radiant Water)
    private static final int VARIANTS = 4;

    private static final Map<String, Integer> dyeColors = new LinkedHashMap<>();
    private static final List<ColorEntry> uniqueColors = new ArrayList<>();
    private static final Map<Integer, ColorEntry> entriesByTypeId = new LinkedHashMap<>();
    private static final Map<Integer, String> primaryDyeForColor = new LinkedHashMap<>();

    private static boolean loaded = false;

    /**
     * Load dye colors from classpath resource
     */
    public static void loadFromClasspath() {
        if (loaded) return;

        dyeColors.clear();
        uniqueColors.clear();
        entriesByTypeId.clear();
        primaryDyeForColor.clear();

        String resourcePath = "/data/dyed_waters/dye_colors.txt";
        InputStream stream = DyedColorRegistry.class.getResourceAsStream(resourcePath);

        if (stream == null) {
            BCLogger.error("Failed to find dye color registry at: {}", resourcePath);
            return;
        }

        // Tracks unique rgb colors to ensure 1:1 dye->rgb allocation (deterministic by file order)
        Set<Integer> seenRgb = new HashSet<>();
        int dyeIndex = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            int lineNum = 0;
            int totalEntries = 0;

            reader.readLine(); // Skip header
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

                int rgb;
                try {
                    rgb = Integer.parseInt(hexColor.replace("0x", ""), 16);
                } catch (NumberFormatException e) {
                    BCLogger.warn("Invalid hex color '{}' on line {}", hexColor, lineNum);
                    continue;
                }

                totalEntries++;

                // Only accept "dye" category
                if ("dye".equals(category)) {
                    dyeColors.put(sourceId, rgb);
                    primaryDyeForColor.putIfAbsent(rgb, formatDyeName(extractPath(sourceId)));
                } else {
                    BCLogger.warn("Unexpected category '{}' in dye_colors.txt on line {}", category, lineNum);
                    continue;
                }

                // Allocate 4 TypeIDs per unique dye color (dyed/infused/radiant/infused_radiant)
                if (seenRgb.add(rgb)) {
                    int baseTypeId = TintedIdRanges.DYE_START + (dyeIndex * VARIANTS);

                    uniqueColors.add(new ColorEntry(baseTypeId + 0, rgb, "dyed"));
                    uniqueColors.add(new ColorEntry(baseTypeId + 1, rgb, "infused"));
                    uniqueColors.add(new ColorEntry(baseTypeId + 2, rgb, "radiant"));
                    uniqueColors.add(new ColorEntry(baseTypeId + 3, rgb, "infused_radiant"));

                    dyeIndex++;
                }
            }

            // Build cache ONCE
            for (ColorEntry entry : uniqueColors) {
                entriesByTypeId.put(entry.typeId, entry);
            }

            loaded = true;

            BCLogger.info("Dye color registry loaded:");
            BCLogger.info("  Total entries: {}", totalEntries);
            BCLogger.info("  Dye colors: {}", dyeColors.size());
            BCLogger.info("  Unique dye colors (rgb): {}", seenRgb.size());
            BCLogger.info("  Total TypeIDs to register (rgb * 4): {}", uniqueColors.size());

        } catch (IOException e) {
            BCLogger.error("Failed to load dye color registry: {}", e.getMessage());
        }

        BCLogger.info("Unique dye colors breakdown:");
        for (ColorEntry entry : uniqueColors) {
            BCLogger.info("  typeId={} rgb=0x{} category={}",
                    entry.typeId, Integer.toHexString(entry.rgb), entry.category);
        }
    }

    /**
     * Get ColorEntry by typeId
     */
    public static ColorEntry getEntryByTypeId(int typeId) {
        return entriesByTypeId.get(typeId);
    }

    /**
     * Get primary/canonical dye display name for a color.
     * Used for deterministic bucket naming.
     */
    public static String getPrimaryDyeForColor(int rgb) {
        return primaryDyeForColor.get(rgb);
    }

    /**
     * Get color for a dye ID
     */
    public static Integer getDyeColor(String dyeId) {
        return dyeColors.get(dyeId);
    }

    /**
     * Get all unique colors for fluid registration (includes 4 variants per rgb)
     */
    public static List<ColorEntry> getUniqueColors() {
        return new ArrayList<>(uniqueColors);
    }

    /**
     * Get all ColorRegistrations for Core tinted liquids registration.
     */
    public static List<TintedLiquidsRegistry.ColorRegistration> getRegistrations() {
        List<TintedLiquidsRegistry.ColorRegistration> out = new ArrayList<>(uniqueColors.size());
        for (ColorEntry e : uniqueColors) {
            out.add(new TintedLiquidsRegistry.ColorRegistration(e.typeId, e.rgb, e.category));
        }
        return out;
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
     * Get all dye names that have this color (should typically be 1)
     */
    public static List<String> getDyesForColor(int rgb) {
        List<String> result = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : dyeColors.entrySet()) {
            Integer v = entry.getValue();
            if (v != null && v == rgb) {
                result.add(formatDyeName(extractPath(entry.getKey())));
            }
        }
        return result;
    }

    private static String extractPath(String id) {
        // "minecraft:red_dye" -> "red_dye"
        int idx = id.indexOf(':');
        return (idx >= 0 && idx + 1 < id.length()) ? id.substring(idx + 1) : id;
    }

    private static String formatDyeName(String name) {
        // "red_dye" -> "Red"
        String withoutDye = name.replace("_dye", "");
        String[] words = withoutDye.split("_");
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
