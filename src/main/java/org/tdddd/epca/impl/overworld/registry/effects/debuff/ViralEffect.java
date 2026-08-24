package org.tdddd.epca.impl.overworld.registry.effects.debuff;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import org.tdddd.epca.impl.overworld.registry.effects.RemovableEffect;

public class ViralEffect extends MobEffect implements RemovableEffect {

    public ViralEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x00FF00);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        int interval = 1 << amplifier; 
        return interval > 0 ? duration % interval == 0 : true;
    }

    @Override
    public boolean isRemovable() {
        return false; 
    }

    
    public static float getDamageMultiplier(int amplifier) {
        return (amplifier + 1) * 0.1f;
    }
}