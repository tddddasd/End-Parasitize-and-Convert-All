package org.tdddd.epca.impl.overworld.registry.entities;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class EpcaEntityManager {

    private static final Map<EntityType<? extends LivingEntity>, Supplier<AttributeSupplier>> ATTRIBUTE_BLUEPRINTS = new LinkedHashMap<>();

    private static final List<EntityType<? extends LivingEntity>> RENDER_TYPES = new ArrayList<>();

    private static final Map<EntityType<?>, ResourceLocation> MODEL_MAP = new HashMap<>();
    private static final Map<EntityType<?>, ResourceLocation> TEXTURE_MAP = new HashMap<>();
    private static final Map<EntityType<?>, ResourceLocation> ANIMATION_MAP = new HashMap<>();

    private static final Map<EntityType<?>, Set<LivingEntity>> TRACKED_ENTITIES = new ConcurrentHashMap<>();

    public static <T extends LivingEntity> EntityType<T> registerMob(
            EntityType<T> type, Supplier<AttributeSupplier> attrSupplier) {
        ATTRIBUTE_BLUEPRINTS.put(type, attrSupplier);
        RENDER_TYPES.add(type);
        return type;
    }

    public static <T extends LivingEntity> EntityType<T> registerMobWithRender(
            EntityType<T> type, Supplier<AttributeSupplier> attrSupplier,
            ResourceLocation model, ResourceLocation texture, ResourceLocation animation) {
        ATTRIBUTE_BLUEPRINTS.put(type, attrSupplier);
        RENDER_TYPES.add(type);
        MODEL_MAP.put(type, model);
        TEXTURE_MAP.put(type, texture);
        ANIMATION_MAP.put(type, animation);
        return type;
    }

    public static <T extends LivingEntity> EntityType<T> registerRenderOnly(
            EntityType<T> type, ResourceLocation model, ResourceLocation texture, ResourceLocation animation) {
        RENDER_TYPES.add(type);
        MODEL_MAP.put(type, model);
        TEXTURE_MAP.put(type, texture);
        ANIMATION_MAP.put(type, animation);
        return type;
    }

    public static <T extends LivingEntity> EntityType<T> registerMobNoRender(
            EntityType<T> type, Supplier<AttributeSupplier> attrSupplier) {
        ATTRIBUTE_BLUEPRINTS.put(type, attrSupplier);
        return type;
    }

    public static void createAttributes(EntityAttributeCreationEvent event) {
        ATTRIBUTE_BLUEPRINTS.forEach((type, supplier) -> event.put(type, supplier.get()));
        ATTRIBUTE_BLUEPRINTS.clear();
    }

    @SuppressWarnings("unchecked")
    public static List<EntityType<? extends LivingEntity>> consumeRenderTypes() {
        List<EntityType<? extends LivingEntity>> result = new ArrayList<>(RENDER_TYPES);
        RENDER_TYPES.clear();
        return result;
    }

    public static ResourceLocation getModel(EntityType<?> type) {
        return MODEL_MAP.get(type);
    }

    public static ResourceLocation getTexture(EntityType<?> type) {
        return TEXTURE_MAP.get(type);
    }

    public static ResourceLocation getAnimation(EntityType<?> type) {
        return ANIMATION_MAP.get(type);
    }

    public static void track(LivingEntity entity) {
        if (entity.level().isClientSide) return;
        TRACKED_ENTITIES.computeIfAbsent(entity.getType(),
                k -> Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()))).add(entity);
    }

    public static void untrack(LivingEntity entity) {
        Set<LivingEntity> instances = TRACKED_ENTITIES.get(entity.getType());
        if (instances != null) instances.remove(entity);
    }

    @SuppressWarnings("unchecked")
    public static <T extends LivingEntity> List<T> getTracked(EntityType<T> type) {
        Set<LivingEntity> set = TRACKED_ENTITIES.get(type);
        if (set == null) return List.of();
        return (List<T>) set.stream().filter(LivingEntity::isAlive).toList();
    }

    public static void clearAll() {
        TRACKED_ENTITIES.values().forEach(Set::clear);
        TRACKED_ENTITIES.clear();
    }
}
