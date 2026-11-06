package org.tdddd.epca.impl.events;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.effects.debuff.FearEffect;

@Mod.EventBusSubscriber(modid = epca.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PanicEffectEventHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {

        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;

        if (attacker instanceof Player) return;

        MobEffectInstance panic = attacker.getEffect(ModEffects.FEAR.get());
        if (panic == null) return;
        if (panic.getAmplifier() < 0) return;

        if (event.getSource().isIndirect()) return;

        LivingEntity victim = event.getEntity();

        if (FearEffect.rollPanicMiss(attacker)) {

            event.setCanceled(true);

            if (attacker instanceof Mob mob && mob != victim) {
                mob.setLastHurtByMob(victim);
            }
        }
    }
}
