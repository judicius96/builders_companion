package com.builderscompanion.biomewaters;

import com.builderscompanion.biomewaters.registry.WaterColorRegistry;
import com.builderscompanion.core.api.TintedLiquidProviderRegistry;
import com.builderscompanion.core.registry.tintedliquids.TintedLiquidsRegistry;
import com.builderscompanion.core.util.BCLogger;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.ArrayList;
import java.util.List;

@Mod(BCBiomeWaters.MODID)
public class BCBiomeWaters {
    public static final String MODID = "biome_waters";

    public BCBiomeWaters() {
        BCLogger.info("Biome Waters initializing...");

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Load biome color data
        WaterColorRegistry.loadFromClasspath();

        // IMPORTANT: register colors BEFORE registry events finish
        List<WaterColorRegistry.ColorEntry> colors = WaterColorRegistry.getUniqueColors();
        List<TintedLiquidsRegistry.ColorRegistration> registrations = new ArrayList<>();
        for (WaterColorRegistry.ColorEntry entry : colors) {
            registrations.add(new TintedLiquidsRegistry.ColorRegistration(
                    entry.typeId,
                    entry.rgb,
                    entry.category
            ));
        }

        BCLogger.info("Biome Waters: Registering {} colors with BC Core", registrations.size());
        TintedLiquidsRegistry.registerColors(registrations);

        // Register provider with BC Core
        TintedLiquidProviderRegistry.register(new BiomeWaterProvider());

        // Register bucket fill events
        MinecraftForge.EVENT_BUS.register(com.builderscompanion.biomewaters.events.TintedBucketEvents.class);

        // Setup lifecycle events (safe here)
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::onLoadComplete);

        BCLogger.info("Biome Waters initialized");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BCLogger.info("Biome Waters - Common Setup");

            // Safe: non-registry initialization only
            TintedLiquids.initialize();

            BCLogger.info("Biome Waters common setup complete");
        });
    }

    private void onLoadComplete(final FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            BCLogger.info("Biome Waters: Load complete, populating fluid arrays...");
            TintedLiquidsRegistry.populateArrays();
        });
    }
}
