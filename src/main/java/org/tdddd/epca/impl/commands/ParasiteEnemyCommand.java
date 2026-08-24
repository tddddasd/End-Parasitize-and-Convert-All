package org.tdddd.epca.impl.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.tdddd.epca.impl.ModConfig;

import java.util.ArrayList;
import java.util.List;

public class ParasiteEnemyCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("epca_parasite")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("enemy")
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", StringArgumentType.string())
                                        .executes(context -> addEnemy(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "player")
                                        ))
                                )
                        )
                        .then(Commands.literal("remove")
                                .then(Commands.argument("player", StringArgumentType.string())
                                        .executes(context -> removeEnemy(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "player")
                                        ))
                                )
                        )
                        .then(Commands.literal("list")
                                .executes(context -> listEnemies(context.getSource()))
                        )
                )
        );
    }

    private static int addEnemy(CommandSourceStack source, String playerName) {
        List<String> enemies = new ArrayList<>(ModConfig.PARASITE_ENEMY_PLAYERS.get());

        if (enemies.contains(playerName)) {
            source.sendFailure(Component.literal("玩家 " + playerName + " 已在列表中"));
            return 0;
        }

        enemies.add(playerName);
        ModConfig.PARASITE_ENEMY_PLAYERS.set(enemies);
        ModConfig.clearCache();

        source.sendSuccess(() -> Component.literal("已将玩家 " + playerName + " 添加到寄生体敌对列表"), true);
        return 1;
    }

    private static int removeEnemy(CommandSourceStack source, String playerName) {
        List<String> enemies = new ArrayList<>(ModConfig.PARASITE_ENEMY_PLAYERS.get());

        if (!enemies.contains(playerName)) {
            source.sendFailure(Component.literal("玩家 " + playerName + " 不在敌对列表中"));
            return 0;
        }

        enemies.remove(playerName);
        ModConfig.PARASITE_ENEMY_PLAYERS.set(enemies);
        ModConfig.clearCache();

        source.sendSuccess(() -> Component.literal("已将玩家 " + playerName + " 从寄生体敌对列表中移除"), true);
        return 1;
    }

    private static int listEnemies(CommandSourceStack source) {
        List<? extends String> enemies = ModConfig.PARASITE_ENEMY_PLAYERS.get();

        if (enemies.isEmpty()) {
            source.sendSuccess(() -> Component.literal("寄生体敌对列表为空"), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("寄生体敌对玩家列表:"), false);
        for (String player : enemies) {
            source.sendSuccess(() -> Component.literal(" - " + player), false);
        }

        return enemies.size();
    }
}