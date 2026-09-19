package org.tdddd.epca.impl.events;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.overworld.difficulty.DifficultyEffects;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

@EventBusSubscriber
public class CothEffectEvents {
    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        
        if (event.getEffectInstance().getEffect() != ModEffects.COTH) {
            return;
        }

        LivingEntity entity = event.getEntity();
        MobEffectInstance newEffect = event.getEffectInstance();
        int newAmplifier = newEffect.getAmplifier();

        MobEffectInstance existingEffect = entity.getEffect(ModEffects.COTH);

        
        if (existingEffect != null && newAmplifier <= existingEffect.getAmplifier()) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }
}