package com.builderscompanion.core.registry.tintedliquids;

import com.builderscompanion.core.BCCore;
import com.builderscompanion.core.item.tintedliquids.TintedWaterBucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TintedLiquidsItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BCCore.MODID);

    public static final RegistryObject<Item> TINTED_WATER_BUCKET =
            ITEMS.register("tinted_water_bucket",
                    () -> new TintedWaterBucketItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .craftRemainder(Items.BUCKET)
                    ));

    private TintedLiquidsItems() {}
}