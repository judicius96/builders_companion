package com.builderscompanion.core.datagen;

import com.builderscompanion.core.BCCore;
import com.builderscompanion.core.BCTags;
import com.builderscompanion.core.util.BCLogger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
import net.minecraft.data.tags.TagsProvider.TagAppender;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class BCTintedFluidTagsProvider extends FluidTagsProvider {

    // Adjust to match your actual file location.
    // You mentioned you currently see data/bccore/registered_colors.txt.
    private static final String[] MASTER_PATH_CANDIDATES = new String[] {
            "/data/bc_core/water_colors/registered_colors.txt"
    };

    private static final int VANILLA_DEFAULT_WATER = 0x3F76E4;

    public BCTintedFluidTagsProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            ExistingFileHelper existingFileHelper
    ) {
        super(output, lookupProvider, BCCore.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        TagAppender<Fluid> waterTag = tag(FluidTags.WATER);
        TagAppender<Fluid> waterLikeTag = tag(BCTags.Fluids.WATER_LIKE);

        // Build rgb -> typeId in file order (first time we see a color assigns typeId)
        Map<Integer, Integer> rgbToTypeId = loadUniqueColorTypeIds();
        if (rgbToTypeId.isEmpty()) {
            BCLogger.warn("Datagen: no colors found in master list; tags will be empty.");
            return;
        }

        for (int typeId : rgbToTypeId.values()) {
            String idSuffix = String.format("%03d", typeId);

            ResourceLocation stillId = new ResourceLocation(BCCore.MODID, "tinted_water_still_" + idSuffix);
            ResourceLocation flowingId = new ResourceLocation(BCCore.MODID, "tinted_water_flowing_" + idSuffix);

            waterTag.addOptional(stillId);
            waterTag.addOptional(flowingId);

            waterLikeTag.addOptional(stillId);
            waterLikeTag.addOptional(flowingId);
        }

        BCLogger.info("Datagen: tagged {} tinted water variants (still+flowing in both tags).", rgbToTypeId.size());
    }

    private Map<Integer, Integer> loadUniqueColorTypeIds() {
        InputStream stream = null;
        String chosenPath = null;

        for (String p : MASTER_PATH_CANDIDATES) {
            stream = BCTintedFluidTagsProvider.class.getResourceAsStream(p);
            if (stream != null) {
                chosenPath = p;
                break;
            }
        }

        if (stream == null) {
            BCLogger.error("Datagen: could not find master color list at any known path.");
            for (String p : MASTER_PATH_CANDIDATES) {
                BCLogger.error("  Tried: {}", p);
            }
            return Map.of();
        }

        // Preserve file order
        Map<Integer, Integer> rgbToTypeId = new LinkedHashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;

            // Skip header (matches your existing format)
            reader.readLine();

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\|");
                if (parts.length < 3) continue;

                String hexColor = parts[1].trim();

                int rgb;
                try {
                    rgb = Integer.parseInt(hexColor.replace("0x", ""), 16);
                } catch (NumberFormatException ignored) {
                    continue;
                }

                if (rgb == VANILLA_DEFAULT_WATER) continue;

                if (!rgbToTypeId.containsKey(rgb)) {
                    rgbToTypeId.put(rgb, rgbToTypeId.size());
                }
            }
        } catch (Exception e) {
            BCLogger.error("Datagen: failed reading master list {}: {}", chosenPath, e.getMessage());
            return Map.of();
        }

        return rgbToTypeId;
    }
}
