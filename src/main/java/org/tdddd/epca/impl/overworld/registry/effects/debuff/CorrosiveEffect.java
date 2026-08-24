package org.tdddd.epca.impl.overworld.registry.effects.debuff;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.tdddd.epca.impl.overworld.registry.effects.RemovableEffect;

public class CorrosiveEffect extends MobEffect implements RemovableEffect {
    public CorrosiveEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x0A5F20);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        
        int effectLevel = amplifier + 1;
        int damagePerSecond = effectLevel * 3; 

        
        if (entity.tickCount % 20 == 0) {
            
            for (ItemStack armor : entity.getArmorSlots()) {
                
                if (armor.isEmpty() || !armor.isDamageableItem()) {
                    continue;
                }

                
                if (entity.getRandom().nextFloat() < 1.0f) {
                    
                    armor.hurtAndBreak(
                            damagePerSecond,
                            entity,
                            e -> {} 
                    );
                }
            }
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        
        return true;
    }

    @Override
    public boolean isRemovable() {
        return false; 
    }
}