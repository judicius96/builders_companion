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
import com.builderscompanion.dimensions.dimension.DimensionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;

/**
 * Handles portal activation when players use ignition items on portal frames.
 *
 * <p>This event handler listens for right-click events and checks if:
 * <ul>
 *   <li>The player is using a configured ignition item</li>
 *   <li>The clicked block is part of a valid portal frame</li>
 *   <li>The portal can be activated</li>
 * </ul>
 *
 * <p>If all conditions are met, the portal is lit using {@link PortalHelper}.
 *
 * @since 1.0.0
 */
public class PortalEventHandler {

    /**
     * Handles right-click events on blocks.
     *
     * <p>This checks if the player is trying to light a portal with the
     * configured ignition item.
     *
     * @param event the player interact event
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        InteractionHand hand = event.getHand();

        // Only process main hand to avoid double activation
        if (hand != InteractionHand.MAIN_HAND) {
            return;
        }

        // Get the item the player is holding
        ItemStack heldItem = player.getItemInHand(hand);

        if (heldItem.isEmpty()) {
            return;
        }

        // Get the clicked block
        BlockState clickedBlock = level.getBlockState(pos);

        // Check if this could be a portal frame for any dimension
        if (isPortalActivation(heldItem, clickedBlock)) {
            BCLogger.debug("Player {} attempting portal activation at {}",
                    player.getName().getString(), pos);

            // Try to light the portal
            boolean lit = PortalHelper.lightPortal(level, pos, player);

            if (lit) {
                // Cancel the event to prevent other interactions
                event.setCanceled(true);
            }
        }
    }

    /**
     * Checks if the player is attempting to activate a portal.
     *
     * <p>This matches the held item against ignition items configured for
     * all registered dimensions.
     *
     * @param heldItem the item the player is holding
     * @param clickedBlock the block that was clicked
     * @return true if this could be a portal activation attempt
     */
    private static boolean isPortalActivation(ItemStack heldItem, BlockState clickedBlock) {
        // Get all registered dimensions
        Map<ResourceKey<Level>, DimensionConfig> dimensions =
                DimensionRegistry.getAllDimensions();

        for (DimensionConfig config : dimensions.values()) {
            // Check if held item matches ignition item
            Item ignitionItem = getItemFromId(config.portal.ignitionItem);

            if (ignitionItem != null && heldItem.is(ignitionItem)) {
                // Check if clicked block matches frame block
                net.minecraft.world.level.block.Block frameBlock =
                        getBlockFromId(config.portal.frameBlock);

                if (frameBlock != null && clickedBlock.is(frameBlock)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Gets an item from a resource location string.
     *
     * @param itemId the item ID (e.g., "minecraft:flint_and_steel")
     * @return the item, or null if invalid
     */
    private static Item getItemFromId(String itemId) {
        try {
            ResourceLocation id = new ResourceLocation(itemId);
            return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets a block from a resource location string.
     *
     * @param blockId the block ID (e.g., "minecraft:obsidian")
     * @return the block, or null if invalid
     */
    private static net.minecraft.world.level.block.Block getBlockFromId(String blockId) {
        try {
            ResourceLocation id = new ResourceLocation(blockId);
            return net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(id);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Handles entity collision with portal blocks.
     *
     * <p>This is called when an entity enters a portal block and initiates
     * the teleportation process.
     *
     * @param event the entity travel to dimension event
     */
    // TODO: Add EntityTravelToDimensionEvent handler for portal collisions
    // This would detect when entity enters portal block and call PortalHelper.teleportThroughPortal()
}
