package org.tdddd.epca.impl.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.tdddd.epca.impl.events.BiomassEventHandler;
import org.tdddd.epca.impl.overworld.data.NestLeaderManager;

public class NestLeaderCommand {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("epca_nestleader")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("add")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    boolean success = NestLeaderManager.addNestLeader(target.getUUID());
                                    if (success) {
                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal(target.getName().getString() + " 已成为领巢者"), true);
                                        BiomassEventHandler.syncBiomass(target);
                                    } else {
                                        ctx.getSource().sendFailure(Component.literal("添加失败"));
                                    }
                                    return success ? 1 : 0;
                                })))
                .then(Commands.literal("remove")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    boolean success = NestLeaderManager.removeNestLeader(target.getUUID());
                                    if (success) {
                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal(target.getName().getString() + " 已取消领巢者"), true);
                                        BiomassEventHandler.syncBiomass(target);
                                    } else {
                                        ctx.getSource().sendFailure(Component.literal("目标不为领巢者"));
                                    }
                                    return success ? 1 : 0;
                                })))
        );
    }
}