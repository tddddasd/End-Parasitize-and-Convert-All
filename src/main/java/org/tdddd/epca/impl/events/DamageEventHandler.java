package org.tdddd.epca.impl.events;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.overworld.registry.ModDamageTypes;
import org.tdddd.epca.impl.epca;

@Mod.EventBusSubscriber(modid = epca.MODID)
public class DamageEventHandler {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();

        
        if (source.is(ModDamageTypes.MINIMUM)) {
            LivingEntity entity = event.getEntity();

            
            if (entity.isDeadOrDying()) {
                return;
            }

            float damage = event.getAmount();
            
            float newHealth = entity.getHealth() - damage;
            entity.setHealth(newHealth);
            
            event.setCanceled(true);
            
            if (newHealth <= 0.0F) {
                entity.die(source);
            }
        }
    }
}