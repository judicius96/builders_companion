package com.builderscompanion.dyedwaters.events;

import com.builderscompanion.core.item.tintedliquids.TintedWaterBucketItem;
import com.builderscompanion.core.registry.tintedliquids.TintedIdRanges;
import com.builderscompanion.core.registry.tintedliquids.TintedLiquidsItems;
import com.builderscompanion.core.registry.tintedliquids.TintedLiquidsRegistry;
import com.builderscompanion.core.util.BCLogger;
import com.builderscompanion.dyedwaters.registry.DyedColorRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Handles crafting recipes for dyed water buckets.
 * Recipe: Water Bucket + Dye = Dyed Water Bucket
 */
public class DyedBucketEvents {

    /**
     * Also handle picking up already-placed dyed water blocks
     */
    @SubscribeEvent
    public static void onBucketFill(net.minecraftforge.event.entity.player.FillBucketEvent event) {
        // Only handle our tinted fluids being picked up
        ItemStack emptyBucket = event.getEmptyBucket();
        if (!emptyBucket.is(Items.BUCKET)) {
            return;
        }

        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getTarget() instanceof net.minecraft.world.phys.BlockHitResult hit)) {
            return;
        }

        net.minecraft.core.BlockPos pos = hit.getBlockPos();
        net.minecraft.world.level.material.FluidState fluidState = event.getLevel().getFluidState(pos);

        if (!fluidState.isSource()) {
            return;
        }

        // Check if it's one of our dyed fluids
        Fluid fluid = fluidState.getType();
        int typeId = -1;

        for (int i = 0; i < TintedLiquidsRegistry.STILL_FLUIDS.length; i++) {
            Fluid still = TintedLiquidsRegistry.STILL_FLUIDS[i];
            Fluid flowing = TintedLiquidsRegistry.FLOWING_FLUIDS[i];
            if (still == null || flowing == null) continue;

            if (fluid == still || fluid == flowing) {
                // If typeId is in dye range, it's a dyed water
                if (i >= TintedIdRanges.DYE_START && i < TintedIdRanges.DYE_END_EXCLUSIVE) {
                    typeId = i;
                    break;
                }
            }
        }

        // Not a dyed fluid, let other handlers deal with it
        if (typeId == -1) {
            return;
        }

        // Remove the source block
        net.minecraft.world.level.block.state.BlockState state = event.getLevel().getBlockState(pos);
        if (state.getBlock() instanceof net.minecraft.world.level.block.BucketPickup pickup) {
            pickup.pickupBlock(event.getLevel(), pos, state);
        } else {
            return;
        }

        // Create dyed bucket
        ItemStack dyedBucket = new ItemStack(TintedLiquidsItems.TINTED_WATER_BUCKET.get());
        TintedWaterBucketItem.setTypeId(dyedBucket, typeId);

        event.setFilledBucket(dyedBucket);
        event.setResult(net.minecraftforge.eventbus.api.Event.Result.ALLOW);

        net.minecraft.world.entity.player.Player player = event.getEntity();
        if (player != null) {
            player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        }

        BCLogger.info("DyedBucketEvents: Picked up dyed water typeId={} at {}", typeId, pos);
    }



    private static boolean areSameDye(ItemStack dye1, ItemStack dye2) {
        return dye1.getItem() == dye2.getItem();
    }

    private static int getBaseTypeIdForDye(ItemStack dyeStack) {
        String dyeId = ForgeRegistries.ITEMS.getKey(dyeStack.getItem()).toString();
        Integer rgb = DyedColorRegistry.getDyeColor(dyeId);
        if (rgb == null) return -1;

        for (DyedColorRegistry.ColorEntry entry : DyedColorRegistry.getUniqueColors()) {
            if (entry.rgb == rgb && "dyed".equals(entry.category)) {
                return entry.typeId;
            }
        }
        return -1;
    }
}
