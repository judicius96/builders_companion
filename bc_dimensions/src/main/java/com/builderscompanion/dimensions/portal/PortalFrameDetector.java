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

import com.builderscompanion.core.util.BCLogger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Detects and validates portal frames.
 *
 * <p>A valid portal frame is a rectangular frame of a specific block, similar
 * to a nether portal:
 * <pre>
 * ████████
 * █      █
 * █      █
 * █      █
 * ████████
 * </pre>
 *
 * <p><b>Frame Requirements:</b>
 * <ul>
 *   <li>Minimum size: 3x3 (including frame)</li>
 *   <li>Maximum size: 23x23 (including frame)</li>
 *   <li>All frame blocks must be the same type</li>
 *   <li>Interior can be air or portal blocks</li>
 *   <li>Can be oriented on X or Z axis</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * Block frameBlock = Blocks.OBSIDIAN;
 * Optional<PortalFrame> frame = PortalFrameDetector.detectFrame(
 *     level, pos, frameBlock
 * );
 *
 * if (frame.isPresent()) {
 *     PortalFrame portal = frame.get();
 *     System.out.println("Found portal: " + portal.width + "x" + portal.height);
 *     System.out.println("Axis: " + portal.axis);
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public class PortalFrameDetector {

    /** Minimum portal size (including frame) */
    public static final int MIN_SIZE = 3;

    /** Maximum portal size (including frame) */
    public static final int MAX_SIZE = 23;

    /**
     * Detects a portal frame at a position.
     *
     * <p>This searches for a complete rectangular frame of the specified block.
     * The search starts at the given position and expands outward to find the
     * full frame dimensions.
     *
     * @param level the level to search in
     * @param pos the position to search from (can be any part of the frame)
     * @param frameBlock the block that makes up the frame
     * @return the detected frame, or empty if invalid
     */
    public static Optional<PortalFrame> detectFrame(Level level, BlockPos pos, Block frameBlock) {
        // Try both X and Z orientations
        Optional<PortalFrame> xAxisFrame = detectFrameOnAxis(level, pos, frameBlock, Direction.Axis.X);
        if (xAxisFrame.isPresent()) {
            return xAxisFrame;
        }

        Optional<PortalFrame> zAxisFrame = detectFrameOnAxis(level, pos, frameBlock, Direction.Axis.Z);
        if (zAxisFrame.isPresent()) {
            return zAxisFrame;
        }

        return Optional.empty();
    }

    /**
     * Detects a portal frame on a specific axis.
     *
     * @param level the level
     * @param pos the starting position
     * @param frameBlock the frame block
     * @param axis the axis (X or Z)
     * @return the detected frame, or empty if invalid
     */
    private static Optional<PortalFrame> detectFrameOnAxis(Level level, BlockPos pos,
                                                           Block frameBlock, Direction.Axis axis) {
        // Find the bottom-left corner of the frame
        BlockPos corner = findCorner(level, pos, frameBlock, axis);

        if (corner == null) {
            return Optional.empty();
        }

        // Measure frame dimensions
        int width = measureWidth(level, corner, frameBlock, axis);
        int height = measureHeight(level, corner, frameBlock);

        if (width < MIN_SIZE || width > MAX_SIZE || height < MIN_SIZE || height > MAX_SIZE) {
            return Optional.empty();
        }

        // Validate the complete frame
        if (!validateFrame(level, corner, width, height, frameBlock, axis)) {
            return Optional.empty();
        }

        // Create frame object
        PortalFrame frame = new PortalFrame(corner, width, height, axis, frameBlock);

        BCLogger.debug("Detected {}x{} portal frame at {} on {} axis",
                width, height, corner, axis);

        return Optional.of(frame);
    }

    /**
     * Finds the bottom-left corner of a portal frame.
     *
     * @param level the level
     * @param start the starting position
     * @param frameBlock the frame block
     * @param axis the portal axis
     * @return the corner position, or null if not found
     */
    private static @Nullable BlockPos findCorner(Level level, BlockPos start,
                                                 Block frameBlock, Direction.Axis axis) {
        BlockPos.MutableBlockPos pos = start.mutable();

        // Move down to find bottom
        int maxDown = MAX_SIZE;
        while (maxDown-- > 0) {
            if (!isFrameBlock(level, pos, frameBlock)) {
                pos.move(Direction.UP);
                break;
            }
            pos.move(Direction.DOWN);
        }

        // Move to left edge based on axis
        Direction leftDirection = axis == Direction.Axis.X ? Direction.WEST : Direction.NORTH;
        int maxLeft = MAX_SIZE;

        while (maxLeft-- > 0) {
            if (!isFrameBlock(level, pos, frameBlock)) {
                pos.move(leftDirection.getOpposite());
                break;
            }
            pos.move(leftDirection);
        }

        // Verify this is actually a corner
        if (isFrameBlock(level, pos, frameBlock)) {
            return pos.immutable();
        }

        return null;
    }

    /**
     * Measures the width of a portal frame.
     *
     * @param level the level
     * @param corner the bottom-left corner
     * @param frameBlock the frame block
     * @param axis the portal axis
     * @return the width in blocks
     */
    private static int measureWidth(Level level, BlockPos corner, Block frameBlock, Direction.Axis axis) {
        Direction direction = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        BlockPos.MutableBlockPos pos = corner.mutable();

        int width = 0;
        while (width < MAX_SIZE && isFrameBlock(level, pos, frameBlock)) {
            width++;
            pos.move(direction);
        }

        return width;
    }

    /**
     * Measures the height of a portal frame.
     *
     * @param level the level
     * @param corner the bottom-left corner
     * @param frameBlock the frame block
     * @return the height in blocks
     */
    private static int measureHeight(Level level, BlockPos corner, Block frameBlock) {
        BlockPos.MutableBlockPos pos = corner.mutable();

        int height = 0;
        while (height < MAX_SIZE && isFrameBlock(level, pos, frameBlock)) {
            height++;
            pos.move(Direction.UP);
        }

        return height;
    }

    /**
     * Validates that a complete frame exists with the given dimensions.
     *
     * @param level the level
     * @param corner the bottom-left corner
     * @param width the frame width
     * @param height the frame height
     * @param frameBlock the frame block
     * @param axis the portal axis
     * @return true if the frame is valid
     */
    private static boolean validateFrame(Level level, BlockPos corner, int width, int height,
                                         Block frameBlock, Direction.Axis axis) {
        Direction horizontal = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;

        // Check all four edges
        for (int i = 0; i < width; i++) {
            // Bottom edge
            BlockPos bottomPos = corner.relative(horizontal, i);
            if (!isFrameBlock(level, bottomPos, frameBlock)) {
                return false;
            }

            // Top edge
            BlockPos topPos = bottomPos.above(height - 1);
            if (!isFrameBlock(level, topPos, frameBlock)) {
                return false;
            }
        }

        for (int i = 0; i < height; i++) {
            // Left edge
            BlockPos leftPos = corner.above(i);
            if (!isFrameBlock(level, leftPos, frameBlock)) {
                return false;
            }

            // Right edge
            BlockPos rightPos = leftPos.relative(horizontal, width - 1);
            if (!isFrameBlock(level, rightPos, frameBlock)) {
                return false;
            }
        }

        // Interior can be air or portal blocks (we don't validate interior here)
        return true;
    }

    /**
     * Checks if a block is the frame block.
     *
     * @param level the level
     * @param pos the position
     * @param frameBlock the expected frame block
     * @return true if the block matches
     */
    private static boolean isFrameBlock(Level level, BlockPos pos, Block frameBlock) {
        BlockState state = level.getBlockState(pos);
        return state.is(frameBlock);
    }

    /**
     * Represents a detected portal frame.
     */
    public static class PortalFrame {
        /** Bottom-left corner of the frame */
        public final BlockPos corner;

        /** Width of the frame (including frame blocks) */
        public final int width;

        /** Height of the frame (including frame blocks) */
        public final int height;

        /** Portal orientation axis */
        public final Direction.Axis axis;

        /** The block making up the frame */
        public final Block frameBlock;

        public PortalFrame(BlockPos corner, int width, int height,
                          Direction.Axis axis, Block frameBlock) {
            this.corner = corner;
            this.width = width;
            this.height = height;
            this.axis = axis;
            this.frameBlock = frameBlock;
        }

        /**
         * Gets the interior width (excluding frame).
         *
         * @return interior width
         */
        public int getInteriorWidth() {
            return width - 2;
        }

        /**
         * Gets the interior height (excluding frame).
         *
         * @return interior height
         */
        public int getInteriorHeight() {
            return height - 2;
        }

        /**
         * Gets the center position of the portal.
         *
         * @return center position
         */
        public BlockPos getCenter() {
            Direction horizontal = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
            return corner
                    .relative(horizontal, width / 2)
                    .above(height / 2);
        }

        @Override
        public String toString() {
            return String.format("PortalFrame{%dx%d at %s, axis=%s}",
                    width, height, corner, axis);
        }
    }
}
