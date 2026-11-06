package org.tdddd.epca.impl.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class EpcaRenderCommand {

    private record Entry(String id, boolean positional, Consumer<CommandContext<CommandSourceStack>> global,
                         BiConsumer<CommandContext<CommandSourceStack>, Vec3> atPosition) {
    }

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    static {

        register(new Entry("sky_rupture", false,
                ctx -> org.tdddd.epca.impl.client.render.sky.SkyRuptureEffect.trigger(13),
                null));
    }

    private static void register(Entry entry) {
        ENTRIES.put(entry.id(), entry);
    }

    private EpcaRenderCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("epca_render")
                .requires(source -> source.hasPermission(0))
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            ENTRIES.keySet().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(ctx -> run(ctx, StringArgumentType.getString(ctx, "id"), null))

                        .then(Commands.argument("stage", IntegerArgumentType.integer(-2, 13))
                                .executes(ctx -> runStage(ctx, StringArgumentType.getString(ctx, "id"),
                                        IntegerArgumentType.getInteger(ctx, "stage"))))
                        .then(Commands.argument("pos", Vec3Argument.vec3())
                                .executes(ctx -> run(ctx, StringArgumentType.getString(ctx, "id"),
                                        Vec3Argument.getVec3(ctx, "pos"))))));
    }

    private static int run(CommandContext<CommandSourceStack> ctx, String id, Vec3 pos) {
        Entry entry = ENTRIES.get(id);
        if (entry == null) {
            ctx.getSource().sendFailure(Component.literal("unknown render id '" + id + "', available: "
                    + String.join(", ", ENTRIES.keySet())));
            return 0;
        }
        try {
            if (pos != null) {
                if (!entry.positional() || entry.atPosition() == null) {
                    ctx.getSource().sendFailure(Component.literal(
                            "render id '" + id + "' does not take a position"));
                    return 0;
                }
                entry.atPosition().accept(ctx, pos);
            } else {
                entry.global().accept(ctx);
            }
            ctx.getSource().sendSuccess(() -> Component.literal("played render effect '" + id + "'"), false);
            return 1;
        } catch (RuntimeException e) {
            ctx.getSource().sendFailure(Component.literal("render id '" + id + "' failed: " + e));
            return 0;
        }
    }

    private static int runStage(CommandContext<CommandSourceStack> ctx, String id, int stage) {
        if (!"erosion_stage_rise".equals(id)) {
            ctx.getSource().sendFailure(Component.literal(
                    "render id '" + id + "' does not take a stage"));
            return 0;
        }
        return runErosionStageRise(ctx, stage);
    }

    private static int runErosionStageRise(CommandContext<CommandSourceStack> ctx, int stage) {

        org.tdddd.epca.impl.client.render.sky.SkyRuptureEffect.trigger(stage);
        ctx.getSource().sendSuccess(() -> Component.literal("erosion stage rise at stage " + stage), false);
        return 1;
    }
}
