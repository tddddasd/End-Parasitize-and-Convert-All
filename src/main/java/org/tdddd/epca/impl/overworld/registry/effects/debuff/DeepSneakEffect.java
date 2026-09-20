package org.tdddd.epca.impl.overworld.registry.effects.debuff;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.effects.RemovableEffect;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DeepSneakEffect extends MobEffect implements RemovableEffect{
    private static final String SPEED_MODIFIER_UUID = "1a2b3c4d-5e6f-7a8b-9a0b-c1d2e3a4b5c6";
    private static final String DAMAGE_MODIFIER_UUID = "1a2b3c4d-5e6f-7a8b-9a0b-c1d2e3a4b5c7";

    public DeepSneakEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x0000AA);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        super.applyEffectTick(entity, amplifier);

        
        boolean isParasite = IParasite.isParasiteByTagOrInterface(entity);

        if (isParasite) {
            if (entity.isOnFire() && !entity.isInLava()) {
                entity.clearFire();
            }
            removeSpeedModifier(entity);
            return;
        }

        if (!hasLeatherArmor(entity)) {
            float damage = 0.025f * (amplifier + 1);
            if (entity.isAlive() && !entity.isInvulnerable()) {
                entity.hurt(entity.damageSources().freeze(), damage);
            }
        }

        if (!hasTurtleHelmet(entity)) {
            applySpeedModifier(entity, amplifier);
        } else {
            removeSpeedModifier(entity);
        }
    }

    private static boolean hasLeatherArmor(LivingEntity entity) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof ArmorItem armorItem) {
                if (armorItem.getMaterial() == ArmorMaterials.LEATHER) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasTurtleHelmet(LivingEntity entity) {
        ItemStack helmet = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (!helmet.isEmpty() && helmet.getItem() instanceof ArmorItem armorItem) {
            return armorItem.getMaterial() == ArmorMaterials.TURTLE;
        }
        return false;
    }

    private static void applySpeedModifier(LivingEntity entity, int amplifier) {
        AttributeInstance instance = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (instance == null) return;

        
        instance.removeModifier(UUID.fromString(SPEED_MODIFIER_UUID));

        double amount = -0.0125 * (amplifier + 1);
        AttributeModifier modifier = new AttributeModifier(
                UUID.fromString(SPEED_MODIFIER_UUID),
                "DeepSneak speed reduction",
                amount,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
        instance.addPermanentModifier(modifier);
    }

    private static void removeSpeedModifier(LivingEntity entity) {
        AttributeInstance instance = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (instance != null) {
            instance.removeModifier(UUID.fromString(SPEED_MODIFIER_UUID));
        }
    }

    @Override
    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        if (modifier.getId().toString().equals(SPEED_MODIFIER_UUID)) {
            
            return -0.0125 * (amplifier + 1);
        } else if (modifier.getId().toString().equals(DAMAGE_MODIFIER_UUID)) {
            
            return -0.0125 * (amplifier + 1);
        }
        return 0;
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();

        if (source.getEntity() instanceof LivingEntity attacker) {
            if (attacker.hasEffect(ModEffects.DEEP_SNEAK.get()) && !IParasite.isParasiteByTagOrInterface(attacker)) {
                if (!hasTurtleHelmet(attacker)) {
                    int amplifier = attacker.getEffect(ModEffects.DEEP_SNEAK.get()).getAmplifier();
                    float damageReduction = 0.0125f * (amplifier + 1);
                    float newDamage = event.getAmount() * (1 - damageReduction);
                    event.setAmount(newDamage);
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

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (event.getEffect() == ModEffects.DEEP_SNEAK.get()) {
            LivingEntity entity = event.getEntity();
            if (entity != null) {
                removeSpeedModifier(entity);
            }
        }
    }
}