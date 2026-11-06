package org.tdddd.epca.impl.overworld.raid;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;
import org.tdddd.epca.impl.overworld.registry.ModEntities;

import java.util.HashMap;
import java.util.Map;

public final class InfestedPurification {

    private static volatile Map<EntityType<?>, EntityType<?>> toOriginal;

    private static Map<EntityType<?>, EntityType<?>> table() {
        Map<EntityType<?>, EntityType<?>> map = toOriginal;
        if (map != null) return map;
        synchronized (InfestedPurification.class) {
            if (toOriginal == null) {
                map = new HashMap<>();
                map.put(ModEntities.INFESTED_ZOMBIE.get(), EntityType.ZOMBIE);
                map.put(ModEntities.INFESTED_HUSK.get(), EntityType.HUSK);
                map.put(ModEntities.INFESTED_DROWNED.get(), EntityType.DROWNED);
                map.put(ModEntities.INFESTED_PILLAGER.get(), EntityType.PILLAGER);
                map.put(ModEntities.INFESTED_VINDICATOR.get(), EntityType.VINDICATOR);
                map.put(ModEntities.INFESTED_PIG.get(), EntityType.PIG);
                map.put(ModEntities.INFESTED_SHEEP.get(), EntityType.SHEEP);
                map.put(ModEntities.INFESTED_COW.get(), EntityType.COW);
                map.put(ModEntities.INFESTED_CHICKEN.get(), EntityType.CHICKEN);
                map.put(ModEntities.INFESTED_ENDERMITE.get(), EntityType.ENDERMITE);
                map.put(ModEntities.INFESTED_SILVERFISH.get(), EntityType.SILVERFISH);
                map.put(ModEntities.INFESTED_SKELETON.get(), EntityType.SKELETON);
                map.put(ModEntities.INFESTED_FOX.get(), EntityType.FOX);
                map.put(ModEntities.INFESTED_SLIME_SIZE0.get(), EntityType.SLIME);
                map.put(ModEntities.INFESTED_SLIME_SIZE1.get(), EntityType.SLIME);
                map.put(ModEntities.INFESTED_SLIME_SIZE3.get(), EntityType.SLIME);
                map.put(ModEntities.INFESTED_SPIDER.get(), EntityType.SPIDER);
                map.put(ModEntities.INFESTED_BAT.get(), EntityType.BAT);
                map.put(ModEntities.INFESTED_VILLAGER.get(), EntityType.VILLAGER);
                map.put(ModEntities.INFESTED_ZOMBIE_VILLAGER.get(), EntityType.ZOMBIE_VILLAGER);
                map.put(ModEntities.INFESTED_WOLF.get(), EntityType.WOLF);
                map.put(ModEntities.INFESTED_ENDERMAN.get(), EntityType.ENDERMAN);
                toOriginal = map;
            }
            return toOriginal;
        }
    }

    private InfestedPurification() { }

    public static @Nullable EntityType<?> originalOf(EntityType<?> infested) {
        return table().get(infested);
    }

    public static boolean isInfested(EntityType<?> type) {
        return table().containsKey(type);
    }

    public static boolean purify(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return false;
        EntityType<?> original = originalOf(entity.getType());
        if (original == null) return false;
        if (!(original.create(level, EntitySpawnReason.MOB_SUMMONED) instanceof LivingEntity fresh)) return false;

        fresh.snapTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());

        float ratio = entity.getMaxHealth() <= 0.0F ? 1.0F : entity.getHealth() / entity.getMaxHealth();
        fresh.setHealth(Math.max(1.0F, fresh.getMaxHealth() * ratio));
        if (fresh instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
        level.addFreshEntity(fresh);
        entity.discard();
        return true;
    }

    public static int size() {
        return table().size();
    }

    public static String originalId(LivingEntity entity) {
        EntityType<?> original = originalOf(entity.getType());
        return original == null ? "none" : String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(original));
    }
}
