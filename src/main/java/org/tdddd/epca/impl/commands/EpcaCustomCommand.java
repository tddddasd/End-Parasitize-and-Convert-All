package org.tdddd.epca.impl.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.overworld.data.WorldDifficultyData;
import org.tdddd.epca.impl.overworld.difficulty.DifficultyLevel;
import org.tdddd.epca.impl.epca;

@Mod.EventBusSubscriber(modid = epca.MODID)
public class EpcaCustomCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal("epca_custom")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("difficulty")
                                .then(Commands.argument("level", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            builder.suggest("easy");
                                            builder.suggest("normal");
                                            builder.suggest("expert");
                                            builder.suggest("master");
                                            return builder.buildFuture();
                                        })
                                        .executes(EpcaCustomCommand::setCustomDifficulty)))
                        .then(Commands.literal("spawnrate")
                                .then(Commands.argument("rate", FloatArgumentType.floatArg(0, 5))
                                        .executes(EpcaCustomCommand::setCustomSpawnRate)))
                        .then(Commands.literal("reward")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(EpcaCustomCommand::setCustomReward)))
                        .executes(EpcaCustomCommand::showHelp)
        );
    }

    private static int setCustomDifficulty(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String levelName = StringArgumentType.getString(ctx, "level");
        DifficultyLevel newBase = switch (levelName) {
            case "easy" -> DifficultyLevel.EASY;
            case "normal" -> DifficultyLevel.NORMAL;
            case "expert" -> DifficultyLevel.EXPERT;
            case "master" -> DifficultyLevel.MASTER;
            default -> null;
        };
        if (newBase == null) {
            ctx.getSource().sendFailure(Component.literal("无效的难度，可选: easy, normal, expert, master"));
            return 0;
        }

        ServerLevel level = ctx.getSource().getLevel();
        WorldDifficultyData data = WorldDifficultyData.get(level);
        if (data.getDifficulty() != DifficultyLevel.CUSTOM) {
            ctx.getSource().sendFailure(Component.literal("当前世界不是自定义难度"));
            return 0;
        }

        data.setCustomBaseDifficulty(newBase);
        ctx.getSource().sendSuccess(() -> Component.literal("已设置自定义难度基础难度为: " + newBase.getName()), true);
        return 1;
    }

    private static int setCustomSpawnRate(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        float rate = FloatArgumentType.getFloat(ctx, "rate");
        ServerLevel level = ctx.getSource().getLevel();
        WorldDifficultyData data = WorldDifficultyData.get(level);
        if (data.getDifficulty() != DifficultyLevel.CUSTOM) {
            ctx.getSource().sendFailure(Component.literal("当前世界不是自定义难度"));
            return 0;
        }

        data.setCustomSpawnRate(rate);
        ctx.getSource().sendSuccess(() -> Component.literal("已设置寄生体刷怪速率为: " + rate + " 倍"), true);
        return 1;
    }

    private static int setCustomReward(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
        ServerLevel level = ctx.getSource().getLevel();
        WorldDifficultyData data = WorldDifficultyData.get(level);
        if (data.getDifficulty() != DifficultyLevel.CUSTOM) {
            ctx.getSource().sendFailure(Component.literal("当前世界不是自定义难度"));
            return 0;
        }

        data.setCustomRewardEnabled(enabled);
        ctx.getSource().sendSuccess(() -> Component.literal("已" + (enabled ? "启用" : "禁用") + "奖励机制"), true);
        return 1;
    }

    private static int showHelp(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal("epca_custom 指令用法：\n" +
                "  /epca_custom difficulty <easy|normal|expert|master>   - 设置基础难度\n" +
                "  /epca_custom spawnrate <0~5>                         - 设置寄生体刷怪速率\n" +
                "  /epca_custom reward <true|false>                     - 开关奖励机制"), false);
        return 1;
    }
}