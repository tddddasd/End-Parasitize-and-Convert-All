package org.tdddd.epca.impl.overworld.registry.effects.buff;

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
    public void applyEffectTick(LivingEntity livingEntity, int amplifier) {
        
        if (!livingEntity.level().isClientSide && livingEntity.tickCount % 20 == 0) {
            
            livingEntity.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                    livingEntity.getBoundingBox().inflate(30),
                    mob -> mob.getTarget() == livingEntity
            ).forEach(mob -> mob.setTarget(null));
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        
        return duration % 20 == 0;
    }

    
    public static boolean hasSpiritEffect(@Nullable LivingEntity entity) {
        if (entity == null) return false;
        return entity.hasEffect(ModEffects.SPIRIT.get());
    }

    @Override
    public boolean isRemovable() {
        return false; 
    }
}