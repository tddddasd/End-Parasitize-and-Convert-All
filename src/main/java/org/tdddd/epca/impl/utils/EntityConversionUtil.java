package org.tdddd.epca.impl.utils;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.tdddd.epca.impl.network.ModNetwork;
import org.tdddd.epca.impl.network.packet.s2c.ColorEffectPacket;
import org.tdddd.epca.impl.overworld.registry.ModParticles;
import org.tdddd.epca.impl.overworld.registry.effects.debuff.CothEffect;

public class EntityConversionUtil {
    public static void convertTo(LivingEntity entity, EntityType<? extends LivingEntity> targetType) {
        
        if (entity.level().isClientSide) return;

        
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.ZOMBIE_INFECT, SoundSource.HOSTILE, 1.0F, 1.0F);

        
        if (entity.level() instanceof ServerLevel serverLevel) {
            int count = 3 + serverLevel.getRandom().nextInt(3);
            serverLevel.sendParticles(ModParticles.LIVING_FLESH.get(),
                    entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                    count, 0.5, 0.5, 0.5, 0.05);
        }

        
        LivingEntity newEntity = targetType.create(entity.level());
        if (newEntity != null) {
            
            newEntity.copyPosition(entity);
            newEntity.setYHeadRot(entity.getYHeadRot());
            newEntity.setYBodyRot(entity.yBodyRot);

            
            if (entity.hasCustomName()) {
                newEntity.setCustomName(entity.getCustomName());
                newEntity.setCustomNameVisible(entity.isCustomNameVisible());
            }

            
            newEntity.moveTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());

            
            entity.level().addFreshEntity(newEntity);

            
            ModNetwork.sendToAllTracking(
                    new ColorEffectPacket(newEntity, CothEffect.TYPE_CONVERSION_FADE, 6), newEntity);
        }

        
        if (!(entity instanceof Player)) {
            entity.remove(Entity.RemovalReason.UNLOADED_WITH_PLAYER);
            entity.teleportTo(1000000, -4000, 1000000);
        }
    }
}