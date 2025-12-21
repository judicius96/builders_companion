/*
 * MIT License
 *
 * Copyright (c) 2025 Builders Companion
 */
package com.builderscompanion.core.datapack;

import com.builderscompanion.core.util.BCLogger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Generates datapack structure for Builders Companion mods.
 *
 * <p>Creates a datapack that Minecraft automatically loads.
 *
 * @since 1.0.0
 */
public class DatapackGenerator {

    private static final Path DATAPACK_DIR = Paths.get("config", "builderscompanion", "bc_datapack");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Initializes the datapack structure.
     *
     * @return the datapack root directory
     */
    public static Path initializeDatapack() {
        try {
            BCLogger.info("Initializing BC datapack structure...");

            // Create root structure
            Files.createDirectories(DATAPACK_DIR);
            Files.createDirectories(DATAPACK_DIR.resolve("data"));

            // Generate pack.mcmeta if it doesn't exist
            Path mcmetaPath = DATAPACK_DIR.resolve("pack.mcmeta");
            if (!Files.exists(mcmetaPath)) {
                generatePackMcmeta();
            }

            BCLogger.info("Datapack initialized at: {}", DATAPACK_DIR);
            return DATAPACK_DIR;

        } catch (IOException e) {
            BCLogger.error("Failed to initialize datapack: {}", e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets the datapack directory.
     *
     * @return the datapack root path
     */
    public static Path getDatapackDir() {
        return DATAPACK_DIR;
    }

    /**
     * Creates a namespace directory within the datapack.
     *
     * @param namespace the namespace (e.g., "bc_dimensions")
     * @param subdirs subdirectories to create (e.g., "dimension", "dimension_type")
     * @return the namespace data directory
     */
    public static Path createNamespace(String namespace, String... subdirs) throws IOException {
        Path namespacePath = DATAPACK_DIR.resolve("data").resolve(namespace);
        Files.createDirectories(namespacePath);

        for (String subdir : subdirs) {
            Files.createDirectories(namespacePath.resolve(subdir));
        }

        return namespacePath;
    }

    /**
     * Writes a JSON file to the datapack.
     *
     * @param relativePath path relative to datapack root (e.g., "data/bc_dimensions/dimension/sand_world.json")
     * @param json the JSON object to write
     */
    public static void writeJson(String relativePath, JsonObject json) throws IOException {
        Path fullPath = DATAPACK_DIR.resolve(relativePath);
        Files.createDirectories(fullPath.getParent());
        Files.writeString(fullPath, GSON.toJson(json));
        BCLogger.debug("Written datapack file: {}", relativePath);
    }

    /**
     * Generates pack.mcmeta file.
     */
    private static void generatePackMcmeta() throws IOException {
        JsonObject packMcmeta = new JsonObject();
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", 15); // Minecraft 1.20.1 uses pack format 15
        pack.addProperty("description", "Builders Companion - Auto-generated datapack");
        packMcmeta.add("pack", pack);

        Path mcmetaPath = DATAPACK_DIR.resolve("pack.mcmeta");
        Files.writeString(mcmetaPath, GSON.toJson(packMcmeta));

        BCLogger.debug("Generated pack.mcmeta");
    }

    /**
     * Clears all generated datapack files (keeps structure).
     */
    public static void clearDatapack() throws IOException {
        Path dataDir = DATAPACK_DIR.resolve("data");
        if (Files.exists(dataDir)) {
            Files.walk(dataDir)
                    .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            BCLogger.warn("Failed to delete: {}", path);
                        }
                    });
        }
        BCLogger.info("Cleared datapack content");
    }
}