package org.tdddd.epca.impl.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.tdddd.epca.impl.overworld.registry.entities.entity.special.illusion.IllusoryButterflyEntity;

/**
 * {@code /epca_butterfly_info}: prints the rewind diagnostics of the illusory butterfly nearest to the
 * command source (timeline tick, phase, block change buckets, recorded spawns / removals).
 *
 * <p>Read only, useful to tell "nothing was recorded" apart from "the replay never reached the bucket".</p>
 */
public final class ButterflyInfoCommand {

    private ButterflyInfoCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("epca_butterfly_info")
                .requires(source -> source.hasPermission(2))
                .executes(context -> info(context.getSource())));
    }

    private static int info(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        Vec3 origin = source.getPosition();

        // every loaded entity of the level, so the command also works when the player is not in the arena
        IllusoryButterflyEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof IllusoryButterflyEntity butterfly)) {
                continue;
            }
            double distance = butterfly.distanceToSqr(origin);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = butterfly;
            }
        }
        if (nearest == null) {
            source.sendFailure(Component.literal("[butterfly] no illusory butterfly loaded in " + level.dimension().location()));
            return 0;
        }
        IllusoryButterflyEntity found = nearest;
        double distance = Math.sqrt(nearestDistance);
        source.sendSuccess(() -> Component.literal("[butterfly] " + found.debugInfo()
                + " distance=" + String.format("%.1f", distance)), false);
        return 1;
    }
}
