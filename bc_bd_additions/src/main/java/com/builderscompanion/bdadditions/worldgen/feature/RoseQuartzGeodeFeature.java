package com.builderscompanion.bdadditions.worldgen.feature;

import com.builderscompanion.bdadditions.BCAdditions;
import com.builderscompanion.bdadditions.boprosequartz.BopRoseQuartz;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluids;

public class RoseQuartzGeodeFeature extends Feature<NoneFeatureConfiguration> {

    private static final TagKey<Block> STONE_REPLACEABLES =
            TagKey.create(Registries.BLOCK,
                    new ResourceLocation("minecraft", "stone_ore_replaceables"));

    private static final TagKey<Block> DEEPSLATE_REPLACEABLES =
            TagKey.create(Registries.BLOCK,
                    new ResourceLocation("minecraft", "deepslate_ore_replaceables"));

    private static final TagKey<Block> FORGE_ORES =
            TagKey.create(Registries.BLOCK, new ResourceLocation("forge", "ores"));

    public RoseQuartzGeodeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        BlockPos origin = ctx.origin();

        // --- Optional gate: "near lava aquifers" ---
        // Currently Disabled for further testing
        //if (!lavaNearby(level, origin, 10)) return false;

        int outerR = 5 + random.nextInt(3); // 5..7

        // One-band each (outer shell, inner shell, lining), hollow inside.
        // This matches the "looks like one layer each" expectation.
        int hollowR = outerR - 3;
        int liningR = hollowR + 1;
        int innerShellR = hollowR + 2;

        if (hollowR < 2) return false;

        int outerR2 = outerR * outerR;
        int hollowR2 = hollowR * hollowR;
        int liningR2 = liningR * liningR;
        int innerShellR2 = innerShellR * innerShellR;

        // Materials
        BlockState outerShell = Blocks.BLACKSTONE.defaultBlockState();
        BlockState innerShell = Blocks.NETHERRACK.defaultBlockState();

        BlockState lining = (BopRoseQuartz.ROSE_QUARTZ_BLOCK != Blocks.AIR
                ? BopRoseQuartz.ROSE_QUARTZ_BLOCK.defaultBlockState()
                : Blocks.AMETHYST_BLOCK.defaultBlockState());

        Block liningBlock = lining.getBlock();
        BlockState budding = BCAdditions.BUDDING_ROSE_QUARTZ.get().defaultBlockState();

        // ---- Pass 1: build full geode ----
        int carvedAny = 0;
        int placedAny = 0;


        for (int dx = -outerR; dx <= outerR; dx++) {
            for (int dy = -outerR; dy <= outerR; dy++) {
                for (int dz = -outerR; dz <= outerR; dz++) {
                    BlockPos p = origin.offset(dx, dy, dz);

                    int dist2 = dx * dx + dy * dy + dz * dz;
                    if (dist2 > outerR2) continue;

                    if (!canReplace(level, p)) continue;

                    // Hollow first (never overwritten)
                    if (dist2 <= hollowR2) {
                        if (level.getFluidState(p).isEmpty()) {
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                            carvedAny++;
                        }
                        continue;
                    }

                    // Lining (one band)
                    if (dist2 <= liningR2) {
                        level.setBlock(p, lining, Block.UPDATE_ALL);
                        placedAny++;
                        continue;
                    }

                    // Inner shell (one band)
                    if (dist2 <= innerShellR2) {
                        level.setBlock(p, innerShell, Block.UPDATE_ALL);
                        placedAny++;
                        continue;
                    }

                    // Outer shell (remaining)
                    level.setBlock(p, outerShell, Block.UPDATE_ALL);
                    placedAny++;

                }
            }
        }

        if (placedAny == 0) return false;

        // ---- Pass 2: replace some inner-surface lining with budding blocks ----
        int buddingPlaced = replaceSomeInnerLiningWithBudding(
                level, origin, outerR, liningBlock, budding, random,
                0.09f // 9% of eligible inner-surface lining blocks become budding
        );

        // ---- Pass 3: crack AFTER layers are placed ----
        // 25% to 45% maximum crack removal, jagged
        // Currently disabled for further testing
        /*
        boolean cracked = random.nextFloat() < 0.65f; // many geodes are cracked in practice
        if (cracked) {
            double crackFraction = 0.25 + random.nextDouble() * 0.20; // 0.25..0.45
            applyCrack(level, origin, outerR, random, crackFraction);
        }
        */

        return true;
    }

    private static boolean canReplace(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        // Replace anything ores could normally replace
        if (state.is(STONE_REPLACEABLES)
                || state.is(DEEPSLATE_REPLACEABLES)
                || state.is(FORGE_ORES)) {
            return true;
        }

        // Allow air only if adjacent to replaceable stone
        if (state.isAir()) {
            for (Direction dir : Direction.values()) {
                BlockState adj = level.getBlockState(pos.relative(dir));
                if (adj.is(STONE_REPLACEABLES) || adj.is(DEEPSLATE_REPLACEABLES)) {
                    return true;
                }
            }
        }

        return false;
    }

    //Lava check
    //Currently disabled for further testing
    private static boolean lavaNearby(LevelAccessor level, BlockPos origin, int r) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int r2 = r * r;

        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    // Optional: keep the scan roughly spherical to reduce work
                    if (dx * dx + dy * dy + dz * dz > r2) continue;

                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);

                    // Safety
                    if (!level.hasChunkAt(cursor)) continue;

                    if (level.getFluidState(cursor).is(Fluids.LAVA)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    /**
     * Replace a small portion of lining blocks that are on the INNER surface
     * (i.e., touch air) with the budding block.
     */
    private static int replaceSomeInnerLiningWithBudding(WorldGenLevel level, BlockPos origin, int outerR,
                                                         Block liningBlock, BlockState buddingState,
                                                         RandomSource random, float chance) {
        int placed = 0;

        for (int dx = -outerR; dx <= outerR; dx++) {
            for (int dy = -outerR; dy <= outerR; dy++) {
                for (int dz = -outerR; dz <= outerR; dz++) {
                    BlockPos p = origin.offset(dx, dy, dz);

                    if (!level.getBlockState(p).is(liningBlock)) continue;

                    // Must touch the hollow (air) to be inner surface
                    if (!(level.getBlockState(p.north()).isAir()
                            || level.getBlockState(p.south()).isAir()
                            || level.getBlockState(p.east()).isAir()
                            || level.getBlockState(p.west()).isAir()
                            || level.getBlockState(p.above()).isAir()
                            || level.getBlockState(p.below()).isAir())) {
                        continue;
                    }

                    if (random.nextFloat() > chance) continue;

                    level.setBlock(p, buddingState, Block.UPDATE_ALL);
                    placed++;
                }
            }
        }

        return placed;
    }

    /**
     * Crack pass that carves away a jagged wedge of the geode AFTER it is built.
     * We use a directional dot-product threshold (cap) + small random jitter per voxel
     * to avoid a clean slice.
     */
    private static void applyCrack(WorldGenLevel level, BlockPos origin, int outerR,
                                   RandomSource random, double crackFraction) {
        Direction crackDir = Direction.getRandom(random);

        // capFraction = (1 - cosθ)/2 => cosθ = 1 - 2*f
        double cosThreshold = 1.0 - 2.0 * clamp(crackFraction, 0.0, 0.90);

        double ox = crackDir.getStepX();
        double oy = crackDir.getStepY();
        double oz = crackDir.getStepZ();

        int outerR2 = outerR * outerR;

        for (int dx = -outerR; dx <= outerR; dx++) {
            for (int dy = -outerR; dy <= outerR; dy++) {
                for (int dz = -outerR; dz <= outerR; dz++) {
                    int dist2 = dx*dx + dy*dy + dz*dz;
                    if (dist2 > outerR2) continue;

                    // normalize (avoid center)
                    double len = Math.sqrt((double)dist2);
                    if (len < 1e-6) continue;

                    double vx = dx / len;
                    double vy = dy / len;
                    double vz = dz / len;

                    double dot = vx * ox + vy * oy + vz * oz;

                    // jitter makes the crack edge jagged, not planar
                    double jitter = (random.nextDouble() - 0.5) * 0.18; // tune: 0.10..0.25
                    if (dot + jitter >= cosThreshold) {
                        BlockPos p = origin.offset(dx, dy, dz);

                        // carve to air unless fluid is present
                        if (level.getFluidState(p).isEmpty()) {
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                        }
                    }
                }
            }
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}