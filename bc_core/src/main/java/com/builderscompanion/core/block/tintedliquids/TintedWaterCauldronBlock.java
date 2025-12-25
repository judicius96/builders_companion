package com.builderscompanion.core.block.tintedliquids;

import com.builderscompanion.core.blockentity.tintedliquids.TintedCauldronBlockEntity;
import com.builderscompanion.core.item.tintedliquids.TintedWaterBucketItem;
import com.builderscompanion.core.registry.tintedliquids.TintedIdRanges;
import com.builderscompanion.core.registry.tintedliquids.TintedLiquidsItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class TintedWaterCauldronBlock extends BaseEntityBlock {

    // Vanilla uses 0..3 for cauldron fill level
    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL_CAULDRON;

    public TintedWaterCauldronBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, 1));
    }

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext context
    ) {
        return Blocks.CAULDRON.defaultBlockState().getShape(level, pos, context);
    }

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext context
    ) {
        return Blocks.CAULDRON.defaultBlockState().getCollisionShape(level, pos, context);
    }

    /*@Override
    public boolean isSolidRender(BlockState state) {
        return false;
    }*/

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TintedCauldronBlockEntity(pos, state);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(LEVEL); // 0..3
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);

        // --- Fill with tinted bucket ---
        /*if (held.getItem() instanceof TintedWaterBucketItem) {
            int typeId = TintedWaterBucketItem.getTypeId(held);
            if (!isValidTypeId(typeId)) return InteractionResult.FAIL;

            BlockEntity be0 = level.getBlockEntity(pos);
            if (!(be0 instanceof TintedCauldronBlockEntity be)) return InteractionResult.FAIL;

            int curLevel = state.getValue(LEVEL);
            int stored = be.getTypeId();

            // v1 rule: only fill if empty or same type
            if (stored == -1) be.setTypeId(typeId);
            else if (stored != typeId) return InteractionResult.FAIL;

            if (curLevel >= 3) return InteractionResult.sidedSuccess(level.isClientSide);

            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(LEVEL, curLevel + 1), 3);
                if (!player.getAbilities().instabuild) {
                    player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                }

                player.awardStat(Stats.USE_CAULDRON);
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }*/

        // --- Drain into empty bucket ---
        if (held.is(Items.BUCKET)) {
            BlockEntity be0 = level.getBlockEntity(pos);

            // If this isn't our BE, we didn't handle it
            if (!(be0 instanceof TintedCauldronBlockEntity be)) {
                return InteractionResult.PASS;
            }

            int stored = be.getTypeId();
            int curLevel = state.getValue(LEVEL);

            // Our cauldron, but not drainable -> block vanilla bucket logic
            if (!isValidTypeId(stored) || curLevel != 3) {
                return InteractionResult.FAIL;
            }

            if (!level.isClientSide) {
                ItemStack out = new ItemStack(TintedLiquidsItems.TINTED_WATER_BUCKET.get());
                TintedWaterBucketItem.setTypeId(out, stored);

                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }

                if (!player.getInventory().add(out)) {
                    player.drop(out, false);
                }

                // Drain fully, revert to vanilla cauldron
                level.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), 3);

                player.awardStat(Stats.USE_CAULDRON);
                level.playSound(
                        null,
                        pos,
                        SoundEvents.BUCKET_FILL,
                        SoundSource.BLOCKS,
                        1.0F,
                        1.0F
                );
                level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    private static boolean isValidTypeId(int typeId) {
        return typeId >= 0 && typeId < TintedIdRanges.MAX_TYPE_ID;
    }

    // No ticker needed for MVP; return null
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.MODEL;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}
