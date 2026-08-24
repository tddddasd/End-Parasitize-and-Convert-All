package org.tdddd.epca.impl.utils.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Utility methods for entity spawning rules and placement checks.
 */
public final class EntitySpawnUtils {

    private EntitySpawnUtils() {}

    /** Standard monster spawn check: low light level. */
    public static boolean checkMonsterSpawnRules(
            EntityType<?> type, ServerLevelAccessor level,
            MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return level.getMaxLocalRawBrightness(pos) < 8;
    }

    /** Check if sky is dark enough for spawning. */
    public static boolean isDarkSky(ServerLevelAccessor level, BlockPos pos) {
        return level.getMaxLocalRawBrightness(pos) < 8;
    }

    /**
     * Spawn an entity of the given type at the specified position with optional random offset and velocity.
     */
    public static <T extends Entity> T spawnEntity(EntityType<T> type, Level level,
                                                     double x, double y, double z,
                                                     boolean randomOffset, boolean randomVelocity) {
        T entity = type.create(level);
        if (entity == null) return null;

        RandomSource rand = level.random;
        double ox = randomOffset ? rand.nextDouble() - 0.5 : 0;
        double oz = randomOffset ? rand.nextDouble() - 0.5 : 0;
        entity.setPos(x + ox, y, z + oz);

        if (entity instanceof LivingEntity living && randomVelocity) {
            living.setDeltaMovement(
                    (rand.nextDouble() - 0.5) * 0.1,
                    rand.nextDouble() * 0.1,
                    (rand.nextDouble() - 0.5) * 0.1);
        }

        level.addFreshEntity(entity);
        return entity;
    }
}
