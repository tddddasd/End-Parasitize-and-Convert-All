package org.tdddd.epca.impl.overworld.registry.effects.buff;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.tdddd.epca.impl.overworld.registry.blocks.HiveNodeLocator;

/**
 * Link: a parasite carrying it tends to patrol around a hive node.
 *
 * <p>Any living entity with the effect steers towards the nearest hive node within 16 blocks, so the
 * behaviour does not need to be wired into each mob's AI.</p>
 */
public class LinkEffect extends MobEffect {

    private static final int NODE_RANGE = 16;
    private static final double SPEED = 1.0D;

    public LinkEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x8B2E6E);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 40 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos node = HiveNodeLocator.findNearest(level, entity.blockPosition(), NODE_RANGE);
        if (node != null && entity instanceof net.minecraft.world.entity.Mob mob) {
            mob.getNavigation().moveTo(node.getX() + 0.5D, node.getY(), node.getZ() + 0.5D, SPEED);
        }
    }
}
