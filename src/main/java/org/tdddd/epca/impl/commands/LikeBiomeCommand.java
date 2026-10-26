package org.tdddd.epca.impl.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.tdddd.epca.impl.overworld.data.LikeBiome;
import org.tdddd.epca.impl.overworld.data.LikeBiomeManager;
import org.tdddd.epca.impl.overworld.data.LikeBiomeSavedData;

/**
 * {@code /epca_like_biome} - stack a biome on top of the real one for the chunk the command runs in.
 *
 * <pre>
 * /epca_like_biome set &lt;biome&gt; [colors] [rain] [spawn]
 * /epca_like_biome remove
 * /epca_like_biome removeall
 * /epca_like_biome info
 * </pre>
 *
 * <p>{@code colors}, {@code rain} are booleans; {@code spawn} is {@code override}, {@code coexist} or
 * {@code none}. The like biome only layers properties on top: the real biome of the chunk is untouched.</p>
 */
public final class LikeBiomeCommand {

    private LikeBiomeCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("epca_like_biome")
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

    private static int set(CommandContext<CommandSourceStack> ctx, boolean colors, boolean rain, int spawn) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());

        ResourceLocation id = ResourceLocation.tryParse(StringArgumentType.getString(ctx, "biome"));
        if (id == null) {
            source.sendFailure(Component.literal("无效的生物群系 id: " + StringArgumentType.getString(ctx, "biome")));
            return 0;
        }
        // the like biome must be a real biome, otherwise nothing could be overlaid
        if (LikeBiomeManager.resolve(level, id) == null) {
            source.sendFailure(Component.literal("生物群系不存在: " + id));
            return 0;
        }

        LikeBiomeManager.set(level, pos, new LikeBiome(id, colors, rain, spawn));
        source.sendSuccess(() -> Component.literal(
                "已将类群系 " + id + " 叠加到区块 (" + (pos.getX() >> 4) + ", " + (pos.getZ() >> 4) + ")"
                        + "｜颜色=" + (colors ? "覆盖" : "不覆盖")
                        + "｜降雨=" + (rain ? "覆盖" : "不覆盖")
                        + "｜生物生成=" + LikeBiome.spawnModeName(spawn)), true);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        BlockPos pos = BlockPos.containing(source.getPosition());
        boolean removed = LikeBiomeManager.remove(source.getLevel(), pos);
        source.sendSuccess(() -> Component.literal(removed
                ? "已移除区块 (" + (pos.getX() >> 4) + ", " + (pos.getZ() >> 4) + ") 的类群系"
                : "该区块没有类群系"), false);
        return removed ? 1 : 0;
    }

    private static int removeAll(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        int count = LikeBiomeManager.removeAll(source.getLevel());
        source.sendSuccess(() -> Component.literal("已移除该维度全部类群系，共 " + count + " 个区块"), true);
        return count;
    }

    private static int info(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        LikeBiomeSavedData data = LikeBiomeSavedData.get(level);
        LikeBiome like = LikeBiomeManager.activeAt(level, pos);
        String realBiome = level.getBiome(pos).unwrapKey()
                .map(key -> key.location().toString()).orElse("?");
        source.sendSuccess(() -> Component.literal(
                "原群系: " + realBiome + "｜类群系: " + (like == null ? "无" : like.biomeId().toString())
                        + "｜该维度类群系区块数: " + data.size()), false);
        return like == null ? 0 : 1;
    }
}
