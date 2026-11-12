package org.tdddd.epca.impl.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.tdddd.epca.impl.overworld.data.LikeBiome;
import org.tdddd.epca.impl.overworld.data.LikeBiomeManager;
import org.tdddd.epca.impl.overworld.data.LikeBiomeSavedData;

public final class LikeBiomeCommand {

    private LikeBiomeCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("epca_biome_like")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("set")
                        .then(Commands.argument("biome", StringArgumentType.string())
                                .executes(ctx -> set(ctx, true, false, LikeBiome.SPAWN_OVERRIDE))
                                .then(Commands.argument("colors", IntegerArgumentType.integer(0, 1))
                                        .executes(ctx -> set(ctx, flag(ctx, "colors"), false, LikeBiome.SPAWN_OVERRIDE))
                                        .then(Commands.argument("rain", IntegerArgumentType.integer(0, 1))
                                                .executes(ctx -> set(ctx, flag(ctx, "colors"), flag(ctx, "rain"),
                                                        LikeBiome.SPAWN_OVERRIDE))
                                                .then(Commands.argument("spawn", StringArgumentType.string())
                                                        .executes(ctx -> set(ctx, flag(ctx, "colors"), flag(ctx, "rain"),
                                                                spawnMode(StringArgumentType.getString(ctx, "spawn")))))))))
                .then(Commands.literal("stage")
                        .then(Commands.literal("inherit")
                                .executes(LikeBiomeCommand::stageInherit))
                        .then(Commands.argument("stage", IntegerArgumentType.integer(-2, 64))
                                .executes(ctx -> stage(ctx, IntegerArgumentType.getInteger(ctx, "stage")))))
                .then(Commands.literal("remove")
                        .executes(LikeBiomeCommand::remove))
                .then(Commands.literal("removeall")
                        .executes(LikeBiomeCommand::removeAll))
                .then(Commands.literal("info")
                        .executes(LikeBiomeCommand::info)));
    }

    private static boolean flag(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            return IntegerArgumentType.getInteger(ctx, name) != 0;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static int spawnMode(String name) {
        return switch (name.toLowerCase(java.util.Locale.ROOT)) {
            case "coexist", "both", "add" -> LikeBiome.SPAWN_COEXIST;
            case "none", "off", "false" -> LikeBiome.SPAWN_NONE;
            default -> LikeBiome.SPAWN_OVERRIDE;
        };
    }

    /** Human readable unit coordinates: the like biome unit is a 4x4x4 block volume. */
    private static String unitLabel(BlockPos pos) {
        return LikeBiomeManager.unitCoord(pos.getX()) + ", "
                + LikeBiomeManager.unitCoord(pos.getY()) + ", "
                + LikeBiomeManager.unitCoord(pos.getZ());
    }

    private static String blockLabel(BlockPos pos) {
        return (LikeBiomeManager.unitStart(LikeBiomeManager.unitCoord(pos.getX()))) + ", "
                + (LikeBiomeManager.unitStart(LikeBiomeManager.unitCoord(pos.getY()))) + ", "
                + (LikeBiomeManager.unitStart(LikeBiomeManager.unitCoord(pos.getZ())));
    }

    private static int set(CommandContext<CommandSourceStack> ctx, boolean colors, boolean rain, int spawn) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());

        ResourceLocation id = ResourceLocation.tryParse(StringArgumentType.getString(ctx, "biome"));
        if (id == null) {
            source.sendFailure(Component.literal("无效的生物群系 id: " + StringArgumentType.getString(ctx, "biome")));
            return 0;
        }

        if (LikeBiomeManager.resolve(level, id) == null) {
            source.sendFailure(Component.literal("生物群系不存在: " + id));
            return 0;
        }

        LikeBiomeManager.set(level, pos, new LikeBiome(id, colors, rain, spawn));
        source.sendSuccess(() -> Component.literal(
                "已将群系类群 " + id + " 叠加到单元 [" + unitLabel(pos) + "]（4×4×4 方块，起始 "
                        + blockLabel(pos) + "）"
                        + "｜颜色=" + (colors ? "覆盖" : "不覆盖")
                        + "｜降雨=" + (rain ? "覆盖" : "不覆盖")
                        + "｜渲染: 侵蚀阶段<" + LikeBiome.FULL_COLOR_STAGE + " 仅水色，>="
                        + LikeBiome.FULL_COLOR_STAGE + " 水色+天空+方块"), true);
        return 1;
    }

    private static int stage(CommandContext<CommandSourceStack> ctx, int stage) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        long key = LikeBiomeManager.unitKey(pos);
        LikeBiomeSavedData data = LikeBiomeSavedData.get(level);
        LikeBiome current = data.atKey(key);
        if (current == null) {
            source.sendFailure(Component.literal("单元 [" + unitLabel(pos) + "] 没有群系类群"));
            return 0;
        }
        data.set(key, current.withStage(stage));
        LikeBiomeManager.markUnitChanged(level, key);
        source.sendSuccess(() -> Component.literal("单元 [" + unitLabel(pos) + "] 的侵蚀阶段设为 " + stage
                + (stage >= LikeBiome.FULL_COLOR_STAGE ? "（水色+天空+方块颜色）" : "（仅水色）")), true);
        return 1;
    }

    private static int stageInherit(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        long key = LikeBiomeManager.unitKey(pos);
        LikeBiomeSavedData data = LikeBiomeSavedData.get(level);
        LikeBiome current = data.atKey(key);
        if (current == null) {
            source.sendFailure(Component.literal("单元 [" + unitLabel(pos) + "] 没有群系类群"));
            return 0;
        }
        data.set(key, current.withStage(LikeBiome.STAGE_INHERIT));
        LikeBiomeManager.markUnitChanged(level, key);
        source.sendSuccess(() -> Component.literal(
                "单元 [" + unitLabel(pos) + "] 的侵蚀阶段改为跟随维度阶段（当前 "
                        + LikeBiomeManager.dimensionStage(level) + "）"), true);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        BlockPos pos = BlockPos.containing(source.getPosition());
        boolean removed = LikeBiomeManager.remove(source.getLevel(), pos);
        source.sendSuccess(() -> Component.literal(removed
                ? "已移除单元 [" + unitLabel(pos) + "] 的群系类群"
                : "该单元没有群系类群"), false);
        return removed ? 1 : 0;
    }

    private static int removeAll(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        int count = LikeBiomeManager.removeAll(source.getLevel());
        source.sendSuccess(() -> Component.literal("已移除该维度全部群系类群，共 " + count + " 个单元"), true);
        return count;
    }

    private static int info(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        LikeBiomeSavedData data = LikeBiomeSavedData.get(level);
        long key = LikeBiomeManager.unitKey(pos);
        LikeBiome like = LikeBiomeManager.activeAt(level, pos);
        String realBiome = level.getBiome(pos).unwrapKey()
                .map(k -> k.location().toString()).orElse("?");
        int dimensionStage = LikeBiomeManager.dimensionStage(level);
        String stageText = like == null ? "-" : (like.hasExplicitStage()
                ? like.erosionStage() + "（固定）"
                : dimensionStage + "（跟随维度）");
        boolean infested = Boolean.TRUE.equals(LikeBiomeManager.unitHasInfestedBlock(level, key));
        source.sendSuccess(() -> Component.literal(
                "原群系: " + realBiome
                        + "｜群系类群: " + (like == null ? "无" : like.biomeId().toString())
                        + "｜单元: [" + unitLabel(pos) + "] 4×4×4"
                        + "｜侵蚀阶段: " + stageText
                        + "｜单元内含感染方块: " + (infested ? "是" : "否")
                        + "｜自动管理: " + (data.isManaged(key) ? "是" : "否")
                        + "｜该维度群系类群单元数: " + data.size()), false);
        return like == null ? 0 : 1;
    }
}
