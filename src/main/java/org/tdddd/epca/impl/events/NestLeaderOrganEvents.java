package org.tdddd.epca.impl.events;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.organ.NestLeaderOrganCarry;

/**
 *  GUI STAGE 2 + STAGE 3 /
 *
 * <p>STAGE 2  /
 * {@link NestLeaderOrganCarry}
 *  {@code ServerPlayer} </p>
 *
 * <p>STAGE 3  / <b></b>STAGE A  7  9
 *  {@code entity_gravity}
 * {@link NestLeaderOrganDamageHandler#clear}
 * {@code NestLeaderOrganEffects#clear}
 *
 * {@code epca#onPlayerTick}
 *  {@code addTransientModifier}  transient
 *  id </p>
 *
 * <p>{@code @EventBusSubscriber(modid = epca.MODID)}
 * 1.20.1  {@code bus = Mod.EventBusSubscriber.Bus.FORGE}26.1.2
 * {@code @EventBusSubscriber}  game bus Forge bus
 *  {@code value = Dist.CLIENT}
 * <b></b></p>
 */
@EventBusSubscriber(modid = epca.MODID)
public final class NestLeaderOrganEvents {

    private NestLeaderOrganEvents() {
    }

    /** +  STAGE 3  */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        NestLeaderOrganCarry.returnToPlayer(player);
        NestLeaderOrganDamageHandler.clear(player);
    }

    /**  tick  */
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        NestLeaderOrganCarry.onDimensionChanged(player);
        NestLeaderOrganDamageHandler.clear(player);
    }

    /**
     *
     *
     * <p> {@code epca#onPlayerTick}
     * {@code NestLeaderOrganEffects#notLeader}
     *
     *
     *  tick  {@code notLeader} </p>
     *
     * <p> tick  {@code NestLeaderOrganEffects#tick}
     * </p>
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        NestLeaderOrganDamageHandler.clear(player);
    }
}

