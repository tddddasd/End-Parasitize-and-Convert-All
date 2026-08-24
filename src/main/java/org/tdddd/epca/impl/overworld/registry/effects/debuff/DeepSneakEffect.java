package org.tdddd.epca.impl.overworld.registry.effects.debuff;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.effects.RemovableEffect;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DeepSneakEffect extends MobEffect implements RemovableEffect{
    private static final String SPEED_MODIFIER_UUID = "1a2b3c4d-5e6f-7a8b-9a0b-c1d2e3a4b5c6";
    private static final String DAMAGE_MODIFIER_UUID = "1a2b3c4d-5e6f-7a8b-9a0b-c1d2e3a4b5c7";

    public DeepSneakEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x0000AA);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, SPEED_MODIFIER_UUID, -0.0125, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        super.applyEffectTick(entity, amplifier);

        
        boolean isParasite = IParasite.isParasiteByTagOrInterface(entity);

        
        if (isParasite && entity.isOnFire() && !entity.isInLava()) {
            entity.clearFire();
        }

        
        if (!isParasite) {
            float damage = 0.025f * (amplifier + 1); 
            
            if (entity.isAlive() && !entity.isInvulnerable()) {
                entity.hurt(entity.damageSources().freeze(), damage);
            }
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
        LivingEntity target = event.getEntity();

        
        if (source.getEntity() instanceof LivingEntity attacker) {
            
            if (attacker.hasEffect(ModEffects.DEEP_SNEAK.get()) && !IParasite.isParasiteByTagOrInterface(attacker)) {
                int amplifier = attacker.getEffect(ModEffects.DEEP_SNEAK.get()).getAmplifier();

                
                if (!IParasite.isParasiteByTagOrInterface(attacker)) {
                    
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
}