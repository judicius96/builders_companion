package com.builderscompanion.biomewaters.events;

import com.builderscompanion.biomewaters.TintedLiquids;
import com.builderscompanion.core.registry.tintedliquids.TintedLiquidsItems;
import com.builderscompanion.core.registry.tintedliquids.TintedLiquidsRegistry;
import com.builderscompanion.core.item.tintedliquids.TintedWaterBucketItem;
import com.builderscompanion.core.util.BCLogger;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class TintedBucketEvents {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBucketFill(FillBucketEvent event) {
        BCLogger.info("TintedBucketEvents: FillBucketEvent fired. emptyBucket={}, target={}",
                event.getEmptyBucket(), event.getTarget());

        // Only handle empty vanilla buckets
        ItemStack emptyBucket = event.getEmptyBucket();
        if (!emptyBucket.is(Items.BUCKET)) {
            BCLogger.info("TintedBucketEvents: ignoring because emptyBucket is not vanilla BUCKET");
            return;
        }

        Level level = event.getLevel();
        if (level.isClientSide()) {
            BCLogger.info("TintedBucketEvents: ignoring on client");
            return;
        }

        if (!(event.getTarget() instanceof BlockHitResult hit)) {
            BCLogger.info("TintedBucketEvents: ignoring because target is not BlockHitResult");
            return;
        }

        BlockPos pos = hit.getBlockPos();
        FluidState fluidState = level.getFluidState(pos);

        // Only source blocks
        if (!fluidState.isSource()) {
            BCLogger.info("TintedBucketEvents: ignoring because fluid is not source at {}", pos);
            return;
        }

        int typeId = -1;

        // CASE A: Our tinted water
        Fluid fluid = fluidState.getType();
        for (int i = 0; i < TintedLiquidsRegistry.STILL_FLUIDS.length; i++) {
            Fluid still = TintedLiquidsRegistry.STILL_FLUIDS[i];
            Fluid flowing = TintedLiquidsRegistry.FLOWING_FLUIDS[i];
            if (still == null || flowing == null) continue;

            if (fluid == still || fluid == flowing) {
                typeId = i;
                break;
            }
        }

        // CASE B: Vanilla water -> resolve from biome
        if (typeId == -1) {
            if (!fluidState.is(net.minecraft.world.level.material.Fluids.WATER)) {
                BCLogger.info("TintedBucketEvents: ignoring because fluid is not vanilla water at {}", pos);
                return;
            }

            typeId = TintedLiquids.resolveWaterType(level, pos);
            BCLogger.info("TintedBucketEvents: resolved biome water typeId={} at {}", typeId, pos);

            if (typeId == TintedLiquids.defaultWaterTypeId()) {
                return; // let vanilla bucket fill happen
            }
        }

        if (typeId < 0 || typeId >= 256) {
            BCLogger.warn("TintedBucketEvents: invalid typeId={} at {}", typeId, pos);
            return;
        }


        // Remove the source block like vanilla does
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BucketPickup pickup) {
            ItemStack removed = pickup.pickupBlock(level, pos, state);
            BCLogger.info("TintedBucketEvents: pickupBlock returned {}", removed);
        } else {
            BCLogger.warn("TintedBucketEvents: block at {} is not BucketPickup: {}", pos, state.getBlock());
            // If this happens for vanilla water, something is very wrong with world/block state.
            return;
        }

        // Create tinted bucket with typeId
        ItemStack tintedBucket = new ItemStack(TintedLiquidsItems.TINTED_WATER_BUCKET.get());
        TintedWaterBucketItem.setTypeId(tintedBucket, typeId);

        // Tell Forge we handled it
        event.setFilledBucket(tintedBucket);
        event.setResult(Event.Result.ALLOW);

        Player player = event.getEntity();
        if (player != null) {
            player.swing(InteractionHand.MAIN_HAND, true);
        }

        BCLogger.info("TintedBucketEvents: SUCCESS at {} -> typeId={} returning {}",
                pos, typeId, tintedBucket);
    }
}
