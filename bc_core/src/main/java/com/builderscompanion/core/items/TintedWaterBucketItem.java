package com.builderscompanion.core.tintedliquids.item;

import com.builderscompanion.core.api.TintedLiquidProviderRegistry;
import com.builderscompanion.core.registry.tintedliquids.TintedLiquidsRegistry;
import com.builderscompanion.core.util.BCLogger;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;
import java.util.List;

public class TintedWaterBucketItem extends BucketItem {

    public TintedWaterBucketItem(Properties props) {
        super(Fluids.WATER, props);
    }

    @Override
    public Component getName(ItemStack stack) {
        int typeId = getTypeId(stack);
        if (typeId < 0 || typeId >= 256) {
            return Component.literal("Tinted Water Bucket");
        }

        // Delegate to provider
        Component name = TintedLiquidProviderRegistry.getDisplayName(typeId);
        return name != null ? name : Component.literal("Tinted Water Bucket");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        int typeId = getTypeId(stack);
        if (typeId < 0 || typeId >= 256) {
            super.appendHoverText(stack, level, tooltip, flag);
            return;
        }

        // Delegate to provider
        List<Component> lines = TintedLiquidProviderRegistry.getTooltipLines(typeId);
        tooltip.addAll(lines);

        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }

        int typeId = getTypeId(stack);
        if (typeId < 0 || typeId >= 256) {
            return InteractionResultHolder.fail(stack);
        }

        LiquidBlock liquidBlock = TintedLiquidsRegistry.LIQUID_BLOCKS[typeId];
        if (liquidBlock == null) {
            BCLogger.error("No liquid block registered for typeId={}", typeId);
            return InteractionResultHolder.fail(stack);
        }

        BlockPos clicked = hit.getBlockPos();
        BlockPos placePos;

        BlockState clickedState = level.getBlockState(clicked);
        if (clickedState.canBeReplaced()) {
            placePos = clicked;
        } else {
            placePos = clicked.relative(hit.getDirection());
        }

        if (!level.mayInteract(player, placePos)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!player.mayUseItemAt(placePos, hit.getDirection(), stack)) {
            return InteractionResultHolder.fail(stack);
        }

        BlockState targetState = level.getBlockState(placePos);
        if (!targetState.canBeReplaced()) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            BlockState placeState = liquidBlock.defaultBlockState();

            if (placeState.hasProperty(LiquidBlock.LEVEL)) {
                placeState = placeState.setValue(LiquidBlock.LEVEL, 0);
            }

            boolean placed = level.setBlock(placePos, placeState, 11);
            if (!placed) {
                return InteractionResultHolder.fail(stack);
            }

            if (!player.getAbilities().instabuild) {
                return InteractionResultHolder.success(new ItemStack(Items.BUCKET));
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static int getTypeId(ItemStack stack) {
        return stack.getDamageValue();
    }

    public static void setTypeId(ItemStack stack, int typeId) {
        stack.setDamageValue(typeId);
    }
}