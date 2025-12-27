package com.builderscompanion.core.client;

import com.builderscompanion.core.api.TintedLiquidProviderRegistry;
import com.builderscompanion.core.registry.tintedliquids.TintedLiquidsItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColors;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = "bc_core", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ItemColorHandler {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        System.out.println("[BC Core ItemColorHandler] FMLClientSetupEvent fired!");

        event.enqueueWork(() -> {
            System.out.println("[BC Core ItemColorHandler] Registering unified item color handler...");

            ItemColors itemColors = Minecraft.getInstance().getItemColors();

            itemColors.register((stack, tintIndex) -> {
                if (tintIndex != 1) return 0xFFFFFFFF;

                int typeId = stack.getDamageValue();
                return TintedLiquidProviderRegistry.getRgbColor(typeId);
            }, TintedLiquidsItems.TINTED_WATER_BUCKET.get());

            System.out.println("[BC Core ItemColorHandler] Unified handler registered!");
        });
    }
}