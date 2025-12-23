package com.builderscompanion.core.datagen;

import com.builderscompanion.core.BCCore;
import com.builderscompanion.core.registry.tintedliquids.TintedLiquidsRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
import net.minecraft.data.tags.TagsProvider.TagAppender;  // ← ADD THIS
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.Fluid;  // ← ADD THIS
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class BCTintedFluidTagsProvider extends FluidTagsProvider {

    public BCTintedFluidTagsProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            ExistingFileHelper existingFileHelper
    ) {
        super(output, lookupProvider, BCCore.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        TagAppender<Fluid> waterTag = tag(FluidTags.WATER);

        // Tag all registered tinted waters (0-255 range)
        for (int i = 0; i < 256; i++) {
            if (TintedLiquidsRegistry.STILL_FLUIDS[i] != null) {
                String idSuffix = String.format("%03d", i);
                waterTag.add(
                        ResourceKey.create(ForgeRegistries.FLUIDS.getRegistryKey(),
                                new ResourceLocation(BCCore.MODID, "tinted_water_still_" + idSuffix))
                );
                waterTag.add(
                        ResourceKey.create(ForgeRegistries.FLUIDS.getRegistryKey(),
                                new ResourceLocation(BCCore.MODID, "tinted_water_flowing_" + idSuffix))
                );
            }
        }
    }
}
