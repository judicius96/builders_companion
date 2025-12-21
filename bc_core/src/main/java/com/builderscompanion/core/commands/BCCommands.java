/*
 * MIT License
 *
 * Copyright (c) 2025 Builders Companion
 */
package com.builderscompanion.core.commands;

import com.builderscompanion.core.BCCore;
import com.builderscompanion.core.config.BCConfigLoader;
import com.builderscompanion.core.util.BCLogger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;

@Mod.EventBusSubscriber(modid = BCCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BCCommands {

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("bc")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("reload")
                                .executes(BCCommands::reload))
                        .then(Commands.literal("version")
                                .executes(BCCommands::version))
                        .then(Commands.literal("dump")
                                .then(Commands.literal("biomes")
                                        .executes(BCCommands::dumpBiomes))
                                .then(Commands.literal("structures")
                                        .executes(BCCommands::dumpStructures))
                                .then(Commands.literal("entities")
                                        .executes(BCCommands::dumpEntities)))
        );
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal("§6Reloading Builders Companion configs..."), false);

        try {
            int loaded = BCConfigLoader.reloadAll();
            ctx.getSource().sendSuccess(() ->
                    Component.literal("§aReload complete! Loaded " + loaded + " dimensions"), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cReload failed: " + e.getMessage()));
            BCLogger.error("Config reload failed", e);
            return 0;
        }
    }

    private static int version(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() ->
                Component.literal("§6Builders Companion v1.0.0§r\n" +
                        "§7- BC:Core (loaded)\n" +
                        "§7- BC:Dimensions (loaded)"), false);
        return 1;
    }

    private static int dumpBiomes(CommandContext<CommandSourceStack> ctx) {
        try {
            Path output = Paths.get("config/builderscompanion/dumps/biomes.txt");
            Files.createDirectories(output.getParent());

            List<String> biomes = new ArrayList<>();
            for (ResourceLocation id : net.minecraftforge.registries.ForgeRegistries.BIOMES.getKeys()) {
                biomes.add(id.toString());
            }
            biomes.sort(String::compareTo);

            Files.write(output, biomes);

            ctx.getSource().sendSuccess(() ->
                    Component.literal("§aDumped " + biomes.size() + " biomes to config/builderscompanion/dumps/biomes.txt"), false);
            return 1;
        } catch (IOException e) {
            ctx.getSource().sendFailure(Component.literal("§cFailed to dump biomes: " + e.getMessage()));
            return 0;
        }
    }

    private static int dumpStructures(CommandContext<CommandSourceStack> ctx) {
        try {
            Path output = Paths.get("config/builderscompanion/dumps/structures.txt");
            Files.createDirectories(output.getParent());

            List<String> structures = BuiltInRegistries.STRUCTURE_TYPE.keySet().stream()
                    .map(ResourceLocation::toString)
                    .sorted()
                    .toList();

            Files.write(output, structures);

            ctx.getSource().sendSuccess(() ->
                    Component.literal("§aDumped " + structures.size() + " structures to config/builderscompanion/dumps/structures.txt"), false);
            return 1;
        } catch (IOException e) {
            ctx.getSource().sendFailure(Component.literal("§cFailed to dump structures: " + e.getMessage()));
            return 0;
        }
    }

    private static int dumpEntities(CommandContext<CommandSourceStack> ctx) {
        try {
            Path output = Paths.get("config/builderscompanion/dumps/entities.txt");
            Files.createDirectories(output.getParent());

            List<String> entities = BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                    .map(ResourceLocation::toString)
                    .sorted()
                    .toList();

            Files.write(output, entities);

            ctx.getSource().sendSuccess(() ->
                    Component.literal("§aDumped " + entities.size() + " entities to config/builderscompanion/dumps/entities.txt"), false);
            return 1;
        } catch (IOException e) {
            ctx.getSource().sendFailure(Component.literal("§cFailed to dump entities: " + e.getMessage()));
            return 0;
        }
    }
}