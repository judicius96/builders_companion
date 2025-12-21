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

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

import javax.annotation.Nullable;

/**
 * Fired when a portal is activated (ignited).
 *
 * <p>This event is posted when a player or other trigger attempts to activate
 * a BC:Dimensions portal. Listeners can cancel the activation or modify the
 * target dimension.
 *
 * <p><b>When is this fired?</b> This event is posted on the Forge event bus when:
 * <ul>
 *   <li>A player uses the configured ignition item on a portal frame</li>
 *   <li>A redstone signal activates a portal (if configured)</li>
 *   <li>A portal is activated programmatically via the API</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * @SubscribeEvent
 * public static void onPortalActivated(PortalActivatedEvent event) {
 *     Player player = event.getPlayer();
 *     ResourceKey<Level> target = event.getTargetDimension();
 *
 *     // Prevent portal activation if player doesn't have permission
 *     if (player != null && !hasPermission(player, target)) {
 *         event.setCanceled(true);
 *         player.sendSystemMessage(Component.literal("You don't have permission!"));
 *         return;
 *     }
 *
 *     // Play custom sound
 *     Level level = event.getLevel();
 *     level.playSound(null, event.getPos(), MySounds.PORTAL_ACTIVATE.get(),
 *         SoundSource.BLOCKS, 1.0F, 1.0F);
 * }
 * }</pre>
 *
 * <p><b>Event Bus:</b> This event is posted on {@link net.minecraftforge.common.MinecraftForge#EVENT_BUS}.
 *
 * <p><b>Cancellable:</b> Yes. Cancelling this event will prevent the portal from
 * activating. The ignition item will not be consumed.
 *
 * @since 1.0.0
 */
@Cancelable
public class PortalActivatedEvent extends Event {

    private final BlockPos pos;
    private final Level level;
    private ResourceKey<Level> targetDimension;  // Mutable
    private final Player player;

    /**
     * Creates a new portal activated event.
     *
     * @param pos the portal frame position
     * @param level the level containing the portal
     * @param targetDimension the dimension this portal leads to
     * @param player the player who activated the portal, or null if activated by other means
     */
    public PortalActivatedEvent(BlockPos pos, Level level, ResourceKey<Level> targetDimension,
                               @Nullable Player player) {
        this.pos = pos;
        this.level = level;
        this.targetDimension = targetDimension;
        this.player = player;
    }

    /**
     * Gets the portal frame position.
     *
     * <p>This is typically the position of the bottom-center block of the portal frame.
     *
     * @return the portal position
     */
    public BlockPos getPos() {
        return pos;
    }

    /**
     * Gets the level containing the portal.
     *
     * @return the level
     */
    public Level getLevel() {
        return level;
    }

    /**
     * Gets the target dimension this portal leads to.
     *
     * <p>This can be modified by event listeners to change the portal's destination.
     *
     * @return the target dimension resource key
     */
    public ResourceKey<Level> getTargetDimension() {
        return targetDimension;
    }

    /**
     * Sets the target dimension this portal leads to.
     *
     * <p>Use this to dynamically change portal destinations based on conditions.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * @SubscribeEvent
     * public static void onPortalActivated(PortalActivatedEvent event) {
     *     // Redirect portals during nighttime to a nightmare dimension
     *     if (event.getLevel().isNight()) {
     *         ResourceKey<Level> nightmareDim = ResourceKey.create(
     *             Registry.DIMENSION_REGISTRY,
     *             new ResourceLocation("mymod", "nightmare")
     *         );
     *         event.setTargetDimension(nightmareDim);
     *     }
     * }
     * }</pre>
     *
     * @param targetDimension the new target dimension
     */
    public void setTargetDimension(ResourceKey<Level> targetDimension) {
        this.targetDimension = targetDimension;
    }

    /**
     * Gets the player who activated the portal.
     *
     * <p>This may be null if the portal was activated by redstone, a command,
     * or other non-player means.
     *
     * @return the player, or null
     */
    @Nullable
    public Player getPlayer() {
        return player;
    }

    /**
     * Checks if this portal was activated by a player.
     *
     * <p>This is a convenience method equivalent to {@code getPlayer() != null}.
     *
     * @return true if activated by a player, false otherwise
     */
    public boolean hasPlayer() {
        return player != null;
    }
}
