package com.builderscompanion.core.datagen;

import com.builderscompanion.core.BCCore;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BCCore.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class BCDataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput output = gen.getPackOutput();
        ExistingFileHelper existing = event.getExistingFileHelper();

        gen.addProvider(event.includeServer(), new BCTintedFluidTagsProvider(output, event.getLookupProvider(), existing));
        // BCTintedLiquidClientAssets removed - now handled by child mods
    }
}