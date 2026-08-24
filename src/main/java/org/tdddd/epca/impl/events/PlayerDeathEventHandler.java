package org.tdddd.epca.impl.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.ModEntities;
import org.tdddd.epca.impl.overworld.registry.entities.entity.poverty.MediumIncompleteForm;

@Mod.EventBusSubscriber
public class PlayerDeathEventHandler {
    
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player)) return;
        if (player.level().isClientSide) return;

        
        MobEffectInstance effect = player.getEffect(ModEffects.COTH.get());
        if (effect == null || effect.getAmplifier() == 0) return;

        ServerLevel level = (ServerLevel) player.level();
        MediumIncompleteForm infested = new MediumIncompleteForm(
                ModEntities.MEDIUM_INCOMPLETE_FORM.get(), level);
        infested.setPos(player.getX(), player.getY(), player.getZ());

        

        
        

        level.addFreshEntity(infested);

        player.removeEffect(ModEffects.COTH.get());
    }
}