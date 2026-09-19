package org.tdddd.epca.impl.overworld.registry.effects.debuff;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import org.tdddd.epca.impl.overworld.registry.effects.RemovableEffect;

public class FearEffect extends MobEffect implements RemovableEffect {
    public static final MutableComponent FEAR_MESSAGE = Component.translatable("effect.epca.fear.message")
            .withStyle(ChatFormatting.RED);

    public FearEffect() {
        super(MobEffectCategory.BENEFICIAL,  0x333333);
    }

    // 26.1.2: isDurationEffectTick(duration, amplifier) -> shouldApplyEffectTickThisTick(tickCount, amplification).
    // tickCount is the effect duration for timed effects (MobEffectInstance#tickServer), so the 1.20.1
    // "duration % (1 << amplifier) == 0" condition is reproduced verbatim, negative-shift guard included.
    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
        int interval = 1 << amplification; 
        return interval > 0 ? tickCount % interval == 0 : true;
    }

    @Override
    public boolean isRemovable() {
        return false; 
    }


    
    public static boolean shouldPreventBlockPlacement(int amplifier) {
        return amplifier + 1 >= 2 && Math.random() < 0.2;
    }

    
    public static boolean shouldPreventItemUse(int amplifier) {
        return amplifier + 1 >= 3 && Math.random() < 0.2;
    }
}
