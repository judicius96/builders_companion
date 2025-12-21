package com.builderscompanion.bdadditions.worldgen;

import com.builderscompanion.bdadditions.BCAdditions;
import com.builderscompanion.bdadditions.worldgen.feature.RoseQuartzGeodeFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModWorldgen {
    private ModWorldgen() {}

    // Forge registry
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(ForgeRegistries.FEATURES, BCAdditions.MODID);

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> ROSE_QUARTZ_GEODE_FEATURE =
            FEATURES.register("rose_quartz_geode",
                    () -> new RoseQuartzGeodeFeature(NoneFeatureConfiguration.CODEC));
}
