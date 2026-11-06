package org.tdddd.epca.impl.events;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.effects.debuff.FearEffect;

@EventBusSubscriber(modid = epca.MODID)
public class PanicEffectEventHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {

        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;

        if (attacker instanceof Player) return;

        MobEffectInstance panic = attacker.getEffect(ModEffects.FEAR);
        if (panic == null) return;
        if (panic.getAmplifier() < 0) return;

        if (!event.getSource().isDirect()) return;

        LivingEntity victim = event.getEntity();

        if (FearEffect.rollPanicMiss(attacker)) {

            event.setCanceled(true);

            if (attacker instanceof Mob mob && mob != victim) {
                mob.setLastHurtByMob(victim);
            }
        }
    }
}
