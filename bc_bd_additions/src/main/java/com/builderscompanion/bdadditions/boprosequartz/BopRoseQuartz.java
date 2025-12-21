package com.builderscompanion.bdadditions.boprosequartz;

import com.builderscompanion.bdadditions.BCAdditions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

public final class BopRoseQuartz {
    private BopRoseQuartz() {}

    public static Block SMALL = Blocks.AIR;
    public static Block MEDIUM = Blocks.AIR;
    public static Block LARGE = Blocks.AIR;
    public static Block CLUSTER = Blocks.AIR;
    public static Block ROSE_QUARTZ_BLOCK = Blocks.AIR;

    private static boolean loggedSkip = false;

    public static void resolveOrSkip() {
        if (!ModList.get().isLoaded("biomesoplenty")) {
            logSkipOnce("Skipping RoseQuartz features: Biomes O' Plenty is not installed.");
            return;
        }

        SMALL = get("biomesoplenty", "small_rose_quartz_bud");
        MEDIUM = get("biomesoplenty", "medium_rose_quartz_bud");
        LARGE = get("biomesoplenty", "large_rose_quartz_bud");
        CLUSTER = get("biomesoplenty", "rose_quartz_cluster");
        ROSE_QUARTZ_BLOCK = get("biomesoplenty", "rose_quartz_block");

        // Log once if anything important is missing
        if (!ready() || !hasLining()) {
            logSkipOnce("Skipping RoseQuartz features: BoP registry returned null/air for one or more IDs (likely name mismatch).");
        }
    }

    /** Buds/clusters resolved */
    public static boolean ready() {
        return SMALL != Blocks.AIR
                && MEDIUM != Blocks.AIR
                && LARGE != Blocks.AIR
                && CLUSTER != Blocks.AIR;
    }

    /** Lining block resolved */
    public static boolean hasLining() {
        return ROSE_QUARTZ_BLOCK != Blocks.AIR;
    }

    private static Block get(String ns, String path) {
        Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(ns, path));
        return b == null ? Blocks.AIR : b;
    }

    private static void logSkipOnce(String msg) {
        if (!loggedSkip) {
            loggedSkip = true;
            BCAdditions.LOGGER.warn(msg);
        }
    }
}
