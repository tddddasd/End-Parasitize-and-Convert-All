package org.tdddd.epca.impl.overworld.registry.effects.buff;

import org.tdddd.epca.impl.events.ParasiteEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import org.tdddd.epca.impl.overworld.registry.ModEffects;

public class SymbiosisLieEffect extends MobEffect {

    private static final double SAME_KIND_RADIUS = 8.0D;

    private static final double CURSE_RADIUS = 2.5D;

    private static final int CURSE_DURATION = 200;
    private static final double SAME_KIND_SPEED = 1.0D;

    public SymbiosisLieEffect() { super(MobEffectCategory.BENEFICIAL, 0x5A1E3A); }

    @Override public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) { return tickCount % 20 == 0; }

    @Override public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {

        if (entity instanceof Mob mob) {
            var sameKind = level.getEntitiesOfClass(entity.getClass(),
                    entity.getBoundingBox().inflate(SAME_KIND_RADIUS), e -> e != entity && e.isAlive());
            if (!sameKind.isEmpty()) {
                LivingEntity target = sameKind.get(level.getRandom().nextInt(sameKind.size()));
                mob.getNavigation().moveTo(target, SAME_KIND_SPEED);
            }
        }

        AABB area = entity.getBoundingBox().inflate(CURSE_RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (target == entity || ParasiteEvents.isParasiteEntity(target)) continue;
            target.addEffect(new MobEffectInstance(ModEffects.PARA_CURSE, CURSE_DURATION, 0, false, true, true));
        }
        return true;
    }
}
