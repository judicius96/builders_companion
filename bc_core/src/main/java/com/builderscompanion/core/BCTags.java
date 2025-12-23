package com.builderscompanion.core;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

public class BCTags {
    public static class Fluids {
        public static final TagKey<Fluid> WATER_LIKE = tag("water_like");

        private static TagKey<Fluid> tag(String name) {
            return TagKey.create(Registries.FLUID, new ResourceLocation(BCCore.MODID, name));
        }
    }
}