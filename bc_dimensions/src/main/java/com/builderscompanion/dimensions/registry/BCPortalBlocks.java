/*
 * MIT License
 *
 * Copyright (c) 2025 Builders Companion
 */
package com.builderscompanion.dimensions.registry;

import com.builderscompanion.core.config.DimensionConfig;
import com.builderscompanion.core.util.BCLogger;
import com.builderscompanion.dimensions.BCDimensions;
import com.builderscompanion.dimensions.portal.CustomPortalBlock;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for custom portal blocks.
 *
 * <p>Each dimension gets its own portal block type.
 *
 * @since 1.0.0
 */
public class BCPortalBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, BCDimensions.MODID);

    private static final Map<String, RegistryObject<Block>> PORTAL_BLOCKS = new HashMap<>();

    /**
     * Registers a portal block for a dimension.
     *
     * @param config the dimension configuration
     * @param dimensionKey the dimension's ResourceKey
     * @return the registered portal block
     */
    public static RegistryObject<Block> registerPortalBlock(DimensionConfig config, ResourceKey<Level> dimensionKey) {
        String registryName = config.id + "_portal";

        RegistryObject<Block> portalBlock = BLOCKS.register(registryName,
                () -> new CustomPortalBlock(config, dimensionKey));

        PORTAL_BLOCKS.put(config.id, portalBlock);

        BCLogger.debug("Registered portal block: {}", registryName);
        return portalBlock;
    }

    /**
     * Gets the portal block for a dimension.
     *
     * @param dimensionId the dimension ID
     * @return the portal block, or null if not registered
     */
    public static Block getPortalBlock(String dimensionId) {
        RegistryObject<Block> block = PORTAL_BLOCKS.get(dimensionId);
        return block != null ? block.get() : null;
    }
}