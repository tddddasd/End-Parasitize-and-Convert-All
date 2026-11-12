package org.tdddd.epca.impl.overworld.registry.effects.debuff;

import org.tdddd.epca.impl.events.ParasiteEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.tdddd.epca.impl.overworld.registry.effects.RemovableEffect;

public class SoulBreakingCurseEffect extends MobEffect implements RemovableEffect {
    public SoulBreakingCurseEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x9932CC);
    }

    private Holder<MobEffect> holder() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(this);
    }

    @Override
    public boolean applyEffectTick(ServerLevel serverLevel, LivingEntity entity, int amplifier) {
        super.applyEffectTick(serverLevel, entity, amplifier);

            if (entity.level().isClientSide()) return true;

            Level level = entity.level();
            double x = entity.getX();
            double y = entity.getY();
            double z = entity.getZ();

            level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F, (1.0F + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.2F) * 0.7F);

            if (level instanceof net.minecraft.server.level.ServerLevel) {
                ((net.minecraft.server.level.ServerLevel) level).sendParticles(
                        ParticleTypes.EXPLOSION,
                        x, y, z,
                        100,
                        4.0D, 4.0D, 4.0D,
                        0.5D
                );

                ((net.minecraft.server.level.ServerLevel) level).sendParticles(
                        ParticleTypes.EXPLOSION_EMITTER,
                        x, y, z,
                        20,
                        4.0D, 4.0D, 4.0D,
                        0.5D
                );
            }

            level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(4.0))
                    .forEach(target -> {

                        if (target instanceof Player || ParasiteEvents.isParasiteByTagOrInterface(entity)) {
                            return;
                        }

                        double distSqr = target.distanceToSqr(x, y, z);

                        if (distSqr <= 16.0D ) {
                            target.setHealth(target.getHealth() - target.getMaxHealth() * (amplifier + 1) * 0.01F);
                        }
                    });

            entity.removeEffect(holder());
            return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {

        return true;
    }

    @Override
    public boolean isRemovable() {
        return false;
    }
}
