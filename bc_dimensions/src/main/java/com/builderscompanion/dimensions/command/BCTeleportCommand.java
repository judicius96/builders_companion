/*
 * MIT License
 *
 * Copyright (c) 2025 Builders Companion
 */
package com.builderscompanion.dimensions.command;

import com.builderscompanion.dimensions.registry.BCDimensionRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Command to teleport to custom dimensions.
 *
 * <p>Usage: {@code /bc tp <dimension_id>}
 *
 * @since 1.0.0
 */
public class BCTeleportCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("bc")
                        .then(Commands.literal("tp")
                                .then(Commands.argument("dimension", StringArgumentType.string())
                                        .executes(BCTeleportCommand::teleport)))
        );
    }

    private static int teleport(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        // Must be a player
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can teleport"));
            return 0;
        }

        String dimensionId = StringArgumentType.getString(ctx, "dimension");
        ResourceKey<Level> dimensionKey = BCDimensionRegistry.getDimensionKey(dimensionId);

        if (dimensionKey == null) {
            source.sendFailure(Component.literal("§cDimension '" + dimensionId + "' not found"));
            return 0;
        }

        ServerLevel targetLevel = source.getServer().getLevel(dimensionKey);

        if (targetLevel == null) {
            source.sendFailure(Component.literal("§cDimension '" + dimensionId + "' is not loaded"));
            return 0;
        }

        // Teleport to spawn point of dimension
        BlockPos spawnPos = targetLevel.getSharedSpawnPos();
        player.teleportTo(targetLevel, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), 0, 0);

        source.sendSuccess(() -> Component.literal("§aTeleported to " + dimensionId), true);
        return 1;
    }
}
