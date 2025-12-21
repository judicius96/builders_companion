/*
 * MIT License
 *
 * Copyright (c) 2025 Builders Companion
 */
package com.builderscompanion.dimensions;

import com.builderscompanion.core.config.BCConfigLoader;
import com.builderscompanion.core.config.DimensionConfig;
import com.builderscompanion.core.util.BCLogger;
import com.builderscompanion.dimensions.command.BCTeleportCommand;
import com.builderscompanion.dimensions.registry.BCBiomeSources;
import com.builderscompanion.dimensions.registry.BCDimensionRegistry;
import com.builderscompanion.dimensions.registry.BCPortalBlocks;
import com.builderscompanion.dimensions.worldgen.DimensionJsonGenerator;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.nio.file.Path;
import java.util.List;

/**
 * Main mod class for Builders Companion: Dimensions.
 *
 * <p>This mod provides:
 * <ul>
 *   <li>Custom dimension creation from YAML configs</li>
 *   <li>Three biome generation modes (region, climate_grid, layered)</li>
 *   <li>Custom portal system</li>
 *   <li>World border management</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Mod(BCDimensions.MODID)
public class BCDimensions {

    public static final String MODID = "bc_dimensions";

    public BCDimensions() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register blocks (must happen during construction)
        BCPortalBlocks.BLOCKS.register(modEventBus);

        // Register setup handler
        modEventBus.addListener(this::commonSetup);

        // Register commands
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);

        BCLogger.info("Builders Companion: Dimensions initializing...");
    }

    /**
     * Common setup - runs after registries are available.
     *
     * <p>This is where we:
     * <ul>
     *   <li>Load dimension configs</li>
     *   <li>Generate dimension JSON files</li>
     *   <li>Register dimensions</li>
     *   <li>Register portal blocks</li>
     * </ul>
     */
    private void commonSetup(final FMLCommonSetupEvent event) {
        BCLogger.info("BC:Dimensions common setup starting...");

        // Register biome sources FIRST
        event.enqueueWork(() -> {
            BCBiomeSources.register();
        });

        // Load all dimension configs
        List<DimensionConfig> dimensions = BCConfigLoader.getAllDimensions();

        if (dimensions.isEmpty()) {
            BCLogger.warn("No dimensions configured. Edit config/builderscompanion/dimensions.yml to add dimensions.");
            return;
        }
        try {
            //Crate namespace in BC:Core's datapack
            Path namespacePath = com.builderscompanion.core.datapack.DatapackGenerator.createNamespace(
                "bc_dimensions", "dimension", "dimension_type");

        // Process each dimension
        for (DimensionConfig config : dimensions) {
            try {
                // Register dimension with our registry
                ResourceKey<Level> dimensionKey = BCDimensionRegistry.registerDimension(config);

                // Register portal block for this dimension
                BCPortalBlocks.registerPortalBlock(config, dimensionKey);

                // Generate dimension JSON files
                DimensionJsonGenerator.generateDimensionFiles(config, config.id);

                BCLogger.info("Prepared dimension: {} ({})", config.displayName, config.id);

            } catch (Exception e) {
                BCLogger.error("Failed to prepare dimension {}: {}", config.id, e.getMessage());
                e.printStackTrace();
            }
        }

            BCLogger.info("BC:Dimensions setup complete. {} dimensions prepared.", dimensions.size());
            BCLogger.info("IMPORTANT: Datapack generated at config/builderscompanion/bc_datapack/");
            BCLogger.info("For EXISTING worlds: Copy this folder to your world's datapacks/ folder");
            BCLogger.info("For NEW worlds: Datapack will be detected automatically");

        } catch (Exception e) {
            BCLogger.error("Failed to generate dimension datapack: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Registers commands.
     */
    private void registerCommands(RegisterCommandsEvent event) {
        BCTeleportCommand.register(event.getDispatcher());
    }
}