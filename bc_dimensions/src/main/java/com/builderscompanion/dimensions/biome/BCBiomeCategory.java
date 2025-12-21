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

import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;

/**
 * BC's biome classification system.
 *
 * <p>This enum replaces Minecraft's removed BiomeCategory system with a
 * tag-based classification. Biomes are categorized based on their tags
 * (BiomeTags.IS_FOREST, BiomeTags.IS_OCEAN, etc.).
 *
 * <p><b>Why We Need This:</b>
 * <p>Minecraft 1.20.1 removed the BiomeCategory enum. We recreate it using
 * biome tags for:
 * <ul>
 *   <li>Biome pool filtering (e.g., "only forest biomes")</li>
 *   <li>Debug logging and reports</li>
 *   <li>Future BC:Dimensions features</li>
 * </ul>
 *
 * <p><b>Classification Logic:</b>
 * <p>Categories are assigned in priority order (first match wins):
 * <ol>
 *   <li>OCEAN - Has IS_OCEAN tag</li>
 *   <li>RIVER - Has IS_RIVER tag</li>
 *   <li>BEACH - Has IS_BEACH tag</li>
 *   <li>MOUNTAIN - Has IS_MOUNTAIN tag</li>
 *   <li>FOREST - Has IS_FOREST tag</li>
 *   <li>JUNGLE - Has IS_JUNGLE tag</li>
 *   <li>TAIGA - Has IS_TAIGA tag</li>
 *   <li>SAVANNA - Has IS_SAVANNA tag</li>
 *   <li>BADLANDS - Has IS_BADLANDS tag</li>
 *   <li>NETHER - Has IS_NETHER tag</li>
 *   <li>END - Has IS_END tag</li>
 *   <li>UNDERGROUND - Temperature < 0.5 and no other tags</li>
 *   <li>PLAINS - Default fallback</li>
 * </ol>
 *
 * @since 1.0.0
 */
public enum BCBiomeCategory {
    /** Ocean biomes (deep ocean, warm ocean, etc.) */
    OCEAN,

    /** River biomes */
    RIVER,

    /** Beach and shore biomes */
    BEACH,

    /** Mountain and hill biomes */
    MOUNTAIN,

    /** Forest biomes (oak forest, birch forest, etc.) */
    FOREST,

    /** Jungle biomes */
    JUNGLE,

    /** Taiga biomes (pine forests, snowy taiga, etc.) */
    TAIGA,

    /** Savanna biomes */
    SAVANNA,

    /** Desert biomes */
    DESERT,

    /** Badlands/Mesa biomes */
    BADLANDS,

    /** Nether biomes */
    NETHER,

    /** End biomes */
    END,

    /** Underground/cave biomes */
    UNDERGROUND,

    /** Plains and other flat biomes (default) */
    PLAINS;

    /**
     * Determines the category for a biome based on its tags.
     *
     * <p>This checks biome tags in priority order and returns the first match.
     * If no tags match, returns PLAINS as default.
     *
     * @param biomeHolder the biome holder
     * @return the biome category
     */
    public static BCBiomeCategory fromBiome(Holder<Biome> biomeHolder) {
        // Check tags in priority order
        if (biomeHolder.is(BiomeTags.IS_OCEAN)) {
            return OCEAN;
        }
        if (biomeHolder.is(BiomeTags.IS_RIVER)) {
            return RIVER;
        }
        if (biomeHolder.is(BiomeTags.IS_BEACH)) {
            return BEACH;
        }
        if (biomeHolder.is(BiomeTags.IS_MOUNTAIN)) {
            return MOUNTAIN;
        }
        if (biomeHolder.is(BiomeTags.IS_FOREST)) {
            return FOREST;
        }
        if (biomeHolder.is(BiomeTags.IS_JUNGLE)) {
            return JUNGLE;
        }
        if (biomeHolder.is(BiomeTags.IS_TAIGA)) {
            return TAIGA;
        }
        if (biomeHolder.is(BiomeTags.IS_SAVANNA)) {
            return SAVANNA;
        }
        if (biomeHolder.is(BiomeTags.IS_BADLANDS)) {
            return BADLANDS;
        }
        if (biomeHolder.is(BiomeTags.IS_NETHER)) {
            return NETHER;
        }
        if (biomeHolder.is(BiomeTags.IS_END)) {
            return END;
        }

        // Check for desert by temperature (hot + dry)
        Biome biome = biomeHolder.value();
        float temp = biome.getBaseTemperature();
        float moisture = biome.getModifiedClimateSettings().downfall();

        if (temp > 1.0f && moisture < 0.2f) {
            return DESERT;
        }

        // Check for underground (cold + no surface tags)
        if (temp < 0.5f) {
            return UNDERGROUND;
        }

        // Default to plains
        return PLAINS;
    }

    /**
     * Gets a human-readable name for this category.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return switch (this) {
            case OCEAN -> "Ocean";
            case RIVER -> "River";
            case BEACH -> "Beach";
            case MOUNTAIN -> "Mountain";
            case FOREST -> "Forest";
            case JUNGLE -> "Jungle";
            case TAIGA -> "Taiga";
            case SAVANNA -> "Savanna";
            case DESERT -> "Desert";
            case BADLANDS -> "Badlands";
            case NETHER -> "Nether";
            case END -> "End";
            case UNDERGROUND -> "Underground";
            case PLAINS -> "Plains";
        };
    }
}
