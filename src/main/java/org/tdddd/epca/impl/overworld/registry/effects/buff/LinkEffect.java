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
 * <p>26.1.2 uses applyEffectTick(ServerLevel, LivingEntity, int) and shouldApplyEffectTickThisTick.</p>
 */
public class LinkEffect extends MobEffect {

    private static final int NODE_RANGE = 16;
    private static final double SPEED = 1.0D;

    public LinkEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x8B2E6E);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return tickCount % 40 == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel serverLevel, LivingEntity entity, int amplifier) {
        BlockPos node = HiveNodeLocator.findNearest(serverLevel, entity.blockPosition(), NODE_RANGE);
        if (node != null && entity instanceof net.minecraft.world.entity.Mob mob) {
            mob.getNavigation().moveTo(node.getX() + 0.5D, node.getY(), node.getZ() + 0.5D, SPEED);
        }
        return true;
    }
}
