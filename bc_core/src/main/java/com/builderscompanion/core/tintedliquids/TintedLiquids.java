package com.builderscompanion.core.tintedliquids;

import com.builderscompanion.core.registry.WaterColorRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves biome -> water color typeId
 */
public final class TintedLiquids {

    private TintedLiquids() {}

    // Cache rgb(24-bit) -> typeId
    private static final Map<Integer, Integer> RGB_TO_TYPE = new HashMap<>();
    private static boolean cacheBuilt = false;

    private static void ensureCache() {
        if (cacheBuilt) return;

        for (WaterColorRegistry.ColorEntry entry : WaterColorRegistry.getUniqueColors()) {
            // Normalize to 24-bit rgb just in case
            int rgb24 = entry.rgb & 0xFFFFFF;
            RGB_TO_TYPE.put(rgb24, entry.typeId);
        }

        cacheBuilt = true;
    }

    /**
     * Resolve water type from biome at position.
     * Returns typeId for the water color, or 0 if not found (default/vanilla).
     */
    public static int resolveWaterType(Level level, BlockPos pos) {
        ensureCache();

        Optional<ResourceKey<Biome>> biomeKeyOpt = level.getBiome(pos).unwrapKey();
        if (biomeKeyOpt.isEmpty()) return 0;

        String biomeId = biomeKeyOpt.get().location().toString();

        Integer color = WaterColorRegistry.getBiomeColor(biomeId);
        if (color == null) return 0;

        int rgb24 = color & 0xFFFFFF;

        Integer typeId = RGB_TO_TYPE.get(rgb24);
        return (typeId != null) ? typeId : 0;
    }

    /**
     * Default water typeId (vanilla color)
     */
    public static int defaultWaterTypeId() {
        return 0;
    }

    /**
     * Initialization (build caches)
     */
    public static void initialize() {
        ensureCache();
    }
}
