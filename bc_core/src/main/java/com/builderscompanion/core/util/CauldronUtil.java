package com.builderscompanion.core.util;

import com.builderscompanion.core.block.tintedliquids.TintedWaterCauldronBlock;
import com.builderscompanion.core.blockentity.tintedliquids.TintedCauldronBlockEntity;
import com.builderscompanion.core.item.tintedliquids.TintedWaterBucketItem;
import com.builderscompanion.core.registry.tintedliquids.TintedIdRanges;
import com.builderscompanion.core.registry.tintedliquids.TintedLiquidsItems;
import com.builderscompanion.core.registry.tintedliquids.TintedLiquidsRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public final class CauldronUtil {
    private CauldronUtil() {}

    public static void init() {
        CauldronInteraction.EMPTY.put(
                TintedLiquidsItems.TINTED_WATER_BUCKET.get(),
                CauldronUtil::fillFromTintedBucket
        );

        CauldronInteraction.WATER.put(
                TintedLiquidsItems.TINTED_WATER_BUCKET.get(),
                CauldronUtil::fillFromTintedBucket
        );
    }

    private static InteractionResult fillFromTintedBucket(
            BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, ItemStack stack
    ) {
        int typeId = TintedWaterBucketItem.getTypeId(stack);

        // Do NOT allow vanilla bucket fallback
        if (!isValidTypeId(typeId)) return InteractionResult.FAIL;

        if (!level.isClientSide) {
            player.setItemInHand(hand, BucketItem.getEmptySuccessItem(stack, player));
            player.awardStat(Stats.USE_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(stack.getItem()));

            BlockState placed = TintedLiquidsRegistry.TINTED_WATER_CAULDRON.get()
                    .defaultBlockState()
                    .setValue(TintedWaterCauldronBlock.LEVEL, 3);

            level.setBlock(pos, placed, 3);

            if (level.getBlockEntity(pos) instanceof TintedCauldronBlockEntity be) {
                be.setTypeId(typeId);
            }

            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean isValidTypeId(int typeId) {
        return typeId >= 0 && typeId < TintedIdRanges.MAX_TYPE_ID;
    }
}
