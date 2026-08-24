package org.tdddd.epca.impl.utils;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.effects.debuff.CothEffect;

public class EffectApplicationInterceptor {

    
    public static boolean canApplyEffect(LivingEntity target, MobEffectInstance effect) {
        
        if (effect.getEffect() != ModEffects.COTH.get()) {
            return true;
        }

        int newAmplifier = effect.getAmplifier();
        return CothEffect.canApplyEffect(target, newAmplifier);
    }

    
    public static boolean applyEffectSafely(LivingEntity target, MobEffectInstance effect) {
        if (canApplyEffect(target, effect)) {
            target.addEffect(effect);
            return true;
        }
        return false;
    }
}