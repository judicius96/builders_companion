package com.builderscompanion.core.item.tintedliquids;

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

import static com.builderscompanion.core.registry.tintedliquids.TintedIdRanges.DYE_START;
import static com.builderscompanion.core.registry.tintedliquids.TintedIdRanges.MAX_TYPE_ID;

public class TintedWaterBucketItem extends BucketItem {

    public TintedWaterBucketItem(Properties props) {
        super(Fluids.WATER, props);
    }

    @Override
    public Component getName(ItemStack stack){
        //check for JEI display marker
        if (stack.hasTag() && stack.getTag().contains("DisplayRecipe", 8)) {
            String variant = stack.getTag().getString("DisplayRecipe");
            return Component.literal(variant + " Water Bucket");
        }

        // 2️⃣ Normal runtime naming
        int rawTypeId = getTypeId(stack);
        if (!isValidTypeId(rawTypeId)) {
            return Component.literal("Tinted Water Bucket");
        }

        // Normalize dye variants back to the base dye id for naming
        int baseTypeId = normalizeForNaming(rawTypeId);

        Component baseName = TintedLiquidProviderRegistry.getDisplayName(baseTypeId);
        if (baseName == null) {
            return Component.literal("Tinted Water Bucket");
        }

        // Optional suffix so the four states actually read differently in-game
        String prefix = variantPrefix(rawTypeId);
        return prefix.isEmpty() ? baseName : Component.literal(prefix + baseName.getString());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        int rawTypeId = getTypeId(stack);
        if (!isValidTypeId(rawTypeId)) {
            super.appendHoverText(stack, level, tooltip, flag);
            return;
        }

        int baseTypeId = normalizeForNaming(rawTypeId);

        // Delegate to provider using base id (so you get the correct dye/biome name)
        List<Component> lines = TintedLiquidProviderRegistry.getTooltipLines(baseTypeId);
        if (lines != null && !lines.isEmpty()) {
            tooltip.addAll(lines);
        }

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
        if (!isValidTypeId(typeId)) {
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

    private static boolean isValidTypeId(int typeId) {
        return typeId >= 0 && typeId < MAX_TYPE_ID;
    }

    /**
     * For dyes, collapse the 4-variant ids back to the base dye id for display lookups.
     * For non-dyes, returns unchanged.
     */
    private static int normalizeForNaming(int typeId) {
        if (!isDyeType(typeId)) return typeId;
        return typeId - ((typeId - DYE_START) & 3);
    }

    private static boolean isDyeType(int typeId) {
        // Match your clamp logic conceptually (start at DYE_START, 4-wide blocks)
        return typeId >= DYE_START;
    }

    private static int dyeVariant(int typeId) {
        if (!isDyeType(typeId)) return 0;
        return (typeId - DYE_START) & 3;
    }

    private static String variantPrefix(int typeId) {
        if (!isDyeType(typeId)) return "";
        return switch (dyeVariant(typeId)) {
            case 1 -> "Infused ";
            case 2 -> "Radiant ";
            case 3 -> "Infused Radiant ";
            default -> "";
        };
    }
}
