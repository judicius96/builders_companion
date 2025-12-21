package com.builderscompanion.core.datagen;

import com.builderscompanion.core.BCCore;
import com.builderscompanion.core.registry.WaterColorRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.List;
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
        // Add ALL tinted variants to minecraft:water so the solver treats them as one family.
        var water = this.tag(FluidTags.WATER);

        List<WaterColorRegistry.ColorEntry> colors = WaterColorRegistry.getUniqueColors();
        for (WaterColorRegistry.ColorEntry entry : colors) {
            int typeId = entry.typeId;
            String idSuffix = String.format("%03d", typeId);

            water.addOptional(new ResourceLocation(BCCore.MODID, "tinted_water_still_" + idSuffix));
            water.addOptional(new ResourceLocation(BCCore.MODID, "tinted_water_flowing_" + idSuffix));
        }
    }
}
