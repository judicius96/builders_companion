package com.builderscompanion.core.client;

import com.builderscompanion.core.BCCore;
import com.builderscompanion.core.registry.tintedliquids.TintedLiquidsRegistry;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = BCCore.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FluidRenderSetup {

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (Fluid fluid : ForgeRegistries.FLUIDS.getValues()) {
                ResourceLocation key = ForgeRegistries.FLUIDS.getKey(fluid);
                if (key == null) continue;

                // Only your mod's fluids; tighten the prefix if you want.
                if (BCCore.MODID.equals(key.getNamespace()) && key.getPath().startsWith("tinted_water_")) {
                    ItemBlockRenderTypes.setRenderLayer(fluid, RenderType.translucent());
                }
            }
        });
    }
}
