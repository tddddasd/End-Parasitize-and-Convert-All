package org.tdddd.epca.impl.utils;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.tdddd.epca.impl.overworld.registry.ModParticles;
import org.tdddd.epca.impl.overworld.registry.effects.debuff.CothEffect;

import java.util.Random;

public class EntityConversionUtil {

    /** Number of meat particles spawned on a conversion (3~5). */
    private static final int MIN_MEAT_PARTICLES = 3;
    private static final int MEAT_PARTICLE_VARIANCE = 3;

    public static void convertTo(LivingEntity entity, EntityType<? extends LivingEntity> targetType) {

        if (entity.level().isClientSide()) return;

        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.ZOMBIE_INFECT, SoundSource.HOSTILE, 1.0F, 1.0F);

        // 26.1.2: Level#addParticle is client-only, so the equivalent EPCA meat particles are
        // emitted from the server through ServerLevel#sendParticles. The former vanilla
        // ParticleTypes.EXPLOSION burst is replaced by 3~5 living-flesh particles.
        spawnMeatParticles(entity);

        LivingEntity newEntity = targetType.create(entity.level(), EntitySpawnReason.CONVERSION);
        if (newEntity != null) {

            newEntity.copyPosition(entity);
            newEntity.setYHeadRot(entity.getYHeadRot());
            newEntity.setYBodyRot(entity.yBodyRot);

            if (entity.hasCustomName()) {
                newEntity.setCustomName(entity.getCustomName());
                newEntity.setCustomNameVisible(entity.isCustomNameVisible());
            }

            newEntity.snapTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());

            entity.level().addFreshEntity(newEntity);

            CothEffect.notifyConvertedEntity(newEntity);
        }

        if (!(entity instanceof Player)) {
            entity.remove(Entity.RemovalReason.UNLOADED_WITH_PLAYER);
            entity.teleportTo(1000000, -4000, 1000000);
        }
    }

    private static void spawnMeatParticles(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        net.minecraft.util.RandomSource random = entity.getRandom();
        int count = MIN_MEAT_PARTICLES + random.nextInt(MEAT_PARTICLE_VARIANCE);
        serverLevel.sendParticles(ModParticles.LIVING_FLESH.get(),
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                count, 0.5, 0.5, 0.5, 0.05);
    }
}
