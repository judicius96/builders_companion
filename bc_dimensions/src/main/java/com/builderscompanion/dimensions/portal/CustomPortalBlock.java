/*
 * MIT License
 *
 * Copyright (c) 2025 Builders Companion
 */
package com.builderscompanion.dimensions.portal;

import com.builderscompanion.core.config.DimensionConfig;
import com.builderscompanion.core.util.BCLogger;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Custom portal block for BC dimensions.
 *
 * <p>This is a placeholder implementation. Full portal logic will be added later.
 *
 * @since 1.0.0
 */
public class CustomPortalBlock extends Block {

    private final DimensionConfig config;
    private final ResourceKey<Level> targetDimension;

    public CustomPortalBlock(DimensionConfig config, ResourceKey<Level> targetDimension) {
        super(Properties.copy(Blocks.NETHER_PORTAL));
        this.config = config;
        this.targetDimension = targetDimension;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // TODO: Implement teleportation logic
        BCLogger.debug("Entity {} touched portal to {}", entity.getName().getString(), config.id);
    }

    /**
     * Gets the target dimension for this portal.
     *
     * @return the target dimension ResourceKey
     */
    public ResourceKey<Level> getTargetDimension() {
        return targetDimension;
    }

    /**
     * Gets the dimension configuration.
     *
     * @return the dimension config
     */
    public DimensionConfig getConfig() {
        return config;
    }
}