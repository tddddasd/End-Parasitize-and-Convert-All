package org.tdddd.epca.impl.overworld.registry.effects.buff;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import org.tdddd.epca.impl.overworld.registry.ModEffects;
import org.tdddd.epca.impl.overworld.registry.entities.IParasite;

/**
 * Symbiosis Lie (1.20.1 parity): the affected mob hunts its own kind and infects everything that is
 * not a parasite around it with Para Curse. Any same-kind mob it damages while the effect is active
 * additionally takes retaliation damage, and the mob bursts on death - both live in
 * {@code SymbiosisDeathHandler}.
 */
public class SymbiosisLieEffect extends MobEffect {
    /** How far the mob looks for a same-kind victim to walk to. */
    private static final double SAME_KIND_RADIUS = 8.0D;
    /** How far the Para Curse spreads around the affected mob. */
    private static final double CURSE_RADIUS = 2.5D;
    /** Duration of the Para Curse handed to neighbours, same as 1.20.1. */
    private static final int CURSE_DURATION = 200;
    private static final double SAME_KIND_SPEED = 1.0D;

    public SymbiosisLieEffect() { super(MobEffectCategory.BENEFICIAL, 0x5A1E3A); }

    @Override public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) { return tickCount % 20 == 0; }

    @Override public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        // Walk towards a same-kind neighbour so the mob visibly seeks its own kind.
        if (entity instanceof Mob mob) {
            var sameKind = level.getEntitiesOfClass(entity.getClass(),
                    entity.getBoundingBox().inflate(SAME_KIND_RADIUS), e -> e != entity && e.isAlive());
            if (!sameKind.isEmpty()) {
                LivingEntity target = sameKind.get(level.getRandom().nextInt(sameKind.size()));
                mob.getNavigation().moveTo(target, SAME_KIND_SPEED);
            }
        }

        // Spread Para Curse to every nearby non-parasite.
        AABB area = entity.getBoundingBox().inflate(CURSE_RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (target == entity || target instanceof IParasite) continue;
            target.addEffect(new MobEffectInstance(ModEffects.PARA_CURSE, CURSE_DURATION, 0, false, true, true));
        }
        return true;
    }
}
