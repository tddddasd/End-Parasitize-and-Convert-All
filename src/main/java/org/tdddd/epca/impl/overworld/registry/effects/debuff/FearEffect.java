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

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        int interval = 1 << amplifier; 
        return interval > 0 ? duration % interval == 0 : true;
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