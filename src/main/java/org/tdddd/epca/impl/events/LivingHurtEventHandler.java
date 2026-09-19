package org.tdddd.epca.impl.events;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

@EventBusSubscriber(modid = epca.MODID)
public class LivingHurtEventHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();

        
        MobEffectInstance effectInstance = entity.getEffect(ModEffects.VIRAL);
        if (effectInstance == null) return;

        int amplifier = effectInstance.getAmplifier();

        
        float damageMultiplier = 1.0f + (amplifier + 1) * 0.5f;

        
        float originalDamage = event.getAmount();
        float newDamage = originalDamage * damageMultiplier;
        event.setAmount(newDamage);
    }
}