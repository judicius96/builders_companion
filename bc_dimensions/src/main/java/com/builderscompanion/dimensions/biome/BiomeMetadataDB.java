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
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Database of all registered biomes with metadata.
 *
 * <p>This class captures biome registrations from all mods during server startup
 * and provides query methods for dimension generation. It stores metadata including:
 * <ul>
 *   <li>Source mod ID</li>
 *   <li>Climate parameters (temperature, moisture)</li>
 *   <li>Biome tags</li>
 *   <li>Biome category</li>
 * </ul>
 *
 * <p><b>Why Server Startup?</b> Biomes are registered during mod loading, but we
 * need to access the full registry including tags, which are only available after
 * data packs are loaded. We capture everything during {@link ServerAboutToStartEvent}.
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * // Get all biomes from a specific mod
 * List<BiomeMetadata> bopBiomes = BiomeMetadataDB.getBiomesByMod("biomesoplenty");
 *
 * // Get specific biome
 * BiomeMetadata plains = BiomeMetadataDB.getBiome(
 *     new ResourceLocation("minecraft", "plains")
 * );
 *
 * // Find biomes matching climate
 * List<BiomeMetadata> coldBiomes = BiomeMetadataDB.getBiomesByClimate(
 *     -0.5f, 0.0f, 0.3
 * );
 * }</pre>
 *
 * <p><b>Thread Safety:</b> This class is thread-safe and can be queried from
 * chunk generation threads.
 *
 * @since 1.0.0
 */
public class BiomeMetadataDB {

    private static final Map<ResourceLocation, BiomeMetadata> BIOMES = new ConcurrentHashMap<>();
    private static final Map<String, List<BiomeMetadata>> BIOMES_BY_MOD = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    /**
     * Captures biome registrations when the server starts.
     *
     * <p>This is called automatically by Forge. We use ServerAboutToStartEvent
     * because it fires after all biomes are registered and after data packs
     * (which provide tags) are loaded.
     *
     * <p><b>Internal use only.</b>
     *
     * @param event the server startup event
     */
    @SubscribeEvent
    public static void onServerStarting(ServerAboutToStartEvent event) {
        if (initialized) {
            BCLogger.debug("BiomeMetadataDB already initialized, skipping");
            return;
        }

        BCLogger.info("Capturing biome metadata from registry...");

        try {
            // Get the biome registry
            Registry<Biome> biomeRegistry = event.getServer()
                    .registryAccess()
                    .registryOrThrow(Registries.BIOME);

            // Clear existing data (for hot reload support)
            BIOMES.clear();
            BIOMES_BY_MOD.clear();

            // Capture all biomes
            int count = 0;
            for (Map.Entry<ResourceKey<Biome>, Biome> entry : biomeRegistry.entrySet()) {
                ResourceLocation id = entry.getKey().location();
                String modId = id.getNamespace();
                Biome biome = entry.getValue();

                // Get the biome holder
                Holder<Biome> holder = biomeRegistry.getHolderOrThrow(entry.getKey());

                // Extract climate parameters
                float temperature = normalizeTemperature(biome.getBaseTemperature());
                float moisture = normalizeMoisture(biome.getModifiedClimateSettings().downfall());

                // Extract tags
                Set<TagKey<Biome>> tags = extractTags(holder);

                // Determine category from tags
                BCBiomeCategory category = BCBiomeCategory.fromBiome(holder);

                // Create metadata
                BiomeMetadata metadata = new BiomeMetadata(
                        id, modId, holder, temperature, moisture, tags, category
                );

                // Store in main map
                BIOMES.put(id, metadata);

                // Store in mod map
                BIOMES_BY_MOD.computeIfAbsent(modId, k -> new ArrayList<>()).add(metadata);

                count++;
            }

            initialized = true;

            // Log summary
            long modCount = BIOMES_BY_MOD.keySet().size();
            BCLogger.info("Captured {} biomes from {} mods", count, modCount);

            // Log per-mod counts
            if (BCLogger.isDebugMode()) {
                BIOMES_BY_MOD.forEach((modId, biomes) -> {
                    BCLogger.debug("  {} - {} biomes", modId, biomes.size());
                });
            }

        } catch (Exception e) {
            BCLogger.error("Failed to capture biome metadata: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gets all biomes from a specific mod.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * List<BiomeMetadata> bopBiomes = BiomeMetadataDB.getBiomesByMod("biomesoplenty");
     * for (BiomeMetadata biome : bopBiomes) {
     *     System.out.println(biome.id);
     * }
     * }</pre>
     *
     * @param modId the mod ID (e.g., "minecraft", "biomesoplenty")
     * @return list of biomes from that mod, or empty list if none found
     */
    public static List<BiomeMetadata> getBiomesByMod(String modId) {
        return BIOMES_BY_MOD.getOrDefault(modId, Collections.emptyList());
    }

    /**
     * Gets biome metadata by resource location.
     *
     * @param id the biome ID (e.g., "minecraft:plains")
     * @return the biome metadata, or null if not found
     */
    @Nullable
    public static BiomeMetadata getBiome(ResourceLocation id) {
        return BIOMES.get(id);
    }

    /**
     * Gets all registered biomes.
     *
     * @return unmodifiable collection of all biome metadata
     */
    public static Collection<BiomeMetadata> getAllBiomes() {
        return Collections.unmodifiableCollection(BIOMES.values());
    }

    /**
     * Gets all mod IDs that have registered biomes.
     *
     * @return unmodifiable set of mod IDs
     */
    public static Set<String> getAllModIds() {
        return Collections.unmodifiableSet(BIOMES_BY_MOD.keySet());
    }

    /**
     * Finds biomes matching a climate within tolerance.
     *
     * <p>This is used by climate grid mode to select biomes that fit a climate zone.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * // Find cold, wet biomes (taiga, snowy taiga, etc.)
     * List<BiomeMetadata> coldWet = BiomeMetadataDB.getBiomesByClimate(
     *     -0.5f,  // Cold temperature
     *     0.5f,   // Moderate-high moisture
     *     0.3     // Tolerance
     * );
     * }</pre>
     *
     * @param temperature target temperature (-1.0 to 1.0)
     * @param moisture target moisture (-1.0 to 1.0)
     * @param tolerance maximum climate distance
     * @return list of matching biomes
     */
    public static List<BiomeMetadata> getBiomesByClimate(float temperature, float moisture, double tolerance) {
        return BIOMES.values().stream()
                .filter(biome -> biome.isClimateMatch(temperature, moisture, tolerance))
                .collect(Collectors.toList());
    }

    /**
     * Finds biomes by category.
     *
     * @param category the biome category (e.g., PLAINS, FOREST, OCEAN)
     * @return list of biomes in that category
     */
    public static List<BiomeMetadata> getBiomesByCategory(BCBiomeCategory category) {
        return BIOMES.values().stream()
                .filter(biome -> biome.category == category)
                .collect(Collectors.toList());
    }

    /**
     * Finds biomes with a specific tag.
     *
     * @param tag the biome tag
     * @return list of biomes with that tag
     */
    public static List<BiomeMetadata> getBiomesWithTag(TagKey<Biome> tag) {
        return BIOMES.values().stream()
                .filter(biome -> biome.hasTag(tag))
                .collect(Collectors.toList());
    }

    /**
     * Checks if a biome exists in the registry.
     *
     * @param id the biome resource location
     * @return true if the biome exists
     */
    public static boolean hasBiome(ResourceLocation id) {
        return BIOMES.containsKey(id);
    }

    /**
     * Checks if a mod has registered any biomes.
     *
     * @param modId the mod ID
     * @return true if the mod has biomes
     */
    public static boolean hasModBiomes(String modId) {
        return BIOMES_BY_MOD.containsKey(modId);
    }

    /**
     * Gets the number of registered biomes.
     *
     * @return total biome count
     */
    public static int getBiomeCount() {
        return BIOMES.size();
    }

    /**
     * Checks if the database has been initialized.
     *
     * @return true if initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Clears all cached metadata.
     *
     * <p><b>Internal use only.</b> Used for hot reload.
     */
    public static void clear() {
        BIOMES.clear();
        BIOMES_BY_MOD.clear();
        initialized = false;
        BCLogger.debug("Cleared BiomeMetadataDB");
    }

    /**
     * Normalizes Minecraft temperature to -1.0 to 1.0 scale.
     *
     * <p>Minecraft temperature ranges from -0.5 (frozen) to 2.0 (hot).
     * We normalize to -1.0 (coldest) to 1.0 (hottest) for consistent climate calculations.
     *
     * @param mcTemp Minecraft temperature
     * @return normalized temperature (-1.0 to 1.0)
     */
    private static float normalizeTemperature(float mcTemp) {
        // Minecraft range: -0.5 to 2.0
        // Clamp and normalize to -1.0 to 1.0
        float clamped = Math.max(-0.5f, Math.min(2.0f, mcTemp));
        return (clamped - 0.75f) / 1.25f;
    }

    /**
     * Normalizes Minecraft downfall/moisture to -1.0 to 1.0 scale.
     *
     * <p>Minecraft downfall ranges from 0.0 (dry) to 1.0 (wet).
     * We normalize to -1.0 (driest) to 1.0 (wettest).
     *
     * @param downfall Minecraft downfall value
     * @return normalized moisture (-1.0 to 1.0)
     */
    private static float normalizeMoisture(float downfall) {
        // Minecraft range: 0.0 to 1.0
        // Normalize to -1.0 to 1.0
        return downfall * 2.0f - 1.0f;
    }

    /**
     * Extracts all tags from a biome holder.
     *
     * @param holder the biome holder
     * @return set of tags
     */
    private static Set<TagKey<Biome>> extractTags(Holder<Biome> holder) {
        Set<TagKey<Biome>> tags = new HashSet<>();

        // Get all tags this biome belongs to
        holder.tags().forEach(tags::add);

        return tags;
    }
}
