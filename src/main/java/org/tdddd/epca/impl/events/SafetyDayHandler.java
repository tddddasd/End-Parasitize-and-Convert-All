package org.tdddd.epca.impl.events;

import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.ModConfig;
import org.tdddd.epca.impl.overworld.data.SafetyDaySavedData;
import org.tdddd.epca.impl.epca;
import net.minecraft.sounds.SoundSource;

@EventBusSubscriber(modid = epca.MODID)
public class SafetyDayHandler {

    
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (!ModConfig.isSafetyDayEnabled()) continue;
            SafetyDaySavedData data = SafetyDaySavedData.get(level);
            long currentTick = level.getGameTime();
            
            if (data.isSafetyDayFinished(currentTick) && !data.isEffectTriggered()) {
                data.markEffectTriggered();  
                playEndSound(level);
            }
        }
    }

    private static void playEndSound(ServerLevel level) {
        for (net.minecraft.server.level.ServerPlayer player : level.players()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BELL_RESONATE, SoundSource.AMBIENT, 1.0F, 1.0F);
        }
    }

    
    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        ServerLevel level = (ServerLevel) event.getEntity().level();
        tryStartSafetyDay(level);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        ServerLevel level = (ServerLevel) event.getEntity().level();
        tryStartSafetyDay(level);
    }

    private static void tryStartSafetyDay(ServerLevel level) {
        if (!ModConfig.isSafetyDayEnabled()) return;
        SafetyDaySavedData data = SafetyDaySavedData.get(level);
        if (data.safetyDayEndTick == -1) {
            long endTick = level.getGameTime() + ModConfig.getSafetyDayDurationTicks();
            data.startSafetyDay(endTick);
        }
    }
}