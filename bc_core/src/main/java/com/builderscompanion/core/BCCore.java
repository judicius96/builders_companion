package com.builderscompanion.core;

import com.builderscompanion.core.client.FluidRenderSetup;
import com.builderscompanion.core.registry.WaterColorRegistry;
import com.builderscompanion.core.registry.tintedliquids.TintedLiquidsItems;
import com.builderscompanion.core.registry.tintedliquids.TintedLiquidsRegistry;
import com.builderscompanion.core.tintedliquids.TintedLiquids;
import com.builderscompanion.core.util.BCLogger;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
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

        //Load water color registry
        BCLogger.info("Loading water color registry...");
        WaterColorRegistry.loadFromClasspath();

        // Register all fluid variants
        TintedLiquidsRegistry.registerAll();

        // Register event listeners
        BCLogger.info("BCCore: registering TintedBucketEvents");
        MinecraftForge.EVENT_BUS.register(com.builderscompanion.core.events.tintedliquids.TintedBucketEvents.class);
        BCLogger.info("BCCore: registered TintedBucketEvents");

        // Setup events
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::onLoadComplete);

        // Fluid Registry
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(FluidRenderSetup::clientSetup);
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BCLogger.info("Builders Companion Core - Common Setup");

            // Initialize TintedLiquids system
            TintedLiquids.initialize();

            BCLogger.info("BC-Core initialization complete");
        });
    }
    @SubscribeEvent
    public void onLoadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            BCLogger.info("Load complete, populating fluid arrays...");
            TintedLiquidsRegistry.populateArrays();
        });
    }

}