package org.tdddd.epca.impl.overworld.registry.effects.debuff;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class InfestedOmenEffect extends MobEffect {

    public static final int COLOR = 0x8B1E4B;

    public InfestedOmenEffect() {
        super(MobEffectCategory.HARMFUL, COLOR);
    }
}
