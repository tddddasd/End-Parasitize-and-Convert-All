package org.tdddd.epca.impl.utils;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class EntityConversionUtil {
    public static void convertTo(LivingEntity entity, EntityType<? extends LivingEntity> targetType) {
        
        if (entity.level().isClientSide) return;

        
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.ZOMBIE_INFECT, SoundSource.HOSTILE, 1.0F, 1.0F);

        
        for (int i = 0; i < 10; i++) {
            double offsetX = (entity.getRandom().nextDouble() - 0.5) * 1.5;
            double offsetY = (entity.getRandom().nextDouble() - 0.5) * 1.5;
            double offsetZ = (entity.getRandom().nextDouble() - 0.5) * 1.5;

            entity.level().addParticle(ParticleTypes.EXPLOSION,
                    entity.getX() + offsetX,
                    entity.getY() + 0.5 + offsetY,
                    entity.getZ() + offsetZ,
                    0, 0, 0);
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
        }

        
        if (!(entity instanceof Player)) {
            entity.remove(Entity.RemovalReason.UNLOADED_WITH_PLAYER);
            entity.teleportTo(1000000, -4000, 1000000);
        }
    }
}