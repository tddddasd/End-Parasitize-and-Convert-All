package org.tdddd.epca.impl.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.tdddd.epca.impl.overworld.data.BiomassStorage;
import org.tdddd.epca.impl.overworld.data.EntityBiomassCountManager;

/**
 * {@code /epca_biomass_count} - reads and writes the biomass of an entity or a block.
 *
 * <p>Both sides go through {@link BiomassStorage}, so the clamp rules are the same ones the game uses
 * for players, mobs, the beckon core and the hive node.</p>
 */
public final class BiomassCountCommand {

    private BiomassCountCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("epca_biomass_count")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("entity")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.literal("get")
                                        .executes(ctx -> entityGet(ctx, EntityArgument.getEntity(ctx, "targets"))))
                                .then(Commands.literal("add")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                                .executes(ctx -> entityChange(ctx, EntityArgument.getEntity(ctx, "targets"),
                                                        IntegerArgumentType.getInteger(ctx, "amount")))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(ctx -> entityChange(ctx, EntityArgument.getEntity(ctx, "targets"),
                                                        -IntegerArgumentType.getInteger(ctx, "amount")))))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(ctx -> entitySet(ctx, EntityArgument.getEntity(ctx, "targets"),
                                                        IntegerArgumentType.getInteger(ctx, "amount")))))))
                .then(Commands.literal("block")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.literal("get")
                                        .executes(ctx -> blockGet(ctx)))
                                .then(Commands.literal("add")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                                .executes(ctx -> blockChange(ctx, IntegerArgumentType.getInteger(ctx, "amount")))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(ctx -> blockChange(ctx, -IntegerArgumentType.getInteger(ctx, "amount")))))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(ctx -> blockSet(ctx, IntegerArgumentType.getInteger(ctx, "amount"))))))));
    }

    private static int entityGet(CommandContext<CommandSourceStack> ctx, Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            ctx.getSource().sendFailure(Component.literal("target is not a living entity"));
            return 0;
        }
        int count = EntityBiomassCountManager.getCurrentBiomassCount(living);
        int max = EntityBiomassCountManager.getMaxBiomassCount(living);
        ctx.getSource().sendSuccess(() -> Component.literal(
                entity.getName().getString() + ": " + count + "/" + max), false);
        return count;
    }

    private static int entityChange(CommandContext<CommandSourceStack> ctx, Entity entity, int delta) {
        if (!(entity instanceof LivingEntity living)) {
            ctx.getSource().sendFailure(Component.literal("target is not a living entity"));
            return 0;
        }
        int before = EntityBiomassCountManager.getCurrentBiomassCount(living);
        int max = EntityBiomassCountManager.getMaxBiomassCount(living);
        EntityBiomassCountManager.setBiomassCount(living,
                Math.max(0, Math.min(before + delta, max)));
        int after = EntityBiomassCountManager.getCurrentBiomassCount(living);
        ctx.getSource().sendSuccess(() -> Component.literal(
                entity.getName().getString() + ": " + before + " -> " + after + "/" + max), false);
        return after;
    }

    private static int entitySet(CommandContext<CommandSourceStack> ctx, Entity entity, int value) {
        if (!(entity instanceof LivingEntity living)) {
            ctx.getSource().sendFailure(Component.literal("target is not a living entity"));
            return 0;
        }
        EntityBiomassCountManager.setBiomassCount(living, value);
        int after = EntityBiomassCountManager.getCurrentBiomassCount(living);
        int max = EntityBiomassCountManager.getMaxBiomassCount(living);
        ctx.getSource().sendSuccess(() -> Component.literal(
                entity.getName().getString() + " set to " + after + "/" + max), false);
        return after;
    }

    private static BiomassStorage storageAt(CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        ServerLevel level = ctx.getSource().getLevel();
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof BiomassStorage storage) {
            return storage;
        }
        ctx.getSource().sendFailure(Component.literal("block at " + pos + " has no biomass"));
        return null;
    }

    private static int blockGet(CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        BiomassStorage storage = storageAt(ctx);
        if (storage == null) return 0;
        int count = EntityBiomassCountManager.getCurrentBiomassCount(storage);
        int max = EntityBiomassCountManager.getMaxBiomassCount(storage);
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        ctx.getSource().sendSuccess(() -> Component.literal(
                pos + ": " + count + "/" + max), false);
        return count;
    }

    private static int blockChange(CommandContext<CommandSourceStack> ctx, int delta)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        BiomassStorage storage = storageAt(ctx);
        if (storage == null) return 0;
        int before = EntityBiomassCountManager.getCurrentBiomassCount(storage);
        EntityBiomassCountManager.addBiomassCount(storage, delta);
        int after = EntityBiomassCountManager.getCurrentBiomassCount(storage);
        int max = EntityBiomassCountManager.getMaxBiomassCount(storage);
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        ctx.getSource().sendSuccess(() -> Component.literal(
                pos + ": " + before + " -> " + after + "/" + max), false);
        return after;
    }

    private static int blockSet(CommandContext<CommandSourceStack> ctx, int value)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        BiomassStorage storage = storageAt(ctx);
        if (storage == null) return 0;
        EntityBiomassCountManager.setBiomassCount(storage, value);
        int after = EntityBiomassCountManager.getCurrentBiomassCount(storage);
        int max = EntityBiomassCountManager.getMaxBiomassCount(storage);
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        ctx.getSource().sendSuccess(() -> Component.literal(
                pos + " set to " + after + "/" + max), false);
        return after;
    }
}
