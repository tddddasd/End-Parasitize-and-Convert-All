package org.tdddd.epca.impl.overworld.registry.effects.debuff;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.tdddd.epca.impl.overworld.registry.effects.RemovableEffect;

public class CorrosiveEffect extends MobEffect implements RemovableEffect {

    // 26.1.2: LivingEntity#getArmorSlots() is gone; the same four slots are iterated explicitly, in the
    // same order (HEAD, CHEST, LEGS, FEET) as the removed helper used.
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public CorrosiveEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x0A5F20);
    }

    // 26.1.2: applyEffectTick(ServerLevel, LivingEntity, int) returns boolean and only runs server side
    // (MobEffectInstance#tickServer).
    @Override
    public boolean applyEffectTick(ServerLevel serverLevel, LivingEntity entity, int amplifier) {
        
        int effectLevel = amplifier + 1;
        int damagePerSecond = effectLevel * 3; 

        
        if (entity.tickCount % 20 == 0) {
            
            for (EquipmentSlot slot : ARMOR_SLOTS) {
                ItemStack armor = entity.getItemBySlot(slot);
                
                if (armor.isEmpty() || !armor.isDamageableItem()) {
                    continue;
                }

                
                if (entity.getRandom().nextFloat() < 1.0f) {
                    // 26.1.2: ItemStack#hurtAndBreak(int, LivingEntity, Consumer) -> (int, ServerLevel,
                    // LivingEntity, Consumer). The empty on-break callback is preserved.
                    armor.hurtAndBreak(
                            damagePerSecond,
                            serverLevel,
                            entity,
                            e -> {} 
                    );
                }
            }
        }
        return true;
    }

    // 26.1.2: isDurationEffectTick(duration, amplifier) -> shouldApplyEffectTickThisTick(tickCount, amplification).
    // 1.20.1 returned true unconditionally (the 20-tick cadence lives in the body), so keep ticking every tick.
    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
        
        return true;
    }

    @Override
    public boolean isRemovable() {
        return false; 
    }
}
