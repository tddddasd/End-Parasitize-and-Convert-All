package org.tdddd.epca.impl.overworld.registry.effects.debuff;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.tdddd.epca.impl.overworld.registry.effects.RemovableEffect;

public class CorrosiveEffect extends MobEffect implements RemovableEffect {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public CorrosiveEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x0A5F20);
    }

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

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {

        return true;
    }

    @Override
    public boolean isRemovable() {
        return false;
    }
}
