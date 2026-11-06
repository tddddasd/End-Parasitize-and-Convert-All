package org.tdddd.epca.impl.overworld.registry.effects.buff;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.effects.RemovableEffect;

public class SpiritEffect extends MobEffect implements RemovableEffect {

    public SpiritEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x000000);
    }

    @Override
    public boolean applyEffectTick(ServerLevel serverLevel, LivingEntity livingEntity, int amplifier) {

        if (livingEntity.tickCount % 20 == 0) {

            livingEntity.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                    livingEntity.getBoundingBox().inflate(30),
                    mob -> mob.getTarget() == livingEntity
            ).forEach(mob -> mob.setTarget(null));
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {

        return tickCount % 20 == 0;
    }

    public static boolean hasSpiritEffect(@Nullable LivingEntity entity) {
        if (entity == null) return false;
        return entity.hasEffect(ModEffects.SPIRIT);
    }

    @Override
    public boolean isRemovable() {
        return false;
    }
}
