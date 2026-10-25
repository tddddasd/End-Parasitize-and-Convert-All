package org.tdddd.epca.impl.events;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganCarry;

/**
 *  GUI STAGE 2 + STAGE 3 /
 *
 * <p>STAGE 2  /
 * {@link HiveLeaderOrganCarry}
 *  {@code ServerPlayer} </p>
 *
 * <p>STAGE 3  / <b></b>STAGE A  7  9
 *  {@code entity_gravity}
 * {@link HiveLeaderOrganDamageHandler#clear}
 * {@code HiveLeaderOrganEffects#clear}
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
public final class HiveLeaderOrganEvents {

    private HiveLeaderOrganEvents() {
    }

    /** +  STAGE 3  */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        HiveLeaderOrganCarry.returnToPlayer(player);
        HiveLeaderOrganDamageHandler.clear(player);
    }

    /**  tick  */
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        HiveLeaderOrganCarry.onDimensionChanged(player);
        HiveLeaderOrganDamageHandler.clear(player);
    }

    /**
     *
     *
     * <p> {@code epca#onPlayerTick}
     * {@code HiveLeaderOrganEffects#notLeader}
     *
     *
     *  tick  {@code notLeader} </p>
     *
     * <p> tick  {@code HiveLeaderOrganEffects#tick}
     * </p>
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        HiveLeaderOrganDamageHandler.clear(player);
    }
}

