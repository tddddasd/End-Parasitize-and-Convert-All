package org.tdddd.epca.impl.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.epca.impl.epca;
import org.tdddd.epca.impl.overworld.registry.ModTags;

@Mod.EventBusSubscriber(modid = epca.MODID)
public class InfestedUndeadHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        Level level = victim.level();
        if (level.isClientSide) return;

        
        if (!victim.getType().is(ModTags.INFESTED_UNDEAD)) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;

        double followRange = victim.getAttributeValue(Attributes.FOLLOW_RANGE);
        double radius = followRange * 2.0;
        double radiusSqr = radius * radius;

        level.getEntitiesOfClass(LivingEntity.class, victim.getBoundingBox().inflate(radius), e ->
                e != victim &&
                        e.getType().is(ModTags.INFESTED_UNDEAD) &&
                        e instanceof Mob
        ).forEach(e -> {
            Mob nearby = (Mob) e;
            if (nearby.distanceToSqr(victim) <= radiusSqr) {
                nearby.setTarget(attacker);
            }
        });
    }
}