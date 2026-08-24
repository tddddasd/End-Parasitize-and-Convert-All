package org.tdddd.epca.impl.events;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.overworld.difficulty.DifficultyEffects;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

@Mod.EventBusSubscriber
public class CothEffectEvents {

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        
        if (event.getEffectInstance().getEffect() != ModEffects.COTH.get()) {
            return;
        }

        LivingEntity entity = event.getEntity();
        MobEffectInstance newEffect = event.getEffectInstance();
        int newAmplifier = newEffect.getAmplifier();

        MobEffectInstance existingEffect = entity.getEffect(ModEffects.COTH.get());

        
        if (existingEffect != null && newAmplifier <= existingEffect.getAmplifier()) {
            event.setResult(Event.Result.DENY);
        }
    }
}