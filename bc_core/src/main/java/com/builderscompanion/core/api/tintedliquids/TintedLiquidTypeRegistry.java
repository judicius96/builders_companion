package com.builderscompanion.core.api.tintedliquids;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface TintedLiquidTypeRegistry {
    int register(ResourceLocation key, int rgb);
    int getId(ResourceLocation key);
    @Nullable ResourceLocation getKey(int id);
    int getColorRgb(int id);
    int getColorRgb(ResourceLocation key);
    Collection<ResourceLocation> keys();
}
