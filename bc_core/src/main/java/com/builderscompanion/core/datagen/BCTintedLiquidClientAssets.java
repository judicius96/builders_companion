package com.builderscompanion.core.datagen;

import com.builderscompanion.core.BCCore;
import com.builderscompanion.core.registry.WaterColorRegistry;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class BCTintedLiquidClientAssets implements DataProvider {

    private final PackOutput output;

    public BCTintedLiquidClientAssets(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<WaterColorRegistry.ColorEntry> colors = WaterColorRegistry.getUniqueColors();

        CompletableFuture<?>[] futures = new CompletableFuture<?>[colors.size() * 2];
        int idx = 0;

        for (WaterColorRegistry.ColorEntry entry : colors) {
            int typeId = entry.typeId;
            String idSuffix = String.format("%03d", typeId);

            String blockName = "tinted_water_block_" + idSuffix;
            String stillFluidName = "tinted_water_still_" + idSuffix;

            // ---- model JSON (forge:fluid loader) ----
            String modelJson = "{\n" +
                    "  \"loader\": \"forge:fluid\",\n" +
                    "  \"fluid\": \"" + new ResourceLocation(BCCore.MODID, stillFluidName) + "\"\n" +
                    "}\n";

            // ---- blockstate JSON with variants for level=0..15 ----
            StringBuilder variants = new StringBuilder();
            variants.append("{\n  \"variants\": {\n");
            for (int level = 0; level <= 15; level++) {
                variants.append("    \"level=").append(level).append("\": { \"model\": \"")
                        .append(BCCore.MODID).append(":block/").append(blockName).append("\" }");
                variants.append(level == 15 ? "\n" : ",\n");
            }
            variants.append("  }\n}\n");

            Path modelPath = output.getOutputFolder()
                    .resolve("assets/" + BCCore.MODID + "/models/block/" + blockName + ".json");
            Path blockstatePath = output.getOutputFolder()
                    .resolve("assets/" + BCCore.MODID + "/blockstates/" + blockName + ".json");

            futures[idx++] = DataProvider.saveStable(cache, JsonParser.parseString(modelJson), modelPath);
            futures[idx++] = DataProvider.saveStable(cache, JsonParser.parseString(modelJson), modelPath);
        }

        return CompletableFuture.allOf(futures);
    }

    @Override
    public String getName() {
        return "BC Core - Tinted Liquid Client Assets (models + blockstates)";
    }
}
