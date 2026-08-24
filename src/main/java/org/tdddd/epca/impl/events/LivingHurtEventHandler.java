package org.tdddd.epca.impl.events;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LivingHurtEventHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();

        
        MobEffectInstance effectInstance = entity.getEffect(ModEffects.VIRAL.get());
        if (effectInstance == null) return;

        int amplifier = effectInstance.getAmplifier();

        
        float damageMultiplier = 1.0f + (amplifier + 1) * 0.5f;

        
        float originalDamage = event.getAmount();
        float newDamage = originalDamage * damageMultiplier;
        event.setAmount(newDamage);
    }
}