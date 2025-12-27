package com.builderscompanion.bcadditions.boprosequartz;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

public class BuddingRoseQuartzBlock extends BuddingAmethystBlock {

    public BuddingRoseQuartzBlock(Properties props) {
        super(props);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Feature gate: if BoP buds are not resolved, do nothing.
        if (!BopRoseQuartz.ready()) return;

        // Match vanilla budding amethyst tick chance (1 in 5)
        if (random.nextInt(5) != 0) return;

        Direction dir = Direction.getRandom(random);
        BlockPos targetPos = pos.relative(dir);
        BlockState targetState = level.getBlockState(targetPos);

        Block next = null;

        Block smallBud = BopRoseQuartz.SMALL;
        Block mediumBud = BopRoseQuartz.MEDIUM;
        Block largeBud = BopRoseQuartz.LARGE;
        Block cluster = BopRoseQuartz.CLUSTER;

        if (canGrowNewBud(targetState)) {
            next = smallBud;
        } else if (targetState.is(smallBud) && hasFacing(targetState, dir)) {
            next = mediumBud;
        } else if (targetState.is(mediumBud) && hasFacing(targetState, dir)) {
            next = largeBud;
        } else if (targetState.is(largeBud) && hasFacing(targetState, dir)) {
            next = cluster;
        }

        if (next == null) return;

        boolean waterlogged = targetState.getFluidState().getType() == Fluids.WATER;

        BlockState newState = next.defaultBlockState()
                .setValue(AmethystClusterBlock.FACING, dir)
                .setValue(AmethystClusterBlock.WATERLOGGED, waterlogged);

        level.setBlock(targetPos, newState, Block.UPDATE_ALL);
    }

    private static boolean canGrowNewBud(BlockState state) {
        return state.isAir()
                || (state.is(Blocks.WATER) && state.getFluidState().getAmount() == 8);
    }

    private static boolean hasFacing(BlockState state, Direction direction) {
        return state.hasProperty(AmethystClusterBlock.FACING)
                && state.getValue(AmethystClusterBlock.FACING) == direction;
    }
}
