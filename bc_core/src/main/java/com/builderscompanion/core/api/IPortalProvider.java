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
package com.builderscompanion.core.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Interface for mods that want to override BC:Dimensions portal system.
 *
 * <p>This interface allows mods like BC:Portals to provide advanced portal features
 * such as custom shapes, multi-block portals, or enhanced linking systems.
 *
 * <p><b>How it works:</b>
 * <ol>
 *   <li>BC:Dimensions detects a portal activation</li>
 *   <li>Calls {@link #shouldHandlePortal} on the registered provider</li>
 *   <li>If true, BC:Dimensions defers to {@link #teleportEntity}</li>
 *   <li>If false, BC:Dimensions uses its default portal logic</li>
 * </ol>
 *
 * <p><b>Example Implementation:</b>
 * <pre>{@code
 * public class MyPortalProvider implements IPortalProvider {
 *
 *     @Override
 *     public boolean shouldHandlePortal(BlockPos pos, Level level) {
 *         // Check if this is a special multi-block portal
 *         return isMultiBlockPortal(pos, level);
 *     }
 *
 *     @Override
 *     public void teleportEntity(Entity entity, ResourceKey<Level> targetDim) {
 *         // Custom teleportation logic
 *         ServerLevel targetLevel = getServerLevel(targetDim);
 *         BlockPos destination = findPortalDestination(entity, targetLevel);
 *         entity.changeDimension(targetLevel, new MyTeleporter(destination));
 *     }
 * }
 *
 * // Register during mod initialization
 * BCDimensionsAPI.registerPortalProvider(new MyPortalProvider());
 * }</pre>
 *
 * <p><b>Registration:</b> Use {@link BCDimensionsAPI#registerPortalProvider(IPortalProvider)}
 * during FMLCommonSetupEvent. Only one provider can be registered at a time.
 *
 * <p><b>Thread Safety:</b> Implementations must be thread-safe as these methods
 * may be called from chunk generation threads.
 *
 * @since 1.0.0
 * @see BCDimensionsAPI#registerPortalProvider(IPortalProvider)
 */
public interface IPortalProvider {

    /**
     * Checks if this provider should handle the portal at the given position.
     *
     * <p>BC:Dimensions will call this before attempting to handle a portal.
     * If this returns true, BC:Dimensions will defer to this provider's
     * {@link #teleportEntity} method.
     *
     * <p><b>Performance Note:</b> This method should be fast as it may be called
     * frequently. Cache results if expensive calculations are needed.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * @Override
     * public boolean shouldHandlePortal(BlockPos pos, Level level) {
     *     // Check if this is a custom portal shape
     *     Block centerBlock = level.getBlockState(pos).getBlock();
     *     return centerBlock == MyBlocks.CUSTOM_PORTAL.get();
     * }
     * }</pre>
     *
     * @param pos the portal frame position
     * @param level the level containing the portal
     * @return true if this provider handles this portal, false otherwise
     */
    boolean shouldHandlePortal(BlockPos pos, Level level);

    /**
     * Teleports an entity through the portal.
     *
     * <p>This is only called if {@link #shouldHandlePortal} returned true.
     * The implementation is responsible for:
     * <ul>
     *   <li>Finding or creating the destination portal</li>
     *   <li>Calculating the destination coordinates</li>
     *   <li>Performing the actual teleportation</li>
     *   <li>Playing sounds and particles</li>
     * </ul>
     *
     * <p><b>Important:</b> This method is called on the server side only.
     * Always check {@code level.isClientSide} if necessary.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * @Override
     * public void teleportEntity(Entity entity, ResourceKey<Level> targetDim) {
     *     if (entity.level.isClientSide) {
     *         return; // Should never happen, but be safe
     *     }
     *
     *     ServerLevel targetLevel = entity.getServer().getLevel(targetDim);
     *     if (targetLevel == null) {
     *         return;
     *     }
     *
     *     // Find or create portal at destination
     *     BlockPos destination = findOrCreatePortal(targetLevel, entity.blockPosition());
     *
     *     // Teleport
     *     entity.changeDimension(targetLevel, new MyTeleporter(destination));
     *
     *     // Play sound
     *     entity.playSound(SoundEvents.PORTAL_TRAVEL, 1.0F, 1.0F);
     * }
     * }</pre>
     *
     * @param entity the entity to teleport
     * @param targetDim the target dimension resource key
     */
    void teleportEntity(Entity entity, ResourceKey<Level> targetDim);

    /**
     * Called when a portal is activated (ignited).
     *
     * <p>This is optional and provides a hook for portal providers to perform
     * custom logic when a portal is first activated.
     *
     * <p>Default implementation does nothing.
     *
     * @param pos the portal frame position
     * @param level the level containing the portal
     * @param targetDim the dimension this portal leads to
     */
    default void onPortalActivated(BlockPos pos, Level level, ResourceKey<Level> targetDim) {
        // Default: no-op
    }

    /**
     * Called when a portal is deactivated (broken).
     *
     * <p>This is optional and provides a hook for portal providers to clean up
     * resources when a portal is destroyed.
     *
     * <p>Default implementation does nothing.
     *
     * @param pos the portal frame position
     * @param level the level containing the portal
     */
    default void onPortalDeactivated(BlockPos pos, Level level) {
        // Default: no-op
    }
}
