package org.tdddd.epca.impl.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.tdddd.epca.impl.overworld.data.EvolutionDataStorage;
import org.tdddd.epca.impl.overworld.data.EvolutionManager;
import org.tdddd.epca.impl.ModConfig;

public class EvolutionCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("epca_evolution")
                .requires(source -> source.hasPermission(2))

                .then(Commands.literal("status")
                        .executes(context -> showStatus(context, null))
                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                                .executes(context -> showStatus(context, DimensionArgument.getDimension(context, "dimension")))))

                .then(Commands.literal("setpoints")
                        .then(Commands.argument("points", IntegerArgumentType.integer(-100, 2100000000))
                                .executes(context -> setPoints(context, IntegerArgumentType.getInteger(context, "points"), null))
                                .then(Commands.argument("dimension", DimensionArgument.dimension())
                                        .executes(context -> setPoints(context,
                                                IntegerArgumentType.getInteger(context, "points"),
                                                DimensionArgument.getDimension(context, "dimension"))))))

                .then(Commands.literal("setstage")
                .then(Commands.argument("stage", IntegerArgumentType.integer(-2, 13))
                        .executes(context -> setStage(context, IntegerArgumentType.getInteger(context, "stage"), null))
                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                                .executes(context -> setStage(context,
                                        IntegerArgumentType.getInteger(context, "stage"),
                                        DimensionArgument.getDimension(context, "dimension"))))))

                .then(Commands.literal("add")
                        .then(Commands.argument("points", IntegerArgumentType.integer())
                                .executes(context -> addPoints(context, IntegerArgumentType.getInteger(context, "points"), null))
                                .then(Commands.argument("dimension", DimensionArgument.dimension())
                                        .executes(context -> addPoints(context,
                                                IntegerArgumentType.getInteger(context, "points"),
                                                DimensionArgument.getDimension(context, "dimension"))))))

                .then(Commands.literal("reset")
                        .executes(context -> resetDimension(context, null))
                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                                .executes(context -> resetDimension(context, DimensionArgument.getDimension(context, "dimension"))))
                        .then(Commands.literal("all")
                                .executes(context -> resetAllDimensions(context))))

                .then(Commands.literal("thresholds")
                        .executes(EvolutionCommand::showThresholds)));
    }

    private static int showStatus(CommandContext<CommandSourceStack> context, ServerLevel dimension) {
        ServerLevel targetLevel = dimension != null ? dimension : context.getSource().getLevel();
        EvolutionManager manager = new EvolutionManager(targetLevel,
                EvolutionDataStorage.get(targetLevel).getPointsForDimension(targetLevel.dimension()));

        int points = manager.getPoints();
        int stage = manager.getStage();

        Component message = Component.literal("[世界侵蚀度] 当前维度: " + manager.getDimensionName() +
                "\n侵蚀点数: " + points +
                ", 阶段: " + stage);

        if (stage >= 0) {
            message = message.copy().append(" (")
                    .append(EvolutionManager.getStageDisplayName(stage))
                    .append(")");
        }

        Component finalMessage = message;
        context.getSource().sendSuccess(() -> finalMessage, false);
        return 0;
    }

    private static int setPoints(CommandContext<CommandSourceStack> context, int points, ServerLevel dimension) {
        ServerLevel targetLevel = dimension != null ? dimension : context.getSource().getLevel();
        EvolutionManager manager = EvolutionManager.forDimension(targetLevel);
        int oldPoints = manager.getPoints();
        int oldStage = manager.getStage();

        manager.setPoints(points);
        int newStage = manager.getStage();

        context.getSource().sendSuccess(() -> Component.literal("[世界侵蚀度] 已将 " + manager.getDimensionName() +
                        " 的侵蚀点数从 " + oldPoints +
                        " 设置为 " + points +
                        (newStage != oldStage ? " (阶段变化: " + oldStage + " → " + newStage + ")" : "")),
                true);
        return 1;
    }

    private static int setStage(CommandContext<CommandSourceStack> context, int stage, ServerLevel dimension) {
        ServerLevel targetLevel = dimension != null ? dimension : context.getSource().getLevel();
        EvolutionManager manager = EvolutionManager.forDimension(targetLevel);

        if (stage >= 11) {
            // 强制阶段：保存覆盖
            manager.setOverriddenStage(stage);
            context.getSource().sendSuccess(() -> Component.literal("[世界侵蚀度] 已将 " + manager.getDimensionName() +
                            " 的侵蚀阶段强制设置为 " + stage),
                    true);
            return 1;
        } else {
            // 普通阶段：清除强制，设置点数
            manager.clearOverriddenStage();
            double[] thresholds = ModConfig.getStageThresholds();
            int index = stage + 2;
            if (index < 0 || index >= thresholds.length) {
                context.getSource().sendFailure(Component.literal("[世界侵蚀度] 无效的阶段值: " + stage));
                return 0;
            }
            int requiredPoints = (int) Math.round(thresholds[index]);
            int oldStage = manager.getStage();
            manager.setPoints(requiredPoints);
            int newStage = manager.getStage();
            context.getSource().sendSuccess(() -> Component.literal("[世界侵蚀度] 已将 " + manager.getDimensionName() +
                            " 的侵蚀阶段设置为 " + stage + " (对应侵蚀点数: " + requiredPoints + ")" +
                            (newStage != oldStage ? " (侵蚀阶段变化: " + oldStage + " → " + newStage + ")" : "")),
                    true);
            return 1;
        }
    }

    private static int addPoints(CommandContext<CommandSourceStack> context, int points, ServerLevel dimension) {
        ServerLevel targetLevel = dimension != null ? dimension : context.getSource().getLevel();
        EvolutionManager manager = EvolutionManager.forDimension(targetLevel);
        int oldPoints = manager.getPoints();
        int oldStage = manager.getStage();

        manager.addPoints(points);
        int newStage = manager.getStage();

        context.getSource().sendSuccess(() -> Component.literal("[世界侵蚀度] 已为 " + manager.getDimensionName() +
                        " " + (points >= 0 ? "增加" : "减少") +
                        " " + Math.abs(points) +
                        " 侵蚀点数\n旧侵蚀点数: " + oldPoints +
                        " → 新侵蚀点数: " + manager.getPoints() +
                        (newStage != oldStage ? " (阶段变化: " + oldStage + " → " + newStage + ")" : "")),
                true);
        return 1;
    }

    private static int resetDimension(CommandContext<CommandSourceStack> context, ServerLevel dimension) {
        ServerLevel targetLevel = dimension != null ? dimension : context.getSource().getLevel();
        EvolutionDataStorage storage = EvolutionDataStorage.get(targetLevel);
        storage.resetDimension(targetLevel.dimension());
        storage.clearOverriddenStage(targetLevel.dimension());
        context.getSource().sendSuccess(() -> Component.literal("[世界侵蚀度] 已重置 " +
                        getDimensionName(targetLevel) +
                        " 的侵蚀度状态为默认值"),
                true);
        return 1;
    }

    private static int resetAllDimensions(CommandContext<CommandSourceStack> context) {
        ServerLevel overworld = context.getSource().getServer().overworld();
        EvolutionDataStorage storage = EvolutionDataStorage.get(overworld);
        storage.resetAllDimensions();
        storage.getOverriddenDimensions().forEach(storage::clearOverriddenStage);
        context.getSource().sendSuccess(() -> Component.literal("[世界侵蚀度] 已重置所有维度的侵蚀度状态为默认值"),
                true);
        return 1;
    }

    private static int showThresholds(CommandContext<CommandSourceStack> context) {
        StringBuilder sb = new StringBuilder();
        sb.append("[世界侵蚀度] 侵蚀度阶段阈值:\n");

        int[] thresholds = EvolutionManager.STAGE_THRESHOLDS;
        for (int i = 0; i < thresholds.length; i++) {
            int stage = i - 2;
            sb.append("阶段 ").append(stage).append(": ≥ ").append(thresholds[i]).append("\n");
        }

        context.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static String getDimensionName(ServerLevel level) {
        if (level.dimension().equals(Level.OVERWORLD)) return "主世界";
        if (level.dimension().equals(Level.NETHER)) return "下界";
        if (level.dimension().equals(Level.END)) return "末地";
        return level.dimension().location().toString();
    }
}