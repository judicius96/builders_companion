package com.builderscompanion.dyedwaters.recipe;

import com.builderscompanion.core.item.tintedliquids.TintedWaterBucketItem;
import com.builderscompanion.core.registry.tintedliquids.TintedIdRanges;
import com.builderscompanion.core.registry.tintedliquids.TintedLiquidsItems;
import com.builderscompanion.dyedwaters.registry.DyedColorRegistry;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

public class DyedWaterRecipes extends CustomRecipe {

    private final RecipeVariant variant;

    public DyedWaterRecipes(ResourceLocation id, CraftingBookCategory category, RecipeVariant variant) {
        super(id, category);
        this.variant = variant;
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return variant.matches(container);
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        return variant.assemble(container);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return com.builderscompanion.dyedwaters.recipes.RecipeRegistry.DYED_WATER_SERIALIZER.get();
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);

            // Don't return bucket items
            if (stack.is(Items.WATER_BUCKET) || stack.is(TintedLiquidsItems.TINTED_WATER_BUCKET.get())) {
                continue;
            }

            // Return vanilla craft remainders for other items
            if (stack.hasCraftingRemainingItem()) {
                remaining.set(i, stack.getCraftingRemainingItem());
            }
        }

        return remaining;
    }

    public enum RecipeVariant {
        BASE_DYE {
            @Override
            public boolean matches(CraftingContainer container) {
                ItemStack waterBucket = ItemStack.EMPTY;
                ItemStack dye1 = ItemStack.EMPTY;
                ItemStack dye2 = ItemStack.EMPTY;
                int itemCount = 0;

                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (stack.isEmpty()) continue;

                    itemCount++;

                    if (stack.is(Items.WATER_BUCKET)) {
                        if (!waterBucket.isEmpty()) return false;
                        waterBucket = stack;
                    } else if (stack.getItem() instanceof DyeItem) {
                        if (dye1.isEmpty()) {
                            dye1 = stack;
                        } else if (dye2.isEmpty()) {
                            dye2 = stack;
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }

                if (itemCount != 3) return false;
                if (waterBucket.isEmpty() || dye1.isEmpty() || dye2.isEmpty()) return false;

                return dye1.getItem() == dye2.getItem();
            }

            @Override
            public ItemStack assemble(CraftingContainer container) {
                ItemStack dye = ItemStack.EMPTY;

                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (stack.getItem() instanceof DyeItem) {
                        dye = stack;
                        break;
                    }
                }

                if (dye.isEmpty()) return ItemStack.EMPTY;

                String dyeId = ForgeRegistries.ITEMS.getKey(dye.getItem()).toString();
                Integer rgb = DyedColorRegistry.getDyeColor(dyeId);
                if (rgb == null) return ItemStack.EMPTY;

                int baseTypeId = -1;
                for (DyedColorRegistry.ColorEntry entry : DyedColorRegistry.getUniqueColors()) {
                    if (entry.rgb == rgb && "dyed".equals(entry.category)) {
                        baseTypeId = entry.typeId;
                        break;
                    }
                }

                if (baseTypeId < 0) return ItemStack.EMPTY;

                ItemStack result = new ItemStack(TintedLiquidsItems.TINTED_WATER_BUCKET.get());
                TintedWaterBucketItem.setTypeId(result, baseTypeId);
                return result;
            }
        },
        INFUSED_SKIP {
            @Override
            public boolean matches(CraftingContainer container) {
                ItemStack waterBucket = ItemStack.EMPTY;
                ItemStack dye1 = ItemStack.EMPTY;
                ItemStack dye2 = ItemStack.EMPTY;
                ItemStack clay = ItemStack.EMPTY;
                int itemCount = 0;

                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (stack.isEmpty()) continue;

                    itemCount++;

                    if (stack.is(Items.WATER_BUCKET)) {
                        if (!waterBucket.isEmpty()) return false;
                        waterBucket = stack;
                    } else if (stack.is(ItemTags.create(new ResourceLocation("bc_core", "bc_dyes")))) {
                        if (dye1.isEmpty()) {
                            dye1 = stack;
                        } else if (dye2.isEmpty()) {
                            dye2 = stack;
                        } else {
                            return false;
                        }
                    } else if (stack.is(Items.CLAY_BALL)) {
                        if (!clay.isEmpty()) return false;
                        clay = stack;
                    } else {
                        return false;
                    }
                }

                if (itemCount != 4) return false;
                if (waterBucket.isEmpty() || dye1.isEmpty() || dye2.isEmpty() || clay.isEmpty()) return false;

                return dye1.getItem() == dye2.getItem();
            }

            @Override
            public ItemStack assemble(CraftingContainer container) {
                ItemStack dye = ItemStack.EMPTY;

                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (stack.getItem() instanceof DyeItem) {
                        dye = stack;
                        break;
                    }
                }

                if (dye.isEmpty()) return ItemStack.EMPTY;

                String dyeId = ForgeRegistries.ITEMS.getKey(dye.getItem()).toString();
                Integer rgb = DyedColorRegistry.getDyeColor(dyeId);
                if (rgb == null) return ItemStack.EMPTY;

                int infusedTypeId = -1;
                for (DyedColorRegistry.ColorEntry entry : DyedColorRegistry.getUniqueColors()) {
                    if (entry.rgb == rgb && "infused".equals(entry.category)) {
                        infusedTypeId = entry.typeId;
                        break;
                    }
                }

                if (infusedTypeId < 0) return ItemStack.EMPTY;

                ItemStack result = new ItemStack(TintedLiquidsItems.TINTED_WATER_BUCKET.get());
                TintedWaterBucketItem.setTypeId(result, infusedTypeId);
                return result;
            }
        },
        INFUSED_UPGRADE {
            @Override
            public boolean matches(CraftingContainer container) {
                ItemStack bucket = ItemStack.EMPTY;
                ItemStack dye = ItemStack.EMPTY;
                ItemStack clay = ItemStack.EMPTY;
                int itemCount = 0;

                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (stack.isEmpty()) continue;

                    itemCount++;

                    if (stack.is(TintedLiquidsItems.TINTED_WATER_BUCKET.get())) {
                        if (!bucket.isEmpty()) return false;
                        bucket = stack;
                    } else if (stack.is(ItemTags.create(new ResourceLocation("bc_core", "bc_dyes")))) {
                        if (!dye.isEmpty()) return false;
                        dye = stack;
                    } else if (stack.is(Items.CLAY_BALL)) {
                        if (!clay.isEmpty()) return false;
                        clay = stack;
                    } else {
                        return false;
                    }
                }

                if (itemCount != 3) return false;
                if (bucket.isEmpty() || dye.isEmpty() || clay.isEmpty()) return false;

                // Bucket must be base variant (variant 0)
                int typeId = TintedWaterBucketItem.getTypeId(bucket);
                if (typeId < TintedIdRanges.DYE_START) return false;

                int variant = (typeId - TintedIdRanges.DYE_START) & 3;
                if (variant != 0) return false;

                // Dye must match bucket color
                String dyeId = ForgeRegistries.ITEMS.getKey(dye.getItem()).toString();
                Integer dyeRgb = DyedColorRegistry.getDyeColor(dyeId);
                if (dyeRgb == null) return false;

                DyedColorRegistry.ColorEntry bucketEntry = DyedColorRegistry.getEntryByTypeId(typeId);
                if (bucketEntry == null) return false;

                return bucketEntry.rgb == dyeRgb;
            }

            @Override
            public ItemStack assemble(CraftingContainer container) {
                ItemStack bucket = ItemStack.EMPTY;

                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (stack.is(TintedLiquidsItems.TINTED_WATER_BUCKET.get())) {
                        bucket = stack;
                        break;
                    }
                }

                if (bucket.isEmpty()) return ItemStack.EMPTY;

                int baseTypeId = TintedWaterBucketItem.getTypeId(bucket);
                int infusedTypeId = baseTypeId + 1; // Variant 0 → Variant 1

                ItemStack result = new ItemStack(TintedLiquidsItems.TINTED_WATER_BUCKET.get());
                TintedWaterBucketItem.setTypeId(result, infusedTypeId);
                return result;
            }
        },
        RADIANT {
            @Override
            public boolean matches(CraftingContainer container) {
                ItemStack bucket = ItemStack.EMPTY;
                ItemStack ink1 = ItemStack.EMPTY;
                ItemStack ink2 = ItemStack.EMPTY;
                int itemCount = 0;

                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (stack.isEmpty()) continue;

                    itemCount++;

                    if (stack.is(TintedLiquidsItems.TINTED_WATER_BUCKET.get())) {
                        if (!bucket.isEmpty()) return false;
                        bucket = stack;
                    } else if (stack.is(Items.GLOW_INK_SAC)) {
                        if (ink1.isEmpty()) {
                            ink1 = stack;
                        } else if (ink2.isEmpty()) {
                            ink2 = stack;
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }

                if (itemCount != 3) return false;
                if (bucket.isEmpty() || ink1.isEmpty() || ink2.isEmpty()) return false;

                // Bucket must be base variant (variant 0)
                int typeId = TintedWaterBucketItem.getTypeId(bucket);
                if (typeId < TintedIdRanges.DYE_START) return false;

                int variant = (typeId - TintedIdRanges.DYE_START) & 3;
                return variant == 0;
            }

            @Override
            public ItemStack assemble(CraftingContainer container) {
                ItemStack bucket = ItemStack.EMPTY;

                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (stack.is(TintedLiquidsItems.TINTED_WATER_BUCKET.get())) {
                        bucket = stack;
                        break;
                    }
                }

                if (bucket.isEmpty()) return ItemStack.EMPTY;

                int baseTypeId = TintedWaterBucketItem.getTypeId(bucket);
                int radiantTypeId = baseTypeId + 2; // Variant 0 → Variant 2

                ItemStack result = new ItemStack(TintedLiquidsItems.TINTED_WATER_BUCKET.get());
                TintedWaterBucketItem.setTypeId(result, radiantTypeId);
                return result;
            }
        },
        INFUSED_RADIANT_FROM_RADIANT {
            @Override
            public boolean matches(CraftingContainer container) {
                ItemStack bucket = ItemStack.EMPTY;
                ItemStack ink = ItemStack.EMPTY;
                ItemStack shroomlight = ItemStack.EMPTY;
                int itemCount = 0;

                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (stack.isEmpty()) continue;

                    itemCount++;

                    if (stack.is(TintedLiquidsItems.TINTED_WATER_BUCKET.get())) {
                        if (!bucket.isEmpty()) return false;
                        bucket = stack;
                    } else if (stack.is(Items.GLOW_INK_SAC)) {
                        if (!ink.isEmpty()) return false;
                        ink = stack;
                    } else if (stack.is(Items.SHROOMLIGHT)) {
                        if (!shroomlight.isEmpty()) return false;
                        shroomlight = stack;
                    } else {
                        return false;
                    }
                }

                if (itemCount != 3) return false;
                if (bucket.isEmpty() || ink.isEmpty() || shroomlight.isEmpty()) return false;

                // Bucket must be radiant variant (variant 2)
                int typeId = TintedWaterBucketItem.getTypeId(bucket);
                if (typeId < TintedIdRanges.DYE_START) return false;

                int variant = (typeId - TintedIdRanges.DYE_START) & 3;
                return variant == 2;
            }

            @Override
            public ItemStack assemble(CraftingContainer container) {
                ItemStack bucket = ItemStack.EMPTY;

                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (stack.is(TintedLiquidsItems.TINTED_WATER_BUCKET.get())) {
                        bucket = stack;
                        break;
                    }
                }

                if (bucket.isEmpty()) return ItemStack.EMPTY;

                int radiantTypeId = TintedWaterBucketItem.getTypeId(bucket);
                int infusedRadiantTypeId = radiantTypeId + 1; // Variant 2 → Variant 3

                ItemStack result = new ItemStack(TintedLiquidsItems.TINTED_WATER_BUCKET.get());
                TintedWaterBucketItem.setTypeId(result, infusedRadiantTypeId);
                return result;
            }
        },
        INFUSED_RADIANT_SKIP {
            @Override
            public boolean matches(CraftingContainer container) {
                ItemStack bucket = ItemStack.EMPTY;
                ItemStack ink1 = ItemStack.EMPTY;
                ItemStack ink2 = ItemStack.EMPTY;
                ItemStack shroomlight = ItemStack.EMPTY;
                int itemCount = 0;

                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (stack.isEmpty()) continue;

                    itemCount++;

                    if (stack.is(TintedLiquidsItems.TINTED_WATER_BUCKET.get())) {
                        if (!bucket.isEmpty()) return false;
                        bucket = stack;
                    } else if (stack.is(Items.GLOW_INK_SAC)) {
                        if (ink1.isEmpty()) {
                            ink1 = stack;
                        } else if (ink2.isEmpty()) {
                            ink2 = stack;
                        } else {
                            return false;
                        }
                    } else if (stack.is(Items.SHROOMLIGHT)) {
                        if (!shroomlight.isEmpty()) return false;
                        shroomlight = stack;
                    } else {
                        return false;
                    }
                }

                if (itemCount != 4) return false;
                if (bucket.isEmpty() || ink1.isEmpty() || ink2.isEmpty() || shroomlight.isEmpty()) return false;

                // Bucket must be base variant (variant 0)
                int typeId = TintedWaterBucketItem.getTypeId(bucket);
                if (typeId < TintedIdRanges.DYE_START) return false;

                int variant = (typeId - TintedIdRanges.DYE_START) & 3;
                return variant == 0;
            }

            @Override
            public ItemStack assemble(CraftingContainer container) {
                ItemStack bucket = ItemStack.EMPTY;

                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (stack.is(TintedLiquidsItems.TINTED_WATER_BUCKET.get())) {
                        bucket = stack;
                        break;
                    }
                }

                if (bucket.isEmpty()) return ItemStack.EMPTY;

                int baseTypeId = TintedWaterBucketItem.getTypeId(bucket);
                int infusedRadiantTypeId = baseTypeId + 3; // Variant 0 → Variant 3

                ItemStack result = new ItemStack(TintedLiquidsItems.TINTED_WATER_BUCKET.get());
                TintedWaterBucketItem.setTypeId(result, infusedRadiantTypeId);
                return result;
            }
        },
        INFUSED_RADIANT_FROM_INFUSED {
            @Override
            public boolean matches(CraftingContainer container) {
                ItemStack bucket = ItemStack.EMPTY;
                ItemStack ink1 = ItemStack.EMPTY;
                ItemStack ink2 = ItemStack.EMPTY;
                int itemCount = 0;

                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (stack.isEmpty()) continue;

                    itemCount++;

                    if (stack.is(TintedLiquidsItems.TINTED_WATER_BUCKET.get())) {
                        if (!bucket.isEmpty()) return false;
                        bucket = stack;
                    } else if (stack.is(Items.GLOW_INK_SAC)) {
                        if (ink1.isEmpty()) {
                            ink1 = stack;
                        } else if (ink2.isEmpty()) {
                            ink2 = stack;
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }

                if (itemCount != 3) return false;
                if (bucket.isEmpty() || ink1.isEmpty() || ink2.isEmpty()) return false;

                // Bucket must be infused variant (variant 1)
                int typeId = TintedWaterBucketItem.getTypeId(bucket);
                if (typeId < TintedIdRanges.DYE_START) return false;

                int variant = (typeId - TintedIdRanges.DYE_START) & 3;
                return variant == 1;
            }

            @Override
            public ItemStack assemble(CraftingContainer container) {
                ItemStack bucket = ItemStack.EMPTY;

                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (stack.is(TintedLiquidsItems.TINTED_WATER_BUCKET.get())) {
                        bucket = stack;
                        break;
                    }
                }

                if (bucket.isEmpty()) return ItemStack.EMPTY;

                int infusedTypeId = TintedWaterBucketItem.getTypeId(bucket);
                int infusedRadiantTypeId = infusedTypeId + 2; // Variant 1 → Variant 3

                ItemStack result = new ItemStack(TintedLiquidsItems.TINTED_WATER_BUCKET.get());
                TintedWaterBucketItem.setTypeId(result, infusedRadiantTypeId);
                return result;
            }
        },
        REMOVE_DYE {
            @Override
            public boolean matches(CraftingContainer container) {
                ItemStack bucket = ItemStack.EMPTY;
                ItemStack charcoal1 = ItemStack.EMPTY;
                ItemStack charcoal2 = ItemStack.EMPTY;
                int itemCount = 0;

                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (stack.isEmpty()) continue;

                    itemCount++;

                    if (stack.is(TintedLiquidsItems.TINTED_WATER_BUCKET.get())) {
                        if (!bucket.isEmpty()) return false;
                        bucket = stack;
                    } else if (stack.is(Items.CHARCOAL)) {
                        if (charcoal1.isEmpty()) {
                            charcoal1 = stack;
                        } else if (charcoal2.isEmpty()) {
                            charcoal2 = stack;
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }

                if (itemCount != 3) return false;
                if (bucket.isEmpty() || charcoal1.isEmpty() || charcoal2.isEmpty()) return false;

                // Bucket must NOT be radiant (variant 0 or 1 only)
                int typeId = TintedWaterBucketItem.getTypeId(bucket);
                if (typeId < TintedIdRanges.DYE_START) return false;

                int variant = (typeId - TintedIdRanges.DYE_START) & 3;
                return variant == 0 || variant == 1;
            }

            @Override
            public ItemStack assemble(CraftingContainer container) {
                return new ItemStack(Items.WATER_BUCKET);
            }
        },
        REMOVE_GLOW {
            @Override
            public boolean matches(CraftingContainer container) {
                ItemStack bucket = ItemStack.EMPTY;
                ItemStack ink1 = ItemStack.EMPTY;
                ItemStack ink2 = ItemStack.EMPTY;
                int itemCount = 0;

                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (stack.isEmpty()) continue;

                    itemCount++;

                    if (stack.is(TintedLiquidsItems.TINTED_WATER_BUCKET.get())) {
                        if (!bucket.isEmpty()) return false;
                        bucket = stack;
                    } else if (stack.is(Items.INK_SAC)) {
                        if (ink1.isEmpty()) {
                            ink1 = stack;
                        } else if (ink2.isEmpty()) {
                            ink2 = stack;
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }

                if (itemCount != 3) return false;
                if (bucket.isEmpty() || ink1.isEmpty() || ink2.isEmpty()) return false;

                // Bucket must be radiant (variant 2 or 3)
                int typeId = TintedWaterBucketItem.getTypeId(bucket);
                if (typeId < TintedIdRanges.DYE_START) return false;

                int variant = (typeId - TintedIdRanges.DYE_START) & 3;
                return variant == 2 || variant == 3;
            }

            @Override
            public ItemStack assemble(CraftingContainer container) {
                ItemStack bucket = ItemStack.EMPTY;

                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (stack.is(TintedLiquidsItems.TINTED_WATER_BUCKET.get())) {
                        bucket = stack;
                        break;
                    }
                }

                if (bucket.isEmpty()) return ItemStack.EMPTY;

                int radiantTypeId = TintedWaterBucketItem.getTypeId(bucket);
                int downgradeTypeId = radiantTypeId - 2; // Variant 3→1 or 2→0

                ItemStack result = new ItemStack(TintedLiquidsItems.TINTED_WATER_BUCKET.get());
                TintedWaterBucketItem.setTypeId(result, downgradeTypeId);
                return result;
            }
        },
        REMOVE_BOTH {
            @Override
            public boolean matches(CraftingContainer container) {
                ItemStack bucket = ItemStack.EMPTY;
                ItemStack ink1 = ItemStack.EMPTY;
                ItemStack ink2 = ItemStack.EMPTY;
                ItemStack charcoal1 = ItemStack.EMPTY;
                ItemStack charcoal2 = ItemStack.EMPTY;
                int itemCount = 0;

                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (stack.isEmpty()) continue;

                    itemCount++;

                    if (stack.is(TintedLiquidsItems.TINTED_WATER_BUCKET.get())) {
                        if (!bucket.isEmpty()) return false;
                        bucket = stack;
                    } else if (stack.is(Items.INK_SAC)) {
                        if (ink1.isEmpty()) {
                            ink1 = stack;
                        } else if (ink2.isEmpty()) {
                            ink2 = stack;
                        } else {
                            return false;
                        }
                    } else if (stack.is(Items.CHARCOAL)) {
                        if (charcoal1.isEmpty()) {
                            charcoal1 = stack;
                        } else if (charcoal2.isEmpty()) {
                            charcoal2 = stack;
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }

                if (itemCount != 5) return false;
                if (bucket.isEmpty() || ink1.isEmpty() || ink2.isEmpty() || charcoal1.isEmpty() || charcoal2.isEmpty()) return false;

                // Bucket must be radiant (variant 2 or 3)
                int typeId = TintedWaterBucketItem.getTypeId(bucket);
                if (typeId < TintedIdRanges.DYE_START) return false;

                int variant = (typeId - TintedIdRanges.DYE_START) & 3;
                return variant == 2 || variant == 3;
            }

            @Override
            public ItemStack assemble(CraftingContainer container) {
                return new ItemStack(Items.WATER_BUCKET);
            }
        };

        public abstract boolean matches(CraftingContainer container);
        public abstract ItemStack assemble(CraftingContainer container);
    }

    public static class Serializer implements RecipeSerializer<com.builderscompanion.dyedwaters.recipe.DyedWaterRecipes> {

        @Override
        public com.builderscompanion.dyedwaters.recipe.DyedWaterRecipes fromJson(ResourceLocation id, JsonObject json) {
            CraftingBookCategory category = CraftingBookCategory.CODEC.byName(
                    json.has("category") ? json.get("category").getAsString() : "misc",
                    CraftingBookCategory.MISC
            );

            String variantName = json.has("variant") ? json.get("variant").getAsString() : "BASE_DYE";
            RecipeVariant variant = RecipeVariant.valueOf(variantName.toUpperCase());

            return new com.builderscompanion.dyedwaters.recipe.DyedWaterRecipes(id, category, variant);
        }

        @Override
        public com.builderscompanion.dyedwaters.recipe.DyedWaterRecipes fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
            RecipeVariant variant = buffer.readEnum(RecipeVariant.class);
            return new com.builderscompanion.dyedwaters.recipe.DyedWaterRecipes(id, category, variant);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, com.builderscompanion.dyedwaters.recipe.DyedWaterRecipes recipe) {
            buffer.writeEnum(recipe.category());
            buffer.writeEnum(recipe.variant);
        }
    }
}