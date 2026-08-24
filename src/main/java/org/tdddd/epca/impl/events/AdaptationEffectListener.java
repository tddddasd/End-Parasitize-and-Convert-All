package org.tdddd.epca.impl.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.ColorEffectPacket;
import org.tdddd.epca.impl.overworld.registry.ModParticles;
import org.tdddd.epca.impl.overworld.registry.ModSoundEvents;
import org.tdddd.epca.impl.utils.ParticleHelper;
import org.tdddd.yawning_neko_api.events.AdaptationEffectEvent;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AdaptationEffectListener {

    @SubscribeEvent
    public static void onAdaptationEffect(AdaptationEffectEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return; 

        
        entity.hurtTime = 0;
        entity.hurtDuration = 0;

        
        int packetType = event.getEffectType() == AdaptationEffectEvent.Type.PARTIAL_ADAPTATION ? 0 : 1;
        ModNetwork.sendToAllTracking(new ColorEffectPacket(entity, packetType), entity);

        ServerLevel serverLevel = (ServerLevel) entity.level();

        switch (event.getEffectType()) {
            case PARTIAL_ADAPTATION:
                
                serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        ModSoundEvents.PARCIAL_ADAPTATION.get(), SoundSource.NEUTRAL,
                        1.0F, 0.5F + entity.getRandom().nextFloat() * 0.5F);
                
                sendPAdaptationParticles(serverLevel, entity);
                break;
            case FULL_ADAPTATION:
                serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        ModSoundEvents.FULL_ADAPTATION.get(), SoundSource.NEUTRAL,
                        1.0F, 0.5F + entity.getRandom().nextFloat() * 0.5F);
                sendFAdaptationParticles(serverLevel, entity);
                break;
        }
    }

    private static void sendPAdaptationParticles(ServerLevel level, LivingEntity entity) {
        
        AABB bb = entity.getBoundingBox();
        var random = entity.getRandom();
        int count = 3 + random.nextInt(3);
        for (int i = 0; i < count; i++) {
            double x = bb.minX + random.nextDouble() * (bb.maxX - bb.minX);
            double y = bb.minY + random.nextDouble() * (bb.maxY - bb.minY);
            double z = bb.minZ + random.nextDouble() * (bb.maxZ - bb.minZ);
            ParticleHelper.sendParticleToAllPlayers(level, ModParticles.P_ADAPTATION.get(), x, y, z, 1, 0, 0, 0, 0.0);
        }
    }

    private static void sendFAdaptationParticles(ServerLevel level, LivingEntity entity) {
        
        AABB bb = entity.getBoundingBox();
        var random = entity.getRandom();
        int count = 3 + random.nextInt(3);
        for (int i = 0; i < count; i++) {
            double x = bb.minX + random.nextDouble() * (bb.maxX - bb.minX);
            double y = bb.minY + random.nextDouble() * (bb.maxY - bb.minY);
            double z = bb.minZ + random.nextDouble() * (bb.maxZ - bb.minZ);
            ParticleHelper.sendParticleToAllPlayers(level, ModParticles.F_ADAPTATION.get(), x, y, z, 1, 0, 0, 0, 0.0);
        }
    }
}