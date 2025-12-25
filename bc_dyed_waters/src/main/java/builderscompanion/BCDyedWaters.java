package com.builderscompanion.dyedwaters;

import com.builderscompanion.core.api.TintedLiquidProviderRegistry;
import com.builderscompanion.core.registry.tintedliquids.TintedLiquidsRegistry;
import com.builderscompanion.core.util.BCLogger;
import com.builderscompanion.dyedwaters.registry.DyedColorRegistry;
import com.builderscompanion.dyedwaters.provider.DyedWaterProvider;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.ArrayList;
import java.util.List;

@Mod(BCDyedWaters.MODID)
public class BCDyedWaters {
    public static final String MODID = "dyed_waters";

    public BCDyedWaters() {
        BCLogger.info("Dyed Waters initializing...");

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Load dye color data
        DyedColorRegistry.loadFromClasspath();

        // Register colors with BC Core
        List<DyedColorRegistry.ColorEntry> colors = DyedColorRegistry.getUniqueColors();
        List<TintedLiquidsRegistry.ColorRegistration> registrations = new ArrayList<>();
        for (DyedColorRegistry.ColorEntry entry : colors) {
            registrations.add(new TintedLiquidsRegistry.ColorRegistration(
                    entry.typeId,
                    entry.rgb,
                    entry.category
            ));
        }

        BCLogger.info("Dyed Waters: Registering {} colors with BC Core", registrations.size());
        TintedLiquidsRegistry.registerColors(registrations);

        // Register provider with BC Core
        TintedLiquidProviderRegistry.register(new DyedWaterProvider());

        // Register bucket events
        MinecraftForge.EVENT_BUS.register(com.builderscompanion.dyedwaters.events.DyedBucketEvents.class);

        // Setup events
        modBus.addListener(this::commonSetup);

        BCLogger.info("Dyed Waters initialized");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BCLogger.info("Dyed Waters - Common Setup");
            BCLogger.info("Dyed Waters common setup complete");
        });
    }
}