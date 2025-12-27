package com.builderscompanion.bcadditions.enchantment;

import com.builderscompanion.bcadditions.BCAdditions;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, BCAdditions.MODID);

    public static final RegistryObject<Enchantment> SILK_TOUCH_II = ENCHANTMENTS.register(
            "silk_touch_ii",
            () -> new Enchantment(Enchantment.Rarity.VERY_RARE,
                    EnchantmentCategory.DIGGER,
                    new EquipmentSlot[]{EquipmentSlot.MAINHAND}) {
                @Override
                public int getMaxLevel() {
                    return 1;
                }

                @Override
                public boolean checkCompatibility(Enchantment other) {
                    return super.checkCompatibility(other)
                            && other != Enchantments.SILK_TOUCH
                            && other != Enchantments.BLOCK_FORTUNE;
                }
            }
    );
}