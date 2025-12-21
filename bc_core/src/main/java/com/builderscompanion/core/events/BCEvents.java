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
package com.builderscompanion.core.events;

import com.builderscompanion.core.config.DimensionConfig;
import com.builderscompanion.core.events.dimensions.BiomePlacementEvent;
import com.builderscompanion.core.events.dimensions.DimensionRegisteredEvent;
import com.builderscompanion.core.util.BCLogger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;

/**
 * Utility class for posting BC:Dimensions events.
 *
 * <p>This class provides convenient static methods for posting events on the Forge
 * event bus. It handles event creation and logging for consistency across the mod.
 *
 * <p><b>Event Types:</b>
 * <ul>
 *   <li>{@link DimensionRegisteredEvent} - Posted when a dimension is registered</li>
 *   <li>{@link BiomePlacementEvent} - Posted before a biome is placed in a chunk</li>
 *   <li>{@link PortalActivatedEvent} - Posted when a portal is activated</li>
 * </ul>
 *
 * <p><b>Example Usage (Internal):</b>
 * <pre>{@code
 * // Post dimension registered event
 * BCEvents.postDimensionRegistered(dimensionKey, config);
 *
 * // Post biome placement event (may be modified by listeners)
 * Holder<Biome> finalBiome = BCEvents.postBiomePlacement(
 *     chunkPos, x, y, z, selectedBiome, dimension
 * );
 *
 * // Post portal activated event (may be cancelled)
 * boolean allowed = BCEvents.postPortalActivated(pos, level, targetDim, player);
 * }</pre>
 *
 * <p><b>For Mod Developers:</b> To listen to these events, use {@code @SubscribeEvent}
 * on the {@link MinecraftForge#EVENT_BUS}:
 * <pre>{@code
 * @SubscribeEvent
 * public static void onDimensionRegistered(DimensionRegisteredEvent event) {
 *     // Your code here
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public class BCEvents {

    /**
     * Posts a dimension registered event.
     *
     * <p>This event is fired when a dimension is successfully registered with BC:Dimensions.
     * It's posted after validation but before the dimension is created.
     *
     * <p><b>Internal use only.</b> Called by BC:Dimensions during dimension registration.
     *
     * @param dimension the dimension that was registered
     * @param config the dimension configuration
     */
    public static void postDimensionRegistered(ResourceKey<Level> dimension, DimensionConfig config) {
        DimensionRegisteredEvent event = new DimensionRegisteredEvent(dimension, config);
        MinecraftForge.EVENT_BUS.post(event);

        BCLogger.debug("Posted DimensionRegisteredEvent for {}", dimension.location());
    }

    /**
     * Posts a biome placement event.
     *
     * <p>This event is fired before a biome is placed in a chunk during world generation.
     * Listeners can modify the biome or cancel the placement.
     *
     * <p><b>Returns:</b> The final biome to place (may be different from the input if
     * modified by event listeners), or null if the event was cancelled.
     *
     * <p><b>Internal use only.</b> Called by BC:Dimensions during chunk generation.
     *
     * @param chunkPos the chunk position being generated
     * @param biomeX X coordinate in biome space
     * @param biomeY Y coordinate in biome space
     * @param biomeZ Z coordinate in biome space
     * @param biome the biome that will be placed
     * @param dimension the dimension being generated
     * @return the final biome to place, or null if cancelled
     */
    @Nullable
    public static Holder<Biome> postBiomePlacement(ChunkPos chunkPos, int biomeX, int biomeY, int biomeZ,
                                                   Holder<Biome> biome, ResourceKey<Level> dimension) {
        BiomePlacementEvent event = new BiomePlacementEvent(
                chunkPos, biomeX, biomeY, biomeZ, biome, dimension
        );

        if (MinecraftForge.EVENT_BUS.post(event)) {
            // Event was cancelled
            BCLogger.debug("BiomePlacementEvent cancelled at ({}, {}, {}) in {}",
                    biomeX, biomeY, biomeZ, dimension.location());
            return null;
        }

        // Return the potentially modified biome
        Holder<Biome> finalBiome = event.getBiome();

        if (finalBiome != biome) {
            BCLogger.trace("Biome modified by event: {} -> {} at ({}, {}, {})",
                    biome.unwrapKey().map(k -> k.location().toString()).orElse("unknown"),
                    finalBiome.unwrapKey().map(k -> k.location().toString()).orElse("unknown"),
                    biomeX, biomeY, biomeZ);
        }

        return finalBiome;
    }

    /**
     * Posts a portal activated event.
     *
     * <p>This event is fired when a portal is activated (ignited). Listeners can
     * cancel the activation or modify the target dimension.
     *
     * <p><b>Returns:</b> The final portal activation result, including whether it was
     * allowed and what the target dimension is.
     *
     * <p><b>Internal use only.</b> Called by BC:Dimensions when a portal is activated.
     *
     * @param pos the portal frame position
     * @param level the level containing the portal
     * @param targetDimension the initial target dimension
     * @param player the player who activated the portal, or null
     * @return the portal activation result
     */
    public static PortalActivationResult postPortalActivated(BlockPos pos, Level level,
                                                             ResourceKey<Level> targetDimension,
                                                             @Nullable Player player) {
        PortalActivatedEvent event = new PortalActivatedEvent(pos, level, targetDimension, player);

        boolean cancelled = MinecraftForge.EVENT_BUS.post(event);

        if (cancelled) {
            BCLogger.debug("PortalActivatedEvent cancelled at {} in {}",
                    pos, level.dimension().location());
            return new PortalActivationResult(false, null);
        }

        ResourceKey<Level> finalTarget = event.getTargetDimension();

        if (!finalTarget.equals(targetDimension)) {
            BCLogger.debug("Portal target modified by event: {} -> {} at {}",
                    targetDimension.location(), finalTarget.location(), pos);
        }

        return new PortalActivationResult(true, finalTarget);
    }

    /**
     * Result of a portal activation event.
     *
     * <p>Contains information about whether the portal activation was allowed
     * and what the final target dimension is.
     */
    public static class PortalActivationResult {
        private final boolean allowed;
        private final ResourceKey<Level> targetDimension;

        /**
         * Creates a new portal activation result.
         *
         * @param allowed whether the activation was allowed
         * @param targetDimension the target dimension (may be null if not allowed)
         */
        public PortalActivationResult(boolean allowed, @Nullable ResourceKey<Level> targetDimension) {
            this.allowed = allowed;
            this.targetDimension = targetDimension;
        }

        /**
         * Checks if the portal activation was allowed.
         *
         * @return true if allowed, false if cancelled
         */
        public boolean isAllowed() {
            return allowed;
        }

        /**
         * Gets the final target dimension.
         *
         * <p>This may be different from the original target if modified by event listeners.
         *
         * @return the target dimension, or null if activation was not allowed
         */
        @Nullable
        public ResourceKey<Level> getTargetDimension() {
            return targetDimension;
        }
    }
}
