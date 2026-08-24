package org.tdddd.epca.impl.events;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.effects.debuff.ViralEffect;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ViralEffectEventHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();

        
        MobEffectInstance viralEffect = entity.getEffect(ModEffects.VIRAL.get());
        if (viralEffect == null) return;

        
        int amplifier = viralEffect.getAmplifier();
        
        float damageMultiplier = ViralEffect.getDamageMultiplier(amplifier);

        
        float originalDamage = event.getAmount();
        float finalDamage = originalDamage + damageMultiplier;
        event.setAmount(finalDamage);
    }
}
