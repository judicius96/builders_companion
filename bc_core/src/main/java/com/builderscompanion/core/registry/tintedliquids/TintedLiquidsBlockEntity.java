package com.builderscompanion.core.registry.tintedliquids;

import com.builderscompanion.core.BCCore;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TintedLiquidsBlockEntity {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, BCCore.MODID);

    public static final RegistryObject<BlockEntityType<com.builderscompanion.core.blockentity.tintedliquids.TintedCauldronBlockEntity>> TINTED_CAULDRON =
            BLOCK_ENTITIES.register("tinted_cauldron",
                    () -> BlockEntityType.Builder.of(
                            com.builderscompanion.core.blockentity.tintedliquids.TintedCauldronBlockEntity::new,
                            TintedLiquidsRegistry.TINTED_WATER_CAULDRON.get()
                    ).build(null));

}
