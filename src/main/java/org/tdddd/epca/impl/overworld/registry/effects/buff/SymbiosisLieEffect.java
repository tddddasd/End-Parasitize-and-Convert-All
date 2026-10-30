package org.tdddd.epca.impl.overworld.registry.effects.buff;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

/** Symbiosis Lie: spreads Para Curse to nearby non-parasites and darkens the regeneration colour. */
public class SymbiosisLieEffect extends MobEffect {
    public SymbiosisLieEffect() { super(MobEffectCategory.BENEFICIAL, 0x5A1E3A); }

    @Override public boolean isDurationEffectTick(int duration, int amplifier) { return duration % 20 == 0; }

    @Override public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return;
        if (entity instanceof net.minecraft.world.entity.Mob mob) {
            var sameKind = entity.level().getEntitiesOfClass(entity.getClass(),
                    entity.getBoundingBox().inflate(8.0D), e -> e != entity && e.isAlive());
            if (!sameKind.isEmpty()) {
                var target = sameKind.get(entity.level().random.nextInt(sameKind.size()));
                mob.getNavigation().moveTo(target, 1.0D);
            }
        }
        AABB area = entity.getBoundingBox().inflate(2.5D);
        for (LivingEntity target : entity.level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (target == entity || target instanceof IParasite) continue;
            target.addEffect(new MobEffectInstance(ModEffects.PARA_CURSE.get(), 200, 0, false, true, true));
        }
    }
}