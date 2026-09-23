package org.tdddd.epca.impl.client.render.compat;

/**
 * Iris / Oculus shader-pack compatibility probe.
 *
 * <h2>1.20.1 -&gt; 26.1.2</h2>
 * The 1.20.1 twin asked Iris whether a shader pack was active and, when it was, deferred the sky
 * rupture effect and the item shader layers to the end of {@code GameRenderer.renderLevel()} so they
 * could write the main framebuffer after the pack's GBuffer composite. The detection used the Iris API
 * through reflection, and the whole mechanism existed because a pack re-renders the scene.
 *
 * <p>Neither Iris nor Oculus is available for 26.1.2 at the time of this port (the shader pipeline was
 * replaced wholesale by {@code RenderPipeline}, which is what an Iris port has to target). The probe
 * therefore reports "not loaded" and "no pack active" unconditionally, which makes the deferred path
 * in {@code SkyRuptureRenderer} a documented no-op: the effect draws once per frame at the AfterSky
 * stage, which is correct without a pack.</p>
 *
 * <p>The class is kept, rather than the call site being simplified away, so that a future Iris port
 * only has to fill in {@link #detect()} and {@link #isShaderPackActive()} and the deferred path lights
 * up again.</p>
 */
public final class IrisShaderCompat {

    /** Whether an Iris-family mod was found on the classpath. Resolved once. */
    private static final boolean IRIS_LOADED = detect();

    private IrisShaderCompat() {
    }

    private static boolean detect() {
        // 26.1.2: no Iris/Oculus port exists yet. The reflective probe is kept so that the moment a
        // build ships the classes again this flips to true without touching any caller.
        for (String className : new String[]{
                "net.irisshaders.iris.api.v0.IrisApi",
                "net.coderbot.iris.api.v0.IrisApi",
        }) {
            try {
                Class.forName(className);
                return true;
            } catch (Throwable ignored) {
                // Not installed.
            }
        }
        return false;
    }

    /** True when an Iris-family mod is present. */
    public static boolean isIrisLoaded() {
        return IRIS_LOADED;
    }

    /**
     * True when a shader pack is currently active and the scene therefore goes through a GBuffer.
     *
     * <p>Always false on 26.1.2 (see the class comment).</p>
     */
    public static boolean isShaderPackActive() {
        return false;
    }
}
