package com.builderscompanion.biomewaters;

import com.builderscompanion.biomewaters.registry.WaterColorRegistry;
import com.builderscompanion.core.api.TintedLiquidProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class BiomeWaterProvider implements TintedLiquidProvider {

    @Override
    public String getProviderId() {
        return "biome_waters";
    }

    @Override
    public boolean handlesTypeId(int typeId) {
        List<WaterColorRegistry.ColorEntry> colors = WaterColorRegistry.getUniqueColors();
        if (typeId < 0 || typeId >= colors.size()) {
            return false;
        }
        WaterColorRegistry.ColorEntry entry = colors.get(typeId);
        return entry != null && "biome".equals(entry.category);
    }

    @Override
    public Component getDisplayName(int typeId) {
        List<WaterColorRegistry.ColorEntry> colors = WaterColorRegistry.getUniqueColors();
        if (typeId < 0 || typeId >= colors.size()) {
            return null;
        }

        WaterColorRegistry.ColorEntry entry = colors.get(typeId);
        if (entry == null || !"biome".equals(entry.category)) {
            return null;
        }

        String primaryName = WaterColorRegistry.getPrimaryBiomeForColor(entry.rgb);
        if (primaryName != null && !primaryName.isEmpty()) {
            return Component.literal(primaryName + " Water Bucket");
        }
        return Component.literal("Biome Water Bucket");
    }

    @Override
    public int getRgbColor(int typeId) {
        WaterColorRegistry.ColorEntry entry = WaterColorRegistry.getEntryByTypeId(typeId);
        return entry != null ? entry.rgb : 0xFFFFFFFF;
    }

    @Override
    public List<Component> getTooltipLines(int typeId) {
        List<WaterColorRegistry.ColorEntry> colors = WaterColorRegistry.getUniqueColors();
        if (typeId < 0 || typeId >= colors.size()) {
            return List.of();
        }

        WaterColorRegistry.ColorEntry entry = colors.get(typeId);
        if (entry == null || !"biome".equals(entry.category)) {
            return List.of();
        }

        int rgb = entry.rgb;
        List<String> biomes = WaterColorRegistry.getBiomesForColor(rgb);
        List<Component> tooltip = new ArrayList<>();

        if (!biomes.isEmpty()) {
            if (biomes.size() == 1) {
                tooltip.add(Component.literal("Water from: " + biomes.get(0))
                        .withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add(Component.literal("Water from: " + biomes.get(0))
                        .withStyle(ChatFormatting.GRAY));
                String others = String.join(", ", biomes.subList(1, biomes.size()));
                tooltip.add(Component.literal("Also found in: " + others)
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }
        }

        return tooltip;
    }
}