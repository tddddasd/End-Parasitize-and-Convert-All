package org.tdddd.epca.impl.client.render.compat;

import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

public final class IrisShaderCompat {

    private static final boolean IRIS_LOADED = detectIrisLoaded();

    private static volatile boolean resolved;
    private static Method isShaderPackInUseMethod;
    private static Object apiInstance;

    private IrisShaderCompat() {
    }

    private static boolean detectIrisLoaded() {
        try {
            ModList modList = ModList.get();
            return modList != null && (modList.isLoaded("oculus") || modList.isLoaded("iris"));
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isIrisLoaded() {
        return IRIS_LOADED;
    }

    public static boolean isShaderPackActive() {
        if (!IRIS_LOADED) {
            return false;
        }
        if (!resolved) {
            resolveApi();
        }
        if (isShaderPackInUseMethod == null || apiInstance == null) {
            return false;
        }
        try {
            Object active = isShaderPackInUseMethod.invoke(apiInstance);
            return Boolean.TRUE.equals(active);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void resolveApi() {
        synchronized (IrisShaderCompat.class) {
            if (resolved) {
                return;
            }
            try {
                Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                Object instance = apiClass.getMethod("getInstance").invoke(null);
                Method method = apiClass.getMethod("isShaderPackInUse");
                apiInstance = instance;
                isShaderPackInUseMethod = method;
            } catch (Throwable ignored) {

                apiInstance = null;
                isShaderPackInUseMethod = null;
            }
            resolved = true;
        }
    }

    public static boolean shouldDeferItemLayer(ItemDisplayContext context) {
        if (context == ItemDisplayContext.GUI) {
            return false;
        }
        return isShaderPackActive();
    }
}
