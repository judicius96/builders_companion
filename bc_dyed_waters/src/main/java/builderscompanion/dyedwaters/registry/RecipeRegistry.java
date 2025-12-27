package com.builderscompanion.dyedwaters.recipes;

import com.builderscompanion.dyedwaters.BCDyedWaters;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class RecipeRegistry {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, BCDyedWaters.MODID);

    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, BCDyedWaters.MODID);

    public static final RegistryObject<RecipeType<com.builderscompanion.dyedwaters.recipe.DyedWaterRecipes>> DYED_WATER_TYPE =
            TYPES.register("dyed_water", () -> new RecipeType<com.builderscompanion.dyedwaters.recipe.DyedWaterRecipes>() {});

    public static final com.builderscompanion.dyedwaters.recipe.DyedWaterRecipes.Serializer SERIALIZER_INSTANCE = new com.builderscompanion.dyedwaters.recipe.DyedWaterRecipes.Serializer();

    public static final RegistryObject<RecipeSerializer<com.builderscompanion.dyedwaters.recipe.DyedWaterRecipes>> DYED_WATER_SERIALIZER =
            SERIALIZERS.register("dyed_water", () -> SERIALIZER_INSTANCE);
}