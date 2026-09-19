package org.tdddd.epca.impl.overworld.registry.effects.buff;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class SoulProtectionEffect extends MobEffect {
    public SoulProtectionEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFD700);
    }

    // 26.1.2: isDurationEffectTick(duration, amplifier) -> shouldApplyEffectTickThisTick(tickCount, amplification).
    // 1.20.1 returned false, so applyEffectTick never ran; keep that exactly.
    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
        return false;
    }
}
