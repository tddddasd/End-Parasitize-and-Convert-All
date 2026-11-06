package org.tdddd.epca.impl.events;

import org.tdddd.epca.impl.events.ParasiteEvents;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.difficulty.DifficultyEffects;
import org.tdddd.epca.impl.overworld.registry.effects.debuff.ParaCurseEffect;

@EventBusSubscriber(modid = epca.MODID)
public class HardLegendaryParasiteEvents {
    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        Level level = event.getEntity().level();
        if (level.isClientSide()) return;
        if (!DifficultyEffects.isLegendary(level)) return;
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        if (!ParasiteEvents.isParasiteEntity(attacker)) return;

        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (target instanceof Player || ParasiteEvents.isParasiteEntity(target)) return;

        target.getPersistentData().putBoolean("PARA_CURSE", true);
    }

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        Level level = entity.level();
        if (level.isClientSide()) return;
        if (!DifficultyEffects.isLegendary(level)) return;
        if (entity instanceof Player || ParasiteEvents.isParasiteEntity(entity)) return;

        CompoundTag data = entity.getPersistentData();
        if (data.getBoolean("PARA_CURSE").orElse(false)) {
            data.remove("PARA_CURSE");
            ParaCurseEffect.tryConvertEntity(entity, 0, true);
        }
    }
}
