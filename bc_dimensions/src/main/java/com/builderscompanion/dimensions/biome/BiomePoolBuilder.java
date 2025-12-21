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

import com.builderscompanion.core.config.DimensionConfig;
import com.builderscompanion.core.util.BCLogger;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.fml.ModList;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Builds a pool of biomes for a dimension from configuration.
 *
 * <p>This class processes dimension configuration to create a set of biomes that
 * should generate in that dimension. It handles:
 * <ul>
 *   <li>Wildcard patterns (e.g., "biomesoplenty:*")</li>
 *   <li>Include/exclude rules</li>
 *   <li>Vanilla biome inclusion</li>
 *   <li>Validation of biome existence</li>
 *   <li>Missing mod warnings</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * DimensionConfig config = BCConfigLoader.loadDimensionConfig("avalon");
 *
 * try {
 *     Set<Holder<Biome>> biomePool = BiomePoolBuilder.build(config);
 *     System.out.println("Dimension has " + biomePool.size() + " biomes");
 * } catch (IllegalArgumentException e) {
 *     System.err.println("Failed to build biome pool: " + e.getMessage());
 * }
 * }</pre>
 *
 * <p><b>Wildcard Patterns:</b>
 * <ul>
 *   <li><code>"biomesoplenty:*"</code> - All BOP biomes</li>
 *   <li><code>"minecraft:*"</code> - All vanilla biomes</li>
 *   <li><code>"terralith:*"</code> - All Terralith biomes</li>
 * </ul>
 *
 * <p><b>Processing Order:</b>
 * <ol>
 *   <li>Add vanilla biomes (if configured)</li>
 *   <li>Add wildcard mod biomes</li>
 *   <li>Add individual biomes</li>
 *   <li>Remove excluded biomes</li>
 * </ol>
 *
 * @since 1.0.0
 */
public class BiomePoolBuilder {

    private static final Pattern WILDCARD_PATTERN = Pattern.compile("^[a-z0-9_]+:\\*$");

    /**
     * Builds a biome pool from dimension configuration.
     *
     * <p>This method processes the biome configuration and returns a set of biome
     * holders that should generate in the dimension.
     *
     * <p><b>Throws:</b>
     * <ul>
     *   <li>{@link IllegalArgumentException} if required mods are missing</li>
     *   <li>{@link IllegalStateException} if BiomeMetadataDB is not initialized</li>
     * </ul>
     *
     * @param config the dimension configuration
     * @return set of biome holders for this dimension
     * @throws IllegalArgumentException if required mods are missing
     * @throws IllegalStateException if biome database is not initialized
     */
    public static Set<Holder<Biome>> build(DimensionConfig config) {
        if (!BiomeMetadataDB.isInitialized()) {
            throw new IllegalStateException("BiomeMetadataDB must be initialized before building biome pools");
        }

        if (config == null || config.biomes == null) {
            throw new IllegalArgumentException("Dimension config and biomes config cannot be null");
        }

        BCLogger.debug("Building biome pool for dimension: {}", config.displayName);

        Set<Holder<Biome>> pool = new HashSet<>();
        DimensionConfig.BiomeConfig biomes = config.biomes;

        // Step 1: Add vanilla biomes if configured
        if (biomes.includeVanilla) {
            addVanillaBiomes(pool, biomes.vanillaDimension);
        }

        // Step 2: Add mod biomes (wildcards and individuals)
        addModBiomes(pool, biomes.includeMods);

        // Step 2b: Add explicit biomes (from include_biomes)
        addModBiomes(pool, biomes.includeBiomes);

        // Step 3: Remove excluded biomes
        removeExcludedBiomes(pool, biomes.excludeBiomes);

        // Validate pool is not empty
        if (pool.isEmpty()) {
            BCLogger.warn("Biome pool for dimension '{}' is empty! Dimension may not generate properly.",
                    config.displayName);
        }

        BCLogger.info("Built biome pool for '{}': {} biomes", config.displayName, pool.size());
        return pool;
    }

    /**
     * Adds vanilla biomes to the pool.
     *
     * @param pool the biome pool to add to
     * @param vanillaDimension which vanilla dimension's biomes to use ("overworld", "nether", "end")
     */
    private static void addVanillaBiomes(Set<Holder<Biome>> pool, String vanillaDimension) {
        List<BiomeMetadata> vanillaBiomes = BiomeMetadataDB.getBiomesByMod("minecraft");

        if (vanillaBiomes.isEmpty()) {
            BCLogger.warn("No vanilla biomes found in BiomeMetadataDB!");
            return;
        }

        // Filter by dimension type if specified
        for (BiomeMetadata biome : vanillaBiomes) {
            boolean include = false;

            switch (vanillaDimension.toLowerCase()) {
                case "overworld":
                    // Include all non-nether, non-end biomes
                    include = biome.category != BCBiomeCategory.NETHER &&
                              biome.category != BCBiomeCategory.END;
                    break;

                case "nether":
                    include = biome.category == BCBiomeCategory.NETHER;
                    break;

                case "end":
                    include = biome.category == BCBiomeCategory.END;
                    break;

                default:
                    // Unknown dimension type, include all vanilla biomes
                    BCLogger.warn("Unknown vanilla dimension type '{}', including all vanilla biomes",
                            vanillaDimension);
                    include = true;
                    break;
            }

            if (include) {
                pool.add(biome.biomeHolder);
            }
        }

        BCLogger.debug("Added {} vanilla {} biomes", pool.size(), vanillaDimension);
    }

    /**
     * Adds mod biomes to the pool.
     *
     * <p>Handles both wildcard patterns and individual biome IDs.
     *
     * @param pool the biome pool to add to
     * @param includeMods list of mod patterns or biome IDs
     */
    private static void addModBiomes(Set<Holder<Biome>> pool, List<String> includeMods) {
        for (String pattern : includeMods) {
            if (isWildcard(pattern)) {
                addWildcardBiomes(pool, pattern);
            } else {
                addIndividualBiome(pool, pattern);
            }
        }
    }

    /**
     * Adds all biomes from a mod using wildcard pattern.
     *
     * @param pool the biome pool to add to
     * @param pattern the wildcard pattern (e.g., "biomesoplenty:*")
     */
    private static void addWildcardBiomes(Set<Holder<Biome>> pool, String pattern) {
        String modId = pattern.substring(0, pattern.length() - 2); // Remove ":*"

        // Check if mod is loaded
        if (!isModLoaded(modId)) {
            BCLogger.warn("Mod '{}' is not loaded. Biomes from this mod will be skipped.", modId);
            return;
        }

        // Get all biomes from this mod
        List<BiomeMetadata> modBiomes = BiomeMetadataDB.getBiomesByMod(modId);

        if (modBiomes.isEmpty()) {
            BCLogger.warn("Mod '{}' is loaded but has no biomes registered.", modId);
            return;
        }

        // Add all biomes from the mod
        int addedCount = 0;
        for (BiomeMetadata biome : modBiomes) {
            pool.add(biome.biomeHolder);
            addedCount++;
        }

        BCLogger.debug("Added {} biomes from mod '{}'", addedCount, modId);
    }

    /**
     * Adds a single biome by ID.
     *
     * @param pool the biome pool to add to
     * @param biomeId the biome resource location (e.g., "minecraft:plains")
     */
    private static void addIndividualBiome(Set<Holder<Biome>> pool, String biomeId) {
        ResourceLocation id;

        try {
            id = new ResourceLocation(biomeId);
        } catch (Exception e) {
            BCLogger.error("Invalid biome ID '{}': {}", biomeId, e.getMessage());
            return;
        }

        BiomeMetadata biome = BiomeMetadataDB.getBiome(id);

        if (biome == null) {
            BCLogger.warn("Biome '{}' not found. Check spelling or ensure the mod is loaded.", biomeId);
            return;
        }

        pool.add(biome.biomeHolder);
        BCLogger.trace("Added biome: {}", biomeId);
    }

    /**
     * Removes excluded biomes from the pool.
     *
     * @param pool the biome pool to remove from
     * @param excludeBiomes list of biome IDs to exclude
     */
    private static void removeExcludedBiomes(Set<Holder<Biome>> pool, List<String> excludeBiomes) {
        if (excludeBiomes.isEmpty()) {
            return;
        }

        int removedCount = 0;
        for (String biomeId : excludeBiomes) {
            ResourceLocation id;

            try {
                id = new ResourceLocation(biomeId);
            } catch (Exception e) {
                BCLogger.warn("Invalid exclude biome ID '{}': {}", biomeId, e.getMessage());
                continue;
            }

            BiomeMetadata biome = BiomeMetadataDB.getBiome(id);

            if (biome != null) {
                if (pool.remove(biome.biomeHolder)) {
                    removedCount++;
                    BCLogger.trace("Excluded biome: {}", biomeId);
                }
            } else {
                BCLogger.warn("Exclude biome '{}' not found in registry", biomeId);
            }
        }

        if (removedCount > 0) {
            BCLogger.debug("Excluded {} biomes from pool", removedCount);
        }
    }

    /**
     * Checks if a pattern is a wildcard pattern.
     *
     * @param pattern the pattern to check
     * @return true if it's a wildcard pattern (ends with ":*")
     */
    private static boolean isWildcard(String pattern) {
        return WILDCARD_PATTERN.matcher(pattern).matches();
    }

    /**
     * Checks if a mod is loaded.
     *
     * @param modId the mod ID
     * @return true if the mod is loaded
     */
    private static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    /**
     * Gets statistics about a biome pool.
     *
     * <p>This is useful for debugging and generating reports.
     *
     * @param pool the biome pool
     * @return statistics about the pool
     */
    public static BiomePoolStats getStats(Set<Holder<Biome>> pool) {
        Map<String, Integer> biomesByMod = new HashMap<>();
        Map<BCBiomeCategory, Integer> biomesByCategory = new HashMap<>();

        for (Holder<Biome> holder : pool) {
            ResourceLocation id = holder.unwrapKey()
                    .map(key -> key.location())
                    .orElse(new ResourceLocation("unknown", "unknown"));

            String modId = id.getNamespace();
            biomesByMod.merge(modId, 1, Integer::sum);

            BiomeMetadata metadata = BiomeMetadataDB.getBiome(id);
            if (metadata != null) {
                biomesByCategory.merge(metadata.category, 1, Integer::sum);
            }
        }

        return new BiomePoolStats(pool.size(), biomesByMod, biomesByCategory);
    }

    /**
     * Statistics about a biome pool.
     */
    public static class BiomePoolStats {
        public final int totalBiomes;
        public final Map<String, Integer> biomesByMod;
        public final Map<BCBiomeCategory, Integer> biomesByCategory;

        public BiomePoolStats(int totalBiomes,
                            Map<String, Integer> biomesByMod,
                            Map<BCBiomeCategory, Integer> biomesByCategory) {
            this.totalBiomes = totalBiomes;
            this.biomesByMod = Collections.unmodifiableMap(biomesByMod);
            this.biomesByCategory = Collections.unmodifiableMap(biomesByCategory);
        }

        /**
         * Gets a formatted summary of the stats.
         *
         * @return formatted string
         */
        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Total Biomes: ").append(totalBiomes).append("\n");

            sb.append("By Mod:\n");
            biomesByMod.forEach((mod, count) ->
                    sb.append("  ").append(mod).append(": ").append(count).append("\n"));

            sb.append("By Category:\n");
            biomesByCategory.forEach((category, count) ->
                    sb.append("  ").append(category).append(": ").append(count).append("\n"));

            return sb.toString();
        }
    }
}
