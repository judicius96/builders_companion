package com.builderscompanion.core.tintedliquids.item;

import com.builderscompanion.core.registry.WaterColorRegistry;
import com.builderscompanion.core.registry.tintedliquids.TintedLiquidsRegistry;
import com.builderscompanion.core.util.BCLogger;
import net.minecraft.ChatFormatting;
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

    /**
     * Dynamic bucket name based on typeId and category (biome vs dye)
     */
    @Override
    public Component getName(ItemStack stack) {
        int typeId = getTypeId(stack);
        if (typeId < 0 || typeId >= 256) {
            return Component.literal("Tinted Water Bucket");
        }

        // Get the ColorEntry for this typeId
        List<WaterColorRegistry.ColorEntry> colors = WaterColorRegistry.getUniqueColors();
        if (typeId >= colors.size()) {
            return Component.literal("Tinted Water Bucket");
        }

        WaterColorRegistry.ColorEntry entry = colors.get(typeId);

        // Handle biomes
        if ("biome".equals(entry.category)) {
            String primaryName = WaterColorRegistry.getPrimaryBiomeForColor(entry.rgb);
            if (primaryName != null && !primaryName.isEmpty()) {
                return Component.literal(primaryName + " Water Bucket");
            }
            return Component.literal("Biome Water Bucket");
        }

        // Handle dyes
        if ("dye".equals(entry.category)) {
            String dyeName = getDyeNameForColor(entry.rgb);
            if (dyeName != null && !dyeName.isEmpty()) {
                return Component.literal(dyeName + " Water Bucket");
            }
            return Component.literal("Dye Water Bucket");
        }

        // Fallback for unknown categories
        return Component.literal("Tinted Water Bucket");
    }

    /**
     * Enhanced tooltip showing all biomes that share this water color
     */
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        int typeId = getTypeId(stack);
        if (typeId < 0 || typeId >= 256) {
            super.appendHoverText(stack, level, tooltip, flag);
            return;
        }

        // Get the ColorEntry for this typeId
        List<WaterColorRegistry.ColorEntry> colors = WaterColorRegistry.getUniqueColors();
        if (typeId >= colors.size()) {
            super.appendHoverText(stack, level, tooltip, flag);
            return;
        }

        WaterColorRegistry.ColorEntry entry = colors.get(typeId);
        int rgb = entry.rgb;

        // Biome waters: show all biomes with this color
        if ("biome".equals(entry.category)) {
            List<String> biomes = WaterColorRegistry.getBiomesForColor(rgb);

            if (!biomes.isEmpty()) {
                if (biomes.size() == 1) {
                    // Only one biome has this color
                    tooltip.add(Component.literal("Water from: " + biomes.get(0))
                            .withStyle(ChatFormatting.GRAY));
                } else {
                    // Multiple biomes share this color - show primary + others
                    tooltip.add(Component.literal("Water from: " + biomes.get(0))
                            .withStyle(ChatFormatting.GRAY));

                    // Show additional biomes in italics
                    String others = String.join(", ", biomes.subList(1, biomes.size()));
                    tooltip.add(Component.literal("Also found in: " + others)
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                }
            }
        }

        // Dye waters: simple tooltip
        if ("dye".equals(entry.category)) {
            String dyeName = getDyeNameForColor(rgb);
            if (dyeName != null && !dyeName.isEmpty()) {
                tooltip.add(Component.literal("Dyed water: " + dyeName)
                        .withStyle(ChatFormatting.GRAY));
            }
        }

        super.appendHoverText(stack, level, tooltip, flag);
    }

    /**
     * Helper to get dye name from color
     * Uses WaterColorRegistry's existing data
     */
    private String getDyeNameForColor(int rgb) {
        // Get all dye entries (dyeColors map)
        Integer dyeColor = WaterColorRegistry.getDyeColor("minecraft:red_dye"); // Just to check if method exists

        // For now, just return generic "Dyed" until you add the helper method
        // TODO: Add WaterColorRegistry.getPrimaryDyeNameForColor(rgb)
        return "Dyed";
    }

    /**
     * Place tinted water block when using the bucket
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Ray trace like vanilla buckets do
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

        // Vanilla bucket rule: if the clicked block can be replaced, place INTO it.
        // Otherwise place adjacent (offset).
        BlockState clickedState = level.getBlockState(clicked);
        if (clickedState.canBeReplaced()) {
            placePos = clicked;
        } else {
            placePos = clicked.relative(hit.getDirection());
        }

        // Respect basic permissions
        if (!level.mayInteract(player, placePos)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!player.mayUseItemAt(placePos, hit.getDirection(), stack)) {
            return InteractionResultHolder.fail(stack);
        }

        // Can we replace the target?
        BlockState targetState = level.getBlockState(placePos);
        if (!targetState.canBeReplaced()) {
            return InteractionResultHolder.fail(stack);
        }

        // Server: actually place
        if (!level.isClientSide) {
            BlockState placeState = liquidBlock.defaultBlockState();

            // Force source level
            if (placeState.hasProperty(LiquidBlock.LEVEL)) {
                placeState = placeState.setValue(LiquidBlock.LEVEL, 0);
            }

            boolean placed = level.setBlock(placePos, placeState, 11);
            if (!placed) {
                return InteractionResultHolder.fail(stack);
            }

            // Return empty bucket (unless creative mode)
            if (!player.getAbilities().instabuild) {
                return InteractionResultHolder.success(new ItemStack(Items.BUCKET));
            }
        }

        // Client: report success so animation plays
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /**
     * Get typeId from bucket's damage value
     */
    public static int getTypeId(ItemStack stack) {
        return stack.getDamageValue();
    }

    /**
     * Set typeId on bucket's damage value
     */
    public static void setTypeId(ItemStack stack, int typeId) {
        stack.setDamageValue(typeId);
    }
}