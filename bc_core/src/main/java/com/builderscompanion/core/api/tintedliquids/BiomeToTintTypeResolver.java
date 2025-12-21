package com.builderscompanion.core.api.tintedliquids;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

@FunctionalInterface
public interface BiomeToTintTypeResolver {
    /**
     * Return a typeId, or -1 if this resolver has no opinion.
     */
    int resolve(Level level, BlockPos pos, ResourceKey<Biome> biomeKey);
}
