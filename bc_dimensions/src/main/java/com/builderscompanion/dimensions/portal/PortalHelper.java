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
package com.builderscompanion.dimensions.portal;

import com.builderscompanion.core.api.BCDimensionsAPI;
import com.builderscompanion.core.api.IPortalProvider;
import com.builderscompanion.core.config.DimensionConfig;
import com.builderscompanion.core.events.BCEvents;
import com.builderscompanion.core.util.BCLogger;
import com.builderscompanion.dimensions.dimension.DimensionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

/**
 * Helper utilities for portal creation, activation, and usage.
 *
 * <p>This class provides static methods for:
 * <ul>
 *   <li>Creating portal frames and lighting them</li>
 *   <li>Detecting which dimension a portal leads to</li>
 *   <li>Activating portals when ignited</li>
 *   <li>Teleporting entities through portals</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * // Light a portal
 * boolean lit = PortalHelper.lightPortal(level, pos, player);
 *
 * // Teleport through portal
 * PortalHelper.teleportThroughPortal(entity, level, portalPos);
 * }</pre>
 *
 * @since 1.0.0
 */
public class PortalHelper {

    /**
     * Attempts to light a portal at the given position.
     *
     * <p>This method:
     * <ol>
     *   <li>Searches for a valid portal frame</li>
     *   <li>Determines which dimension the portal leads to</li>
     *   <li>Validates the portal can be activated</li>
     *   <li>Fires PortalActivatedEvent (can be cancelled)</li>
     *   <li>Fills the frame with portal blocks</li>
     * </ol>
     *
     * @param level the level containing the portal
     * @param pos the position where ignition was attempted
     * @param player the player who ignited the portal, or null
     * @return true if portal was lit successfully
     */
    public static boolean lightPortal(Level level, BlockPos pos, @Nullable Player player) {
        if (level.isClientSide) {
            return false;
        }

        BCLogger.debug("Attempting to light portal at {}", pos);

        // Find which dimension this portal leads to by checking frame block
        Optional<PortalTarget> target = findPortalTarget(level, pos);

        if (target.isEmpty()) {
            BCLogger.debug("No matching dimension found for portal frame at {}", pos);
            return false;
        }

        PortalTarget portalTarget = target.get();
        PortalFrameDetector.PortalFrame frame = portalTarget.frame;
        ResourceKey<Level> targetDim = portalTarget.dimension;
        DimensionConfig config = portalTarget.config;

        BCLogger.debug("Found portal target: {} (frame: {}x{})",
                targetDim.location(), frame.width, frame.height);

        // Fire portal activated event
        BCEvents.PortalActivationResult result = BCEvents.postPortalActivated(
                pos, level, targetDim, player
        );

        if (!result.isAllowed()) {
            BCLogger.debug("Portal activation cancelled by event");
            return false;
        }

        // Event may have changed the target
        ResourceKey<Level> finalTarget = result.getTargetDimension();

        // Fill the frame with portal blocks
        fillPortalFrame(level, frame);

        // Play sound
        level.playSound(null, pos, SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 1.0F, 1.0F);

        BCLogger.info("Activated portal at {} leading to {}", pos, finalTarget.location());

        return true;
    }

    /**
     * Finds the target dimension for a portal frame.
     *
     * <p>This searches for a portal frame at the position and determines which
     * dimension it leads to by matching the frame block to dimension configs.
     *
     * @param level the level
     * @param pos the position
     * @return the portal target, or empty if no valid frame found
     */
    private static Optional<PortalTarget> findPortalTarget(Level level, BlockPos pos) {
        // Get all registered dimensions
        Map<ResourceKey<Level>, DimensionConfig> dimensions = DimensionRegistry.getAllDimensions();

        // Try each dimension's frame block
        for (Map.Entry<ResourceKey<Level>, DimensionConfig> entry : dimensions.entrySet()) {
            ResourceKey<Level> dimKey = entry.getKey();
            DimensionConfig config = entry.getValue();

            // Get frame block from config
            Block frameBlock = getBlockFromId(config.portal.frameBlock);
            if (frameBlock == null) {
                BCLogger.warn("Invalid frame block for dimension {}: {}",
                        dimKey.location(), config.portal.frameBlock);
                continue;
            }

            // Try to detect frame with this block
            Optional<PortalFrameDetector.PortalFrame> frame =
                    PortalFrameDetector.detectFrame(level, pos, frameBlock);

            if (frame.isPresent()) {
                return Optional.of(new PortalTarget(dimKey, config, frame.get()));
            }
        }

        return Optional.empty();
    }

    /**
     * Fills a portal frame with portal blocks.
     *
     * @param level the level
     * @param frame the portal frame
     */
    private static void fillPortalFrame(Level level, PortalFrameDetector.PortalFrame frame) {
        Direction horizontal = frame.axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;

        // Fill the interior with portal blocks
        for (int x = 1; x < frame.width - 1; x++) {
            for (int y = 1; y < frame.height - 1; y++) {
                BlockPos portalPos = frame.corner
                        .relative(horizontal, x)
                        .above(y);

                // Place nether portal block (we'll use this as a placeholder)
                // TODO: Create custom portal block per dimension
                level.setBlock(portalPos,
                        Blocks.NETHER_PORTAL.defaultBlockState(),
                        3);
            }
        }
    }

    /**
     * Teleports an entity through a portal.
     *
     * <p>This handles the actual teleportation including:
     * <ul>
     *   <li>Checking for IPortalProvider override</li>
     *   <li>Calculating destination with coordinate offset</li>
     *   <li>Finding or creating destination portal</li>
     *   <li>Performing the teleportation</li>
     * </ul>
     *
     * @param entity the entity to teleport
     * @param level the source level
     * @param portalPos the portal position
     * @return true if teleportation succeeded
     */
    public static boolean teleportThroughPortal(Entity entity, Level level, BlockPos portalPos) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        if (!entity.canChangeDimensions()) {
            return false;
        }

        // Find which dimension this portal leads to
        Optional<PortalTarget> target = findPortalTarget(level, portalPos);

        if (target.isEmpty()) {
            BCLogger.warn("Cannot teleport: no valid portal found at {}", portalPos);
            return false;
        }

        PortalTarget portalTarget = target.get();
        ResourceKey<Level> targetDim = portalTarget.dimension;
        DimensionConfig config = portalTarget.config;

        // Check if a portal provider should handle this
        IPortalProvider provider = BCDimensionsAPI.getPortalProvider();
        if (provider != null && provider.shouldHandlePortal(portalPos, level)) {
            BCLogger.debug("Portal provider handling teleportation for {}",
                    entity.getName().getString());
            provider.teleportEntity(entity, targetDim);
            return true;
        }

        // Default BC:Dimensions teleportation
        ServerLevel targetLevel = serverLevel.getServer().getLevel(targetDim);

        if (targetLevel == null) {
            BCLogger.error("Target dimension not found: {}", targetDim.location());
            return false;
        }

        BCLogger.debug("Teleporting {} to {}", entity.getName().getString(), targetDim.location());

        // Create teleporter and perform teleportation
        BCTeleporter teleporter = new BCTeleporter(targetLevel, config);
        entity.changeDimension(targetLevel, teleporter);

        return true;
    }

    /**
     * Gets a block from a resource location string.
     *
     * @param blockId the block ID (e.g., "minecraft:obsidian")
     * @return the block, or null if invalid
     */
    @Nullable
    private static Block getBlockFromId(String blockId) {
        try {
            ResourceLocation id = new ResourceLocation(blockId);
            return net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(id);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Represents a portal's target information.
     */
    private static class PortalTarget {
        public final ResourceKey<Level> dimension;
        public final DimensionConfig config;
        public final PortalFrameDetector.PortalFrame frame;

        public PortalTarget(ResourceKey<Level> dimension, DimensionConfig config,
                           PortalFrameDetector.PortalFrame frame) {
            this.dimension = dimension;
            this.config = config;
            this.frame = frame;
        }
    }
}
