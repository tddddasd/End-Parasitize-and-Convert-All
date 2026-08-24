package org.tdddd.epca.impl.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.ModConfig;
import org.tdddd.epca.impl.overworld.data.SafetyDaySavedData;
import org.tdddd.epca.impl.epca;
import net.minecraft.sounds.SoundSource;

@Mod.EventBusSubscriber(modid = epca.MODID)
public class SafetyDayHandler {

    
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
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
        if (event.getEntity().level().isClientSide) return;
        ServerLevel level = (ServerLevel) event.getEntity().level();
        tryStartSafetyDay(level);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide) return;
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