/*
 * MIT License
 *
 * Copyright (c) 2025 Builders Companion
 */
package com.builderscompanion.core.datagen;

import com.builderscompanion.core.util.BCLogger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.registries.Registries;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Discovers all biomes and their water tint colors.
 *
 * <p>Maps each registered biome to its water color value for reference.
 *
 * @since 1.0.0
 */
public class BiomeTintDiscovery {

    /**
     * Discovers all biomes and their water colors.
     *
     * @return map of biome ID to water color hex value
     */
    public static Map<String, Integer> discoverBiomeWaterColors(MinecraftServer server) {
        BCLogger.info("Discovering biome water colors...");

        Map<String, Integer> biomeWaterColors = new TreeMap<>();

        // Access biomes from server's registry
        var biomeRegistry = server.registryAccess().registryOrThrow(Registries.BIOME);

        System.out.println("=== SERVER BIOME REGISTRY SIZE: " + biomeRegistry.size() + " ===");

        for (var entry : biomeRegistry.entrySet()) {
            ResourceLocation biomeId = entry.getKey().location();
            Biome biome = entry.getValue();

            int waterColor = biome.getWaterColor();
            biomeWaterColors.put(biomeId.toString(), waterColor);
            BCLogger.debug("Biome: {} → Water Color: #{}", biomeId, String.format("%06X", waterColor));
        }

        BCLogger.info("Discovered {} biome water colors", biomeWaterColors.size());
        writeBiomeWaterColorsFile(biomeWaterColors);
        return biomeWaterColors;
    }

    /**
     * Writes the discovered biome water colors to a reference file.
     *
     * @param biomeWaterColors map of biome IDs to water color values
     */
    private static void writeBiomeWaterColorsFile(Map<String, Integer> biomeWaterColors) {
        List<String> lines = new ArrayList<>();
        lines.add("# AUTO-GENERATED: Biome Water Colors");
        lines.add("# Reference for biome water tint values");
        lines.add("# Format: biome_id | hex_color");
        lines.add("# Generated: " + java.time.LocalDateTime.now());
        lines.add("");

        for (Map.Entry<String, Integer> entry : biomeWaterColors.entrySet()) {
            String hexColor = String.format("#%06X", entry.getValue() & 0xFFFFFF);
            lines.add(String.format("%s | %s", entry.getKey(), hexColor));
        }

        try {
            Path outputPath = Paths.get("config", "builderscompanion", "biome_water_colors.txt");
            Files.createDirectories(outputPath.getParent());
            Files.deleteIfExists(outputPath);
            Files.write(outputPath, lines);

            BCLogger.info("Written biome water colors to config/builderscompanion/biome_water_colors.txt");

        } catch (IOException e) {
            BCLogger.error("Failed to write biome water colors file: {}", e.getMessage());
        }
    }
}