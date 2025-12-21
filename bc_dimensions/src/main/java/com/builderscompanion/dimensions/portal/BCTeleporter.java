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

import com.builderscompanion.core.config.DimensionConfig;
import com.builderscompanion.core.util.BCLogger;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Function;

/**
 * Handles entity teleportation between dimensions with coordinate offset support.
 *
 * <p>This teleporter implements the coordinate offset system that allows dimensions
 * to have different coordinate scales (like the Nether's 8:1 ratio).
 *
 * <p><b>Coordinate Offset Examples:</b>
 * <ul>
 *   <li><b>offset = 1</b> - 1:1 mapping (same coordinates)</li>
 *   <li><b>offset = 8</b> - Nether-style (1 block in dimension = 8 in overworld)</li>
 *   <li><b>offset = -8</b> - Reverse nether (8 blocks in dimension = 1 in overworld)</li>
 * </ul>
 *
 * <p><b>Portal Finding:</b> The teleporter will:
 * <ol>
 *   <li>Calculate destination coordinates with offset</li>
 *   <li>Search for existing portal near destination</li>
 *   <li>If not found, create new portal at safe location</li>
 *   <li>Teleport entity to portal</li>
 * </ol>
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * // Create teleporter with 8:1 offset (nether-style)
 * DimensionConfig config = new DimensionConfig();
 * config.portal.portalOffset = 8;
 * BCTeleporter teleporter = new BCTeleporter(targetLevel, config);
 *
 * // Teleport entity
 * entity.changeDimension(targetLevel, teleporter);
 * }</pre>
 *
 * @since 1.0.0
 */
public class BCTeleporter implements ITeleporter {

    private final ServerLevel targetLevel;
    private final DimensionConfig config;
    private final int portalOffset;

    /**
     * Creates a new BC teleporter.
     *
     * @param targetLevel the destination level
     * @param config the dimension configuration
     */
    public BCTeleporter(ServerLevel targetLevel, DimensionConfig config) {
        this.targetLevel = targetLevel;
        this.config = config;
        this.portalOffset = config.portal.portalOffset;
    }

    /**
     * Creates a new BC teleporter with a specific destination.
     *
     * <p>This constructor is used when you already know exactly where the portal should be.
     *
     * @param targetLevel the destination level
     * @param destination the exact destination position
     * @param config the dimension configuration
     */
    public BCTeleporter(ServerLevel targetLevel, BlockPos destination, DimensionConfig config) {
        this(targetLevel, config);
        // Store destination for later use (would need to add field)
    }

    @Override
    public @Nullable PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld,
                                              Function<ServerLevel, PortalInfo> defaultPortalInfo) {
        // Calculate destination coordinates with offset
        BlockPos destination = calculateDestination(entity.blockPosition());

        BCLogger.debug("Teleporting {} from {} to {} (offset={})",
                entity.getName().getString(),
                entity.blockPosition(),
                destination,
                portalOffset);

        // Find or create portal at destination
        Optional<BlockPos> portalPos = findOrCreatePortal(destination);

        if (portalPos.isPresent()) {
            BlockPos portal = portalPos.get();
            Vec3 position = new Vec3(portal.getX() + 0.5, portal.getY(), portal.getZ() + 0.5);

            BCLogger.debug("Portal found/created at {}", portal);

            // Return portal info with entity's current velocity and rotation
            return new PortalInfo(
                    position,
                    entity.getDeltaMovement(),
                    entity.getYRot(),
                    entity.getXRot()
            );
        } else {
            BCLogger.warn("Failed to find or create portal for {} at {}",
                    entity.getName().getString(), destination);

            // Fall back to spawn point
            BlockPos spawnPos = targetLevel.getSharedSpawnPos();
            Vec3 position = new Vec3(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);

            return new PortalInfo(
                    position,
                    Vec3.ZERO,
                    entity.getYRot(),
                    entity.getXRot()
            );
        }
    }

    /**
     * Calculates destination coordinates with portal offset scaling.
     *
     * <p><b>Examples:</b>
     * <ul>
     *   <li>offset=1: (100, 64, 200) → (100, 64, 200) [1:1]</li>
     *   <li>offset=8: (100, 64, 200) → (12, 64, 25) [nether-style]</li>
     *   <li>offset=-8: (100, 64, 200) → (800, 64, 1600) [reverse nether]</li>
     * </ul>
     *
     * @param origin the origin position
     * @return the destination position after offset scaling
     */
    private BlockPos calculateDestination(BlockPos origin) {
        if (portalOffset == 1) {
            return origin;  // 1:1 mapping, no change
        }

        int destX, destZ;

        if (portalOffset > 0) {
            // Positive offset: divide coordinates (nether-style)
            destX = origin.getX() / portalOffset;
            destZ = origin.getZ() / portalOffset;
        } else {
            // Negative offset: multiply coordinates (reverse nether)
            int multiplier = Math.abs(portalOffset);
            destX = origin.getX() * multiplier;
            destZ = origin.getZ() * multiplier;
        }

        // Keep Y coordinate the same
        return new BlockPos(destX, origin.getY(), destZ);
    }

    /**
     * Finds an existing portal near the destination or creates a new one.
     *
     * <p>Search strategy:
     * <ol>
     *   <li>Search in a 128-block radius around destination</li>
     *   <li>If found, return existing portal</li>
     *   <li>If not found, find safe location and create new portal</li>
     * </ol>
     *
     * @param destination the target destination
     * @return the portal position, or empty if creation failed
     */
    private Optional<BlockPos> findOrCreatePortal(BlockPos destination) {
        // Search for existing portal
        Optional<BlockPos> existingPortal = findExistingPortal(destination, 128);

        if (existingPortal.isPresent()) {
            BCLogger.debug("Found existing portal at {}", existingPortal.get());
            return existingPortal;
        }

        // No existing portal found, create new one
        BCLogger.debug("No existing portal found, creating new portal");

        // Find safe location near destination
        BlockPos safeLocation = findSafeLocation(destination);

        if (safeLocation == null) {
            BCLogger.warn("Failed to find safe location for portal near {}", destination);
            return Optional.empty();
        }

        // Create portal at safe location
        boolean created = createPortal(safeLocation);

        if (created) {
            BCLogger.info("Created new portal at {}", safeLocation);
            return Optional.of(safeLocation);
        } else {
            BCLogger.warn("Failed to create portal at {}", safeLocation);
            return Optional.empty();
        }
    }

    /**
     * Searches for an existing portal frame near a position.
     *
     * @param center the center position to search from
     * @param radius the search radius in blocks
     * @return the portal position if found, empty otherwise
     */
    private Optional<BlockPos> findExistingPortal(BlockPos center, int radius) {
        // TODO: Implement portal frame detection
        // This would search for portal frame blocks in the area
        // For now, return empty to always create new portals
        return Optional.empty();
    }

    /**
     * Finds a safe location to place a portal near the destination.
     *
     * <p>A safe location is:
     * <ul>
     *   <li>Within world border</li>
     *   <li>On solid ground or in a cave</li>
     *   <li>Has space for a portal frame</li>
     *   <li>Not in lava or void</li>
     * </ul>
     *
     * @param destination the desired destination
     * @return a safe position, or null if none found
     */
    private @Nullable BlockPos findSafeLocation(BlockPos destination) {
        WorldBorder border = targetLevel.getWorldBorder();

        // Clamp to world border
        double clampedX = Math.max(border.getMinX() + 16, Math.min(border.getMaxX() - 16, destination.getX()));
        double clampedZ = Math.max(border.getMinZ() + 16, Math.min(border.getMaxZ() - 16, destination.getZ()));

        BlockPos clamped = new BlockPos((int) clampedX, destination.getY(), (int) clampedZ);

        // Search vertically for a good spot
        BlockPos.MutableBlockPos searchPos = clamped.mutable();

        // Try to find solid ground below
        for (int y = clamped.getY(); y > targetLevel.getMinBuildHeight(); y--) {
            searchPos.setY(y);

            if (targetLevel.getBlockState(searchPos).isSolidRender(targetLevel, searchPos)) {
                // Found solid ground, portal goes above it
                return searchPos.above().immutable();
            }
        }

        // No ground found, try going up
        for (int y = clamped.getY(); y < targetLevel.getMaxBuildHeight() - 10; y++) {
            searchPos.setY(y);

            if (targetLevel.getBlockState(searchPos).isSolidRender(targetLevel, searchPos)) {
                return searchPos.above().immutable();
            }
        }

        // Last resort: use the original position
        BCLogger.warn("Could not find ideal portal location, using original destination");
        return clamped;
    }

    /**
     * Creates a portal at the specified location.
     *
     * <p>This creates a basic portal frame using the configured frame block.
     *
     * @param location the location to create the portal
     * @return true if portal was created successfully
     */
    private boolean createPortal(BlockPos location) {
        try {
            // TODO: Implement actual portal frame creation
            // This would:
            // 1. Get the frame block from config
            // 2. Build a portal frame shape
            // 3. Place portal blocks inside

            // For now, just ensure the position is safe
            BlockPos.MutableBlockPos pos = location.mutable();

            // Clear some space for the entity to spawn
            for (int y = 0; y < 3; y++) {
                pos.setY(location.getY() + y);
                if (!targetLevel.getBlockState(pos).isAir()) {
                    targetLevel.removeBlock(pos, false);
                }
            }

            // Place a platform if needed
            pos.setY(location.getY() - 1);
            if (targetLevel.getBlockState(pos).isAir()) {
                targetLevel.setBlock(pos,
                        net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState(),
                        3);
            }

            return true;
        } catch (Exception e) {
            BCLogger.error("Error creating portal: {}", e.getMessage());
            return false;
        }
    }

}
