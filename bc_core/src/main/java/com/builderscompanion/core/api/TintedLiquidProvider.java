package com.builderscompanion.core.api;

import net.minecraft.network.chat.Component;
import java.util.List;

/**
 * Provider interface for mods that add tinted liquids (biome waters, dyed waters, etc.)
 */
public interface TintedLiquidProvider {
    /**
     * Unique identifier for this provider
     */
    String getProviderId();

    /**
     * Get display name for a bucket with this typeId
     * @return Component for bucket name, or null if this provider doesn't handle this typeId
     */
    Component getDisplayName(int typeId);

    /**
     * Get tooltip lines for a bucket with this typeId
     * @return List of tooltip components, or empty list if none
     */
    List<Component> getTooltipLines(int typeId);

    /**
     * Check if this provider handles the given typeId
     */
    boolean handlesTypeId(int typeId);
}