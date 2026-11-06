package org.tdddd.epca.impl.events;

import net.minecraft.nbt.CompoundTag;
import org.tdddd.epca.impl.events.ParasiteEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.difficulty.DifficultyEffects;
import org.tdddd.epca.impl.overworld.registry.effects.debuff.ParaCurseEffect;

@Mod.EventBusSubscriber(modid = epca.MODID)
public class HardLegendaryParasiteEvents {
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        Level level = event.getEntity().level();
        if (level.isClientSide()) return;
        if (!DifficultyEffects.isLegendary(level)) return;
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        if (!ParasiteEvents.isParasiteEntity(attacker)) return;

        LivingEntity target = event.getEntity();
        if (target instanceof Player || ParasiteEvents.isParasiteEntity(target)) return;

        target.getPersistentData().putBoolean("PARA_CURSE", true);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.level();
        if (level.isClientSide()) return;
        if (!DifficultyEffects.isLegendary(level)) return;
        if (entity instanceof Player || ParasiteEvents.isParasiteEntity(entity)) return;

        CompoundTag data = entity.getPersistentData();
        if (data.getBoolean("PARA_CURSE")) {
            data.remove("PARA_CURSE");
            ParaCurseEffect.tryConvertEntity(entity, 0, true);
        }
    }
}
