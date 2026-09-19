package org.tdddd.epca.impl.events;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.effects.debuff.ViralEffect;

@EventBusSubscriber(modid = epca.MODID)
public class ViralEffectEventHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();

        
        MobEffectInstance viralEffect = entity.getEffect(ModEffects.VIRAL);
        if (viralEffect == null) return;

        
        int amplifier = viralEffect.getAmplifier();
        
        float damageMultiplier = ViralEffect.getDamageMultiplier(amplifier);

        
        float originalDamage = event.getAmount();
        float finalDamage = originalDamage + damageMultiplier;
        event.setAmount(finalDamage);
    }
}
