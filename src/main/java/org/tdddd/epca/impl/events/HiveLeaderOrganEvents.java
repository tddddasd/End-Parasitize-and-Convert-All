package org.tdddd.epca.impl.events;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganCarry;

@EventBusSubscriber(modid = epca.MODID)
public final class HiveLeaderOrganEvents {

    private HiveLeaderOrganEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        HiveLeaderOrganCarry.returnToPlayer(player);
        HiveLeaderOrganDamageHandler.clear(player);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        HiveLeaderOrganCarry.onDimensionChanged(player);
        HiveLeaderOrganDamageHandler.clear(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        HiveLeaderOrganDamageHandler.clear(player);
    }
}
