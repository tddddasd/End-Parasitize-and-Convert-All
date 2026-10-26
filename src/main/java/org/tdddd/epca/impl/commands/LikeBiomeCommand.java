package org.tdddd.epca.impl.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import org.tdddd.epca.impl.overworld.data.LikeBiome;
import org.tdddd.epca.impl.overworld.data.LikeBiomeManager;
import org.tdddd.epca.impl.overworld.data.LikeBiomeSavedData;

/**
 * {@code /epca_biome_like} - stack a biome on top of the real one for the 4x4x4 section the command runs
 * in. The like biome never replaces the real one.
 */
public final class LikeBiomeCommand {

    private LikeBiomeCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("epca_biome_like")
                .requires(Commands.hasPermission(new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER)))
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
                .then(Commands.literal("remove").executes(LikeBiomeCommand::remove))
                .then(Commands.literal("removeall").executes(LikeBiomeCommand::removeAll))
                .then(Commands.literal("info").executes(LikeBiomeCommand::info)));
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
        String raw = StringArgumentType.getString(ctx, "biome");
        Identifier id = Identifier.tryParse(raw);
        if (id == null) {
            source.sendFailure(Component.literal("invalid biome id: " + raw));
            return 0;
        }
        if (LikeBiomeManager.resolve(level, id) == null) {
            source.sendFailure(Component.literal("biome does not exist: " + id));
            return 0;
        }
        LikeBiomeManager.set(level, pos, new LikeBiome(id, colors, rain, spawn));
        source.sendSuccess(() -> Component.literal(
                "like biome " + id + " stacked on section (" + (pos.getX() >> 2) + ", " + (pos.getY() >> 2)
                        + ", " + (pos.getZ() >> 2) + ")"
                        + " | colors=" + colors
                        + " | rain=" + rain
                        + " | spawn=" + LikeBiome.spawnModeName(spawn)), true);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        BlockPos pos = BlockPos.containing(source.getPosition());
        boolean removed = LikeBiomeManager.remove(source.getLevel(), pos);
        source.sendSuccess(() -> Component.literal(removed
                ? "removed the like biome of this section"
                : "this section has no like biome"), false);
        return removed ? 1 : 0;
    }

    private static int removeAll(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        int count = LikeBiomeManager.removeAll(source.getLevel());
        source.sendSuccess(() -> Component.literal("removed " + count + " like biome sections in this dimension"), true);
        return count;
    }

    private static int info(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        LikeBiome like = LikeBiomeManager.activeAt(level, pos);
        String real = level.getBiome(pos).unwrapKey()
                .map(key -> key.identifier().toString()).orElse("?");
        int total = LikeBiomeSavedData.get(level).size();
        source.sendSuccess(() -> Component.literal(
                "real biome: " + real + " | like biome: " + (like == null ? "none" : like.biomeId().toString())
                        + " | like biome sections here: " + total), false);
        return like == null ? 0 : 1;
    }
}
