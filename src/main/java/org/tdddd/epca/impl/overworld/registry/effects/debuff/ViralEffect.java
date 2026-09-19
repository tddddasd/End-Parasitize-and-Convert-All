package org.tdddd.epca.impl.overworld.registry.effects.debuff;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import org.tdddd.epca.impl.overworld.registry.effects.RemovableEffect;

public class ViralEffect extends MobEffect implements RemovableEffect {

    public ViralEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x00FF00);
    }

    // 26.1.2: isDurationEffectTick(duration, amplifier) -> shouldApplyEffectTickThisTick(tickCount, amplification).
    // tickCount is MobEffectInstance's duration for timed effects (MobEffectInstance#tickServer), so the
    // 1.20.1 "duration % (1 << amplifier) == 0" condition is reproduced verbatim. The old
    // "interval > 0 ? ... : true" guard also absorbed a negative/overflowing shift (1 << 31 == Integer.MIN_VALUE);
    // the same guard is kept so that edge case behaves identically.
    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
        int interval = 1 << amplification;
        return interval > 0 ? tickCount % interval == 0 : true;
    }

    @Override
    public boolean isRemovable() {
        return false; 
    }

    
    public static float getDamageMultiplier(int amplifier) {
        return (amplifier + 1) * 0.1f;
    }
}
