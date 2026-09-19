package org.tdddd.epca.impl.overworld.registry.effects.debuff;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.effects.RemovableEffect;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

import java.util.UUID;

@EventBusSubscriber(modid = epca.MODID)
public class DeepSneakEffect extends MobEffect implements RemovableEffect{
    private static final Identifier SPEED_MODIFIER_ID = Identifier.fromNamespaceAndPath(epca.MODID, "deep_sneak_speed");

    public DeepSneakEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x0000AA);
    }

    @Override
    public boolean applyEffectTick(ServerLevel serverLevel, LivingEntity entity, int amplifier) {
        super.applyEffectTick(serverLevel, entity, amplifier);

        
        boolean isParasite = IParasite.isParasiteByTagOrInterface(entity);

        if (isParasite) {
            if (entity.isOnFire() && !entity.isInLava()) {
                entity.clearFire();
            }
            removeSpeedModifier(entity);
            return true;
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
        return true;
    }

    // 26.1.2: armor is data driven - the material is identified by the item's
    // equipment asset (Equippable#assetId) instead of an ArmorItem#getMaterial().
    private static boolean hasLeatherArmor(LivingEntity entity) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (hasEquipmentAsset(entity.getItemBySlot(slot), EquipmentAssets.LEATHER)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasTurtleHelmet(LivingEntity entity) {
        return hasEquipmentAsset(entity.getItemBySlot(EquipmentSlot.HEAD), EquipmentAssets.TURTLE_SCUTE);
    }

    private static boolean hasEquipmentAsset(ItemStack stack, ResourceKey<EquipmentAsset> asset) {
        if (stack.isEmpty()) return false;
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.assetId().filter(asset::equals).isPresent();
    }

    private static void applySpeedModifier(LivingEntity entity, int amplifier) {
        AttributeInstance instance = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (instance == null) return;

        // 移除旧修饰符，防止重复
        instance.removeModifier(SPEED_MODIFIER_ID);

        double amount = -0.0125 * (amplifier + 1);
        AttributeModifier modifier = new AttributeModifier(
                SPEED_MODIFIER_ID,
                amount,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
        instance.addPermanentModifier(modifier);
    }

    private static void removeSpeedModifier(LivingEntity entity) {
        AttributeInstance instance = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (instance != null) {
            instance.removeModifier(SPEED_MODIFIER_ID);
        }
    }


    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();

        if (source.getEntity() instanceof LivingEntity attacker) {
            if (attacker.hasEffect(ModEffects.DEEP_SNEAK) && !IParasite.isParasiteByTagOrInterface(attacker)) {
                if (!hasTurtleHelmet(attacker)) {
                    int amplifier = attacker.getEffect(ModEffects.DEEP_SNEAK).getAmplifier();
                    float damageReduction = 0.0125f * (amplifier + 1);
                    float newDamage = event.getAmount() * (1 - damageReduction);
                    event.setAmount(newDamage);
                }
            }
        }
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
        return true;
    }

    @Override
    public boolean isRemovable() {
        return false; 
    }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (event.getEffect().value() == ModEffects.DEEP_SNEAK.get()) {
            LivingEntity entity = event.getEntity();
            if (entity != null) {
                removeSpeedModifier(entity);
            }
        }
    }
}