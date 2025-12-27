package com.builderscompanion.biomewaters.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColors;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = "biome_waters", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BiomeWatersClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemColors itemColors = Minecraft.getInstance().getItemColors();

            com.builderscompanion.core.registry.tintedliquids.TintedLiquidsItems.TINTED_WATER_BUCKET.get();
        });
    }

}
