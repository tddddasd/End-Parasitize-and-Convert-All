package org.tdddd.epca.impl.overworld.registry.effects.buff;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.tdddd.epca.impl.overworld.registry.blocks.HiveNodeLocator;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Link: a parasite carrying it tends to patrol around a hive node.
 *
 * <p>Any living entity with the effect steers towards the nearest hive node within 16 blocks, so the
 * behaviour does not need to be wired into each mob's AI.</p>
 */
public class LinkEffect extends MobEffect {

    private static final int NODE_RANGE = 16;
    private static final double SPEED = 1.0D;
    /** A node search scans a 33^3 cube, so cache the result per entity instead of every effect tick. */
    private static final int CACHE_TICKS = 100;
    private static final Map<UUID, CachedNode> CACHE = new HashMap<>();

    private record CachedNode(BlockPos pos, long gameTime) {}

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
        UUID id = entity.getUUID();
        long now = level.getGameTime();
        CachedNode cached = CACHE.get(id);
        BlockPos node;
        if (cached != null && now - cached.gameTime() < CACHE_TICKS
                && (cached.pos() == null
                || level.getBlockState(cached.pos()).is(org.tdddd.epca.impl.overworld.registry.ModBlocks.HIVE_NODE.get()))) {
            node = cached.pos();
        } else {
            node = HiveNodeLocator.findNearest(level, entity.blockPosition(), NODE_RANGE);
            if (CACHE.size() > 512) CACHE.clear();
            CACHE.put(id, new CachedNode(node, now));
        }
        if (node != null && entity instanceof net.minecraft.world.entity.Mob mob) {
            mob.getNavigation().moveTo(node.getX() + 0.5D, node.getY(), node.getZ() + 0.5D, SPEED);
        }
    }
}
