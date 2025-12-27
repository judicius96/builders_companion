package com.builderscompanion.core;

import com.builderscompanion.core.client.FluidRenderSetup;
import com.builderscompanion.core.registry.tintedliquids.TintedLiquidsBlockEntity;
import com.builderscompanion.core.registry.tintedliquids.TintedLiquidsItems;
import com.builderscompanion.core.registry.tintedliquids.TintedLiquidsRegistry;
import com.builderscompanion.core.util.BCLogger;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(BCCore.MODID)
public class BCCore {
    public static final String MODID = "bc_core";

    public BCCore() {
        BCLogger.info("Builders Companion Core initializing...");

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register deferred registers
        TintedLiquidsItems.ITEMS.register(modBus);
        TintedLiquidsRegistry.FLUID_TYPES.register(modBus);
        TintedLiquidsRegistry.FLUIDS.register(modBus);
        TintedLiquidsRegistry.BLOCKS.register(modBus);
        TintedLiquidsBlockEntity.BLOCK_ENTITIES.register(modBus);

        // Setup events
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::onLoadComplete);

        // Fluid Registry
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(FluidRenderSetup::clientSetup);
        }

        BCLogger.info("BC Core initialized");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BCLogger.info("Builders Companion Core - Common Setup");
            BCLogger.info("BC-Core initialization complete");
        });
    }

    private void onLoadComplete(final FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            BCLogger.info("BC Core: Load complete, populating fluid arrays...");
            TintedLiquidsRegistry.populateArrays();
        });
    }
}