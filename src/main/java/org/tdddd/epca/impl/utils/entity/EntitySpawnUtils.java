package org.tdddd.epca.impl.utils.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

public final class EntitySpawnUtils {

    private EntitySpawnUtils() {}

    public static boolean checkMonsterSpawnRules(
            EntityType<?> type, ServerLevelAccessor level,
            EntitySpawnReason spawnType, BlockPos pos, RandomSource random) {
        return level.getMaxLocalRawBrightness(pos) < 8;
    }

    public static boolean isDarkSky(ServerLevelAccessor level, BlockPos pos) {
        return level.getMaxLocalRawBrightness(pos) < 8;
    }

    public static <T extends Entity> T spawnEntity(EntityType<T> type, Level level,
                                                     double x, double y, double z,
                                                     boolean randomOffset, boolean randomVelocity) {
        T entity = type.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (entity == null) return null;

        RandomSource rand = level.getRandom();
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
