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
package com.builderscompanion.core.events.dimensions;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired before a biome is placed in a chunk during world generation.
 *
 * <p>Listeners can modify which biome is placed by changing the biome field.
 * This allows mods like BC:Fluids to swap biomes based on water color or other criteria.
 *
 * <p><b>When is this fired?</b> This event is posted on the Forge event bus during
 * chunk generation, for each biome position in the chunk. It fires AFTER BC:Dimensions
 * has selected a biome but BEFORE it's actually placed.
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * @SubscribeEvent
 * public static void onBiomePlacement(BiomePlacementEvent event) {
 *     Holder<Biome> current = event.getBiome();
 *
 *     // Swap ocean biomes with warm ocean based on water color
 *     if (current.is(Biomes.OCEAN)) {
 *         int waterColor = getWaterColor(event.getBiomeX(), event.getBiomeZ());
 *         if (waterColor == 0xFF6B00) { // Warm color
 *             event.setBiome(getBiomeHolder(Biomes.WARM_OCEAN));
 *         }
 *     }
 * }
 * }</pre>
 *
 * <p><b>Performance Note:</b> This event fires very frequently (multiple times per
 * chunk during generation). Keep event handlers fast:
 * <ul>
 *   <li>Return early if no modification is needed</li>
 *   <li>Avoid expensive calculations</li>
 *   <li>Cache results when possible</li>
 * </ul>
 *
 * <p><b>Event Bus:</b> This event is posted on {@link net.minecraftforge.common.MinecraftForge#EVENT_BUS}.
 *
 * <p><b>Cancellable:</b> Yes. Cancelling this event will prevent the biome from
 * being placed and may result in air/void at that position. Only cancel if you know
 * what you're doing.
 *
 * @since 1.0.0
 */
@Cancelable
public class BiomePlacementEvent extends Event {

    private final ChunkPos chunkPos;
    private final int biomeX;
    private final int biomeY;
    private final int biomeZ;
    private Holder<Biome> biome;  // Mutable
    private final ResourceKey<Level> dimension;

    /**
     * Creates a new biome placement event.
     *
     * @param chunkPos the chunk position being generated
     * @param biomeX X coordinate in biome space (not block space)
     * @param biomeY Y coordinate in biome space (not block space)
     * @param biomeZ Z coordinate in biome space (not block space)
     * @param biome the biome that will be placed
     * @param dimension the dimension being generated
     */
    public BiomePlacementEvent(ChunkPos chunkPos, int biomeX, int biomeY, int biomeZ,
                              Holder<Biome> biome, ResourceKey<Level> dimension) {
        this.chunkPos = chunkPos;
        this.biomeX = biomeX;
        this.biomeY = biomeY;
        this.biomeZ = biomeZ;
        this.biome = biome;
        this.dimension = dimension;
    }

    /**
     * Gets the chunk position being generated.
     *
     * @return the chunk position
     */
    public ChunkPos getChunkPos() {
        return chunkPos;
    }

    /**
     * Gets the X coordinate in biome space.
     *
     * <p><b>Important:</b> Biome coordinates are not the same as block coordinates.
     * Biomes are stored in 4x4x4 block sections. To get block coordinates:
     * <pre>{@code
     * int blockX = event.getBiomeX() << 2; // Multiply by 4
     * }</pre>
     *
     * @return biome X coordinate
     */
    public int getBiomeX() {
        return biomeX;
    }

    /**
     * Gets the Y coordinate in biome space.
     *
     * @return biome Y coordinate
     * @see #getBiomeX() for coordinate conversion notes
     */
    public int getBiomeY() {
        return biomeY;
    }

    /**
     * Gets the Z coordinate in biome space.
     *
     * @return biome Z coordinate
     * @see #getBiomeX() for coordinate conversion notes
     */
    public int getBiomeZ() {
        return biomeZ;
    }

    /**
     * Gets the biome that will be placed.
     *
     * <p>This may have been modified by previous event listeners.
     *
     * @return the biome holder
     */
    public Holder<Biome> getBiome() {
        return biome;
    }

    /**
     * Sets the biome that will be placed.
     *
     * <p>Use this to swap the biome to a different one.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * // Swap plains with forest
     * if (event.getBiome().is(Biomes.PLAINS)) {
     *     event.setBiome(getBiomeHolder(Biomes.FOREST));
     * }
     * }</pre>
     *
     * @param biome the new biome to place
     */
    public void setBiome(Holder<Biome> biome) {
        this.biome = biome;
    }

    /**
     * Gets the dimension being generated.
     *
     * @return the dimension resource key
     */
    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    /**
     * Converts biome X coordinate to block X coordinate.
     *
     * <p>This is a convenience method equivalent to {@code getBiomeX() << 2}.
     *
     * @return block X coordinate
     */
    public int getBlockX() {
        return biomeX << 2;
    }

    /**
     * Converts biome Y coordinate to block Y coordinate.
     *
     * <p>This is a convenience method equivalent to {@code getBiomeY() << 2}.
     *
     * @return block Y coordinate
     */
    public int getBlockY() {
        return biomeY << 2;
    }

    /**
     * Converts biome Z coordinate to block Z coordinate.
     *
     * <p>This is a convenience method equivalent to {@code getBiomeZ() << 2}.
     *
     * @return block Z coordinate
     */
    public int getBlockZ() {
        return biomeZ << 2;
    }
}
