package org.tdddd.epca.impl.client.entity;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Client-side registry for model behaviors and render layers.
 * Allows entities to register custom model animations or additional render layers.
 */
@OnlyIn(Dist.CLIENT)
public class EpcaRenderRegistry {

    @FunctionalInterface
    public interface ModelBehavior {
        void apply(long instanceId, float partialTick);
    }

    private static final Map<Class<?>, List<Consumer<?>>> RENDER_LAYERS = new HashMap<>();
    private static final Map<Class<?>, List<ModelBehavior>> MODEL_BEHAVIORS = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> void registerRenderLayer(Class<T> entityClass, Consumer<T> layer) {
        RENDER_LAYERS.computeIfAbsent(entityClass, k -> new ArrayList<>()).add((Consumer<?>) layer);
    }

    @SuppressWarnings("unchecked")
    public static <T> List<Consumer<T>> getRenderLayers(Class<T> entityClass) {
        return (List<Consumer<T>>) (List<?>) RENDER_LAYERS.getOrDefault(entityClass, List.of());
    }

    public static void registerModelBehavior(Class<?> entityClass, ModelBehavior behavior) {
        MODEL_BEHAVIORS.computeIfAbsent(entityClass, k -> new ArrayList<>()).add(behavior);
    }

    public static List<ModelBehavior> getModelBehaviors(Class<?> entityClass) {
        return MODEL_BEHAVIORS.getOrDefault(entityClass, List.of());
    }
}
