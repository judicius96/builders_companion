package com.builderscompanion.core.registry.tintedliquids;

import com.builderscompanion.core.BCCore;
import com.builderscompanion.core.util.BCLogger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;  // ← ADD THIS
import java.util.HashMap;
import java.util.List;        // ← ADD THIS
import java.util.Map;

public class TintedLiquidsRegistry {

    /* -------------------------------------------------------------------------
     * Deferred Registers
     * ------------------------------------------------------------------------- */

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, BCCore.MODID);

    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, BCCore.MODID);

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, BCCore.MODID);

    public static List<RegistryObject<ForgeFlowingFluid>> getAllStillFluids() {
        return new ArrayList<>(stillFluidRegistry.values());
    }

    public static List<RegistryObject<ForgeFlowingFluid>> getAllFlowingFluids() {
        return new ArrayList<>(flowingFluidRegistry.values());
    }


    /* -------------------------------------------------------------------------
     * Internal Registries
     * ------------------------------------------------------------------------- */

    private static final Map<Integer, RegistryObject<FluidType>> fluidTypeRegistry = new HashMap<>();
    private static final Map<Integer, RegistryObject<ForgeFlowingFluid>> stillFluidRegistry = new HashMap<>();
    private static final Map<Integer, RegistryObject<ForgeFlowingFluid>> flowingFluidRegistry = new HashMap<>();
    private static final Map<Integer, RegistryObject<LiquidBlock>> liquidBlockRegistry = new HashMap<>();

    /* -------------------------------------------------------------------------
     * Public Lookup Arrays
     * ------------------------------------------------------------------------- */

    public static final FluidType[] FLUID_TYPES_ARRAY = new FluidType[256];
    public static final Fluid[] STILL_FLUIDS = new Fluid[256];
    public static final Fluid[] FLOWING_FLUIDS = new Fluid[256];
    public static final LiquidBlock[] LIQUID_BLOCKS = new LiquidBlock[256];

    /* -------------------------------------------------------------------------
     * Registration
     * ------------------------------------------------------------------------- */

    private static final List<ColorRegistration> SUBMITTED_COLORS = new ArrayList<>();

    public static List<ColorRegistration> getSubmittedColors() {
        return List.copyOf(SUBMITTED_COLORS);
    }

    //private static final List<ColorRegistration> pendingRegistrations = new ArrayList<>();

    // Add this inner class
    public static class ColorRegistration {
        public final int typeId;
        public final int rgb;
        public final String category;

        public ColorRegistration(int typeId, int rgb, String category) {
            this.typeId = typeId;
            this.rgb = rgb;
            this.category = category;
        }
    }

    // Replace registerAll() with this:
    public static void registerColors(List<ColorRegistration> colors) {
        SUBMITTED_COLORS.clear();
        SUBMITTED_COLORS.addAll(colors);

        BCLogger.info("Registering {} tinted water variants...", colors.size());

        for (ColorRegistration entry : colors) {
            final int typeId = entry.typeId;
            final int rgb = entry.rgb;
            final String idSuffix = String.format("%03d", typeId);

            /* ---------------- FluidType (tint + textures) ---------------- */
            RegistryObject<FluidType> fluidType = FLUID_TYPES.register(
                    "tinted_water_" + idSuffix,
                    () -> new FluidType(FluidType.Properties.create()
                            .density(1000)
                            .viscosity(1000)
                            .canDrown(true)
                            .canSwim(true)
                            .supportsBoating(true)
                            .canHydrate(true)
                            .canExtinguish(true)
                            .motionScale(0.014D)
                            .fallDistanceModifier(0.0F)
                    ) {

                        @Override
                        public void initializeClient(java.util.function.Consumer<IClientFluidTypeExtensions> consumer) {
                            consumer.accept(new IClientFluidTypeExtensions() {
                                private static final ResourceLocation STILL =
                                        new ResourceLocation(BCCore.MODID, "block/water_still");
                                private static final ResourceLocation FLOWING =
                                        new ResourceLocation(BCCore.MODID, "block/water_flow");

                                @Override
                                public ResourceLocation getStillTexture() {
                                    return STILL;
                                }

                                @Override
                                public ResourceLocation getFlowingTexture() {
                                    return FLOWING;
                                }

                                @Override
                                public int getTintColor() {
                                    return 0xFF000000 | rgb;
                                }
                            });
                        }
                    }
            );
            fluidTypeRegistry.put(typeId, fluidType);

            /* ---------------- Liquid Block ---------------- */
            RegistryObject<LiquidBlock> liquidBlock = BLOCKS.register(
                    "tinted_water_block_" + idSuffix,
                    () -> new LiquidBlock(
                            () -> stillFluidRegistry.get(typeId).get(),
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.WATER)
                                    .replaceable()
                                    .noCollission()
                                    .strength(100.0F)
                                    .noLootTable()
                                    .liquid()
                    )
            );
            liquidBlockRegistry.put(typeId, liquidBlock);

            /* ---------------- ForgeFlowingFluid.Properties ---------------- */
            ForgeFlowingFluid.Properties props = new ForgeFlowingFluid.Properties(
                    fluidType,
                    () -> stillFluidRegistry.get(typeId).get(),
                    () -> flowingFluidRegistry.get(typeId).get()
            )
                    .block(liquidBlock)
                    .slopeFindDistance(4)
                    .levelDecreasePerBlock(1)
                    .tickRate(5)
                    .explosionResistance(100.0F);

            /* ---------------- Still / Flowing Fluids ---------------- */
            RegistryObject<ForgeFlowingFluid> still = FLUIDS.register(
                    "tinted_water_still_" + idSuffix,
                    () -> new TintedSource(props)
            );
            stillFluidRegistry.put(typeId, still);

            RegistryObject<ForgeFlowingFluid> flowing = FLUIDS.register(
                    "tinted_water_flowing_" + idSuffix,
                    () -> new TintedFlowing(props)
            );
            flowingFluidRegistry.put(typeId, flowing);
        }

        BCLogger.info("Tinted water registration complete.");
    }

    public static void populateArrays() {
        BCLogger.info("populateArrays() called. Registry sizes:");
        BCLogger.info("  fluidTypeRegistry: {}", fluidTypeRegistry.size());
        BCLogger.info("  stillFluidRegistry: {}", stillFluidRegistry.size());
        BCLogger.info("  flowingFluidRegistry: {}", flowingFluidRegistry.size());
        BCLogger.info("  liquidBlockRegistry: {}", liquidBlockRegistry.size());

        for (int i : fluidTypeRegistry.keySet()) {
            FLUID_TYPES_ARRAY[i] = fluidTypeRegistry.get(i).get();
            STILL_FLUIDS[i] = stillFluidRegistry.get(i).get();
            FLOWING_FLUIDS[i] = flowingFluidRegistry.get(i).get();
            LIQUID_BLOCKS[i] = liquidBlockRegistry.get(i).get();
        }

        BCLogger.info("Populated fluid arrays for {} types", fluidTypeRegistry.size());

        if (fluidTypeRegistry.containsKey(76)) {
            BCLogger.info("TypeId 76 exists in registry!");
            BCLogger.info("  LIQUID_BLOCKS[76] = {}", LIQUID_BLOCKS[76]);
        } else {
            BCLogger.error("TypeId 76 NOT in registry! Keys present: {}", fluidTypeRegistry.keySet());
        }
    }


    /* -------------------------------------------------------------------------
     * Custom Forge fluids
     * ------------------------------------------------------------------------- */

    /**
     * Source variant.
     * For vanilla-like behavior, respect the water source conversion gamerule.
     */
    private static class TintedSource extends ForgeFlowingFluid.Source {
        public TintedSource(Properties properties) {
            super(properties);
        }

        @Override
        protected boolean canConvertToSource(Level level) {
            return level.getGameRules().getBoolean(GameRules.RULE_WATER_SOURCE_CONVERSION);
        }

        // If your mappings call the overload form, keep this too.
        @Override
        public boolean canConvertToSource(FluidState state, Level level, BlockPos pos) {
            return canConvertToSource(level);
        }

        @Override
        protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
            // vanilla-style no-op for water
        }

        @Override
        protected boolean canBeReplacedWith(
                FluidState state,
                BlockGetter level,
                BlockPos pos,
                Fluid fluid,
                Direction direction
        ) {
            // Vanilla water behavior: can be replaced from below by NON-water.
            return direction == Direction.DOWN && !fluid.is(FluidTags.WATER);
        }
    }

    /**
     * Flowing variant.
     * Enables infinite source creation, controlled by the vanilla gamerule.
     */
    private static class TintedFlowing extends ForgeFlowingFluid.Flowing {
        public TintedFlowing(Properties properties) {
            super(properties);
        }

        @Override
        protected boolean canConvertToSource(Level level) {
            return level.getGameRules().getBoolean(GameRules.RULE_WATER_SOURCE_CONVERSION);
        }

        // If your mappings call the overload form, keep this too.
        @Override
        public boolean canConvertToSource(FluidState state, Level level, BlockPos pos) {
            return canConvertToSource(level);
        }

        @Override
        protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
            // vanilla-style no-op for water
        }

        @Override
        protected boolean canBeReplacedWith(
                FluidState state,
                BlockGetter level,
                BlockPos pos,
                Fluid fluid,
                Direction direction
        ) {
            // Vanilla water behavior: can be replaced from below by NON-water.
            return direction == Direction.DOWN && !fluid.is(FluidTags.WATER);
        }
    }
}

