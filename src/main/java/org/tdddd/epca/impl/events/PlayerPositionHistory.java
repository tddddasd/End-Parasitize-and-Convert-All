package org.tdddd.epca.impl.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.entities.entity.special.BioTortSkillConstants;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 *
 *
 * <h2></h2>
 * <p> 2 " 3  5 "
 * {@code BioTortIncarnation} ""
 *  tick //
 * {@link BioTortSkillConstants#POSITION_HISTORY_TICKS} tick8  5
 * " tick "</p>
 *
 * <h2></h2>
 * <ul>
 *   <li> 5
 *       {@link ServerLevel}{@code ServerPlayer#teleportTo(ServerLevel, x, y, z, yRot, xRot)}
 *       ""</li>
 *   <li>" tick " 5
 *        {@code null}</li>
 *   <li> {@link ServerLevel}
 *       </li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PlayerPositionHistory {

    /**  */
    public static final class Sample {
        public final ServerLevel level;
        public final double x;
        public final double y;
        public final double z;
        public final float yRot;
        public final float xRot;
        /**  {@code level.getGameTime()} */
        public final long tick;

        Sample(ServerLevel level, double x, double y, double z, float yRot, float xRot, long tick) {
            this.level = level;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yRot = yRot;
            this.xRot = xRot;
            this.tick = tick;
        }
    }

    private static final Map<UUID, Deque<Sample>> HISTORY = new HashMap<>();

    private PlayerPositionHistory() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                Deque<Sample> samples = HISTORY.computeIfAbsent(player.getUUID(), key -> new ArrayDeque<>());
                samples.addLast(new Sample(level, player.getX(), player.getY(), player.getZ(),
                        player.getYRot(), player.getXRot(), level.getGameTime()));
                while (samples.size() > BioTortSkillConstants.POSITION_HISTORY_TICKS) {
                    samples.pollFirst();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        HISTORY.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        HISTORY.clear();
    }

    /**
     *  {@code targetTick}
     *
     * @return  {@code null}
     */
    public static Sample sampleAt(UUID playerId, long targetTick) {
        Deque<Sample> samples = HISTORY.get(playerId);
        if (samples == null || samples.isEmpty()) {
            return null;
        }
        Sample best = null;
        long bestDistance = Long.MAX_VALUE;
        for (Sample sample : samples) {
            long distance = Math.abs(sample.tick - targetTick);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = sample;
            }
        }
        return best;
    }
}

