package com.builderscompanion.biomewaters;

import com.builderscompanion.core.events.tintedliquids.TintedBucketEvents;
import com.builderscompanion.core.util.BCLogger;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(BCBiomeWaters.MODID)
public class BCBiomeWaters {
    public static final String MODID = "bc_biome_waters";

    public BCBiomeWaters() {
        BCLogger.info("Builders Companion: Biome Waters initializing...");

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        MinecraftForge.EVENT_BUS.register(TintedBucketEvents.class);
    }

}
