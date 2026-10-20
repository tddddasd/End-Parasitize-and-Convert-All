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

/**
 * {@code /epca_render <id> [pos]} - plays one of epca's render effects on the client.
 *
 * <p>Registered on the client command dispatcher, so the effect triggers can be called directly
 * without a sync packet. Effects that need a position accept one; the rest are global or screen space
 * effects and ignore it.</p>
 */
public final class EpcaRenderCommand {

    /** One render effect: its id, whether it takes a position, and how to trigger it. */
    private record Entry(String id, boolean positional, Consumer<CommandContext<CommandSourceStack>> global,
                         BiConsumer<CommandContext<CommandSourceStack>, Vec3> atPosition) {
    }

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    static {
        // id order matters: this is the order they are documented and listed in.
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
                        // erosion_stage_rise takes a stage instead of a position
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

    /** {@code erosion_stage_rise <stage>}: everything a world erosion stage rise plays. */
    private static int runStage(CommandContext<CommandSourceStack> ctx, String id, int stage) {
        if (!"erosion_stage_rise".equals(id)) {
            ctx.getSource().sendFailure(Component.literal(
                    "render id '" + id + "' does not take a stage"));
            return 0;
        }
        return runErosionStageRise(ctx, stage);
    }

    private static int runErosionStageRise(CommandContext<CommandSourceStack> ctx, int stage) {
        // id 7: the group of effects a stage rise plays. Filled in as those effects are wired up.
        org.tdddd.epca.impl.client.render.sky.SkyRuptureEffect.trigger(stage);
        ctx.getSource().sendSuccess(() -> Component.literal("erosion stage rise at stage " + stage), false);
        return 1;
    }
}
