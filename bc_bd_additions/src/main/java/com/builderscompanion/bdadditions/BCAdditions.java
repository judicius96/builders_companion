package com.builderscompanion.bdadditions;

import com.builderscompanion.bdadditions.boprosequartz.BuddingRoseQuartzBlock;
import com.builderscompanion.bdadditions.worldgen.ModWorldgen;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

/**
 * Builders Companion: Additions
 *
 * Adds additional content that didn't fit elsewhere.
 *
 * Current Content:
 * - Rose Quartz Geode (Biomes O' Plenty integration)
 *
 * @version 1.0.0
 */
@Mod(BCAdditions.MODID)
public class BCAdditions {

    public static final String MODID = "bc_bd_additions";
    public static final Logger LOGGER = LogUtils.getLogger();

    // Deferred Registers
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    // Blocks
    public static final RegistryObject<Block> BUDDING_ROSE_QUARTZ = BLOCKS.register(
            "budding_rose_quartz",
            () -> new BuddingRoseQuartzBlock(
                    BlockBehaviour.Properties.of()
                            .strength(1.5F)
                            .sound(SoundType.AMETHYST)
                            .requiresCorrectToolForDrops()
                            .randomTicks()
                            .lightLevel(state -> 10)
            )
    );

    // Block Items
    public static final RegistryObject<Item> BUDDING_ROSE_QUARTZ_ITEM = ITEMS.register(
            "budding_rose_quartz",
            () -> new BlockItem(BUDDING_ROSE_QUARTZ.get(), new Item.Properties())
    );

    public BCAdditions() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);

        ModWorldgen.FEATURES.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        LOGGER.info("Builders Companion: Builders Delight Additions initializing...");
    }


    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            com.builderscompanion.bdadditions.boprosequartz.BopRoseQuartz.resolveOrSkip();

            if (com.builderscompanion.bdadditions.boprosequartz.BopRoseQuartz.ready()) {
                LOGGER.info("RoseQuartz integration ready: BoP buds/clusters resolved.");
            } else {
                LOGGER.info("RoseQuartz integration not ready: feature will remain inert.");
            }
        });

        LOGGER.info("BC BD Additions: Common setup scheduled");
    }
}