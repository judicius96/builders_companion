/*
 * MIT License
 *
 * Copyright (c) 2025 Builders Companion
 */
package com.builderscompanion.core.datagen;

import com.builderscompanion.core.util.BCLogger;
import cy.jdkdigital.dyenamics.common.items.DyenamicDyeItem;
import cy.jdkdigital.dyenamics.core.util.DyenamicDyeColor;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Discovers all dye items and their colors from all mods.
 *
 * <p>Discovers vanilla DyeItem instances and, if Dyenamics is present,
 * also discovers DyenamicDyeItem instances.
 *
 * @since 1.0.0
 */
public class DyeDiscovery {

    /**
     * Discovers all dye items and returns a map of item ID to color hex value.
     *
     * @return map of dye item IDs to hex color values
     */
    public static Map<String, Integer> discoverDyes() {
        BCLogger.info("Discovering dye colors...");

        Map<String, Integer> dyes = new TreeMap<>();
        boolean dyenamicsLoaded = ModList.get().isLoaded("dyenamics");

        // Iterate through all registered items
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            String itemId = ForgeRegistries.ITEMS.getKey(item).toString();
            Integer colorValue = null;

            // Check for vanilla DyeItem
            if (item instanceof DyeItem dyeItem) {
                DyeColor dyeColor = dyeItem.getDyeColor();
                colorValue = dyeColor.getFireworkColor();
            }
            // Check for Dyenamics dyes (only if Dyenamics is loaded)
            else if (dyenamicsLoaded && item instanceof DyenamicDyeItem) {
                DyenamicDyeColor dyeColor = DyenamicDyeColor.getColor(item);
                if (dyeColor != null) {
                    colorValue = dyeColor.getColorValue();
                }
            }

            if (colorValue != null) {
                dyes.put(itemId, colorValue);
                BCLogger.debug("Found dye: {} with color #{}", itemId, String.format("%06X", colorValue));
            }
        }

        BCLogger.info("Discovered {} dye colors", dyes.size());
        writeDyesFile(dyes);
        return dyes;
    }

    /**
     * Writes the discovered dyes to a reference file.
     *
     * @param dyes map of dye item IDs to color values
     */
    private static void writeDyesFile(Map<String, Integer> dyes) {
        List<String> dyeLines = new ArrayList<>();
        dyeLines.add("# AUTO-GENERATED: All Discovered Dye Colors");
        dyeLines.add("# Reference this when configuring portal colors");
        dyeLines.add("# Format: item_id | hex_color");
        dyeLines.add("# Generated: " + java.time.LocalDateTime.now());
        dyeLines.add("");

        for (Map.Entry<String, Integer> entry : dyes.entrySet()) {
            String hexColor = String.format("#%06X", entry.getValue() & 0xFFFFFF);
            dyeLines.add(String.format("%s | %s", entry.getKey(), hexColor));
        }

        try {
            Path outputPath = Paths.get("config", "builderscompanion", "discovered_dyes.txt");
            Files.createDirectories(outputPath.getParent());
            Files.deleteIfExists(outputPath);
            Files.write(outputPath, dyeLines);

            BCLogger.info("Written {} dye colors to config/builderscompanion/discovered_dyes.txt", dyes.size());

        } catch (IOException e) {
            BCLogger.error("Failed to write discovered dyes file: {}", e.getMessage());
        }
    }
}