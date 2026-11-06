package org.tdddd.epca.impl.client.render.compat;

public final class IrisShaderCompat {

    private static final boolean IRIS_LOADED = detect();

    private IrisShaderCompat() {
    }

    private static boolean detect() {

        for (String className : new String[]{
                "net.irisshaders.iris.api.v0.IrisApi",
                "net.coderbot.iris.api.v0.IrisApi",
        }) {
            try {
                Class.forName(className);
                return true;
            } catch (Throwable ignored) {

            }
        }
        return false;
    }

    public static boolean isIrisLoaded() {
        return IRIS_LOADED;
    }

    public static boolean isShaderPackActive() {
        return false;
    }
}
