package org.tdddd.epca.impl.events;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.data.organ.HiveLeaderOrganCarry;
import org.tdddd.epca.impl.overworld.data.organ.OrganModelDisplay;

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
        OrganModelDisplay.onPlayerJoin(player);
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer viewer)) {
            return;
        }
        if (!(event.getTarget() instanceof ServerPlayer target)) {
            return;
        }
        OrganModelDisplay.sendTo(viewer, target);
    }
}
