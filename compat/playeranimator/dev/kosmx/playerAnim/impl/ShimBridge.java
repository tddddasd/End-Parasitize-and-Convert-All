package dev.kosmx.playerAnim.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EPCA-local reflection guard for the <b>player-animator</b> downgrade shim.
 *
 * <p>This class is <b>not</b> part of the real player-animator API. It exists so that the shim in
 * {@code compat/playeranimator} can tell "the real mod is present" from "we are the substitute",
 * and so that every entry point fails safe (log once, return a neutral value) instead of throwing.
 *
 * <p>The probe is {@code dev.kosmx.playerAnim.impl.Helper} - a class the real player-animator ships
 * and this shim deliberately does not define. If it ever resolves, the shim entry points attempt a
 * reflective delegate into the real implementation and fall back to no-op on any {@link Throwable}.
 *
 * <p><b>Note:</b> because the shim reuses the real package and class names, the JVM cannot load
 * both. The supported lifecycle is: shim present while no official 26.1.2 build exists, then delete
 * {@code compat/playeranimator} and drop in the real jar.
 */
public final class ShimBridge {

    /** Marker class shipped only by the real player-animator. */
    private static final String REAL_MARKER = "dev.kosmx.playerAnim.impl.Helper";

    private static final Logger LOG = LoggerFactory.getLogger("epca-playeranimator-shim");

    private static Boolean realPresent;
    private static boolean warned;

    private ShimBridge() {}

    /** {@code true} only if the genuine player-animator implementation is on the classpath. */
    public static synchronized boolean isRealPlayerAnimatorPresent() {
        if (realPresent == null) {
            boolean found;
            try {
                Class.forName(REAL_MARKER, false, ShimBridge.class.getClassLoader());
                found = true;
            } catch (Throwable ignored) {
                found = false;
            }
            realPresent = found;
        }
        return realPresent;
    }

    /** Emit the one-shot "animations are disabled" notice; never throws. */
    public static void warnDisabledOnce() {
        if (warned || isRealPlayerAnimatorPresent()) return;
        warned = true;
        try {
            LOG.warn("player-animator has no Minecraft 26.1.2 release; EPCA is running with the "
                    + "no-op compat shim (compat/playeranimator). Third-person/first-person item "
                    + "animations are disabled. See PORT-STATUS.md -> 'player-animator downgrade'.");
        } catch (Throwable ignored) {
            // logging must never break gameplay
        }
    }

    /**
     * Reflective delegate used by the shim entry points. Returns {@code null} whenever the real
     * implementation is absent or anything at all goes wrong.
     */
    public static Object tryReal(String ownerClass, String method, Class<?>[] parameterTypes, Object... args) {
        if (!isRealPlayerAnimatorPresent()) return null;
        try {
            Class<?> owner = Class.forName(ownerClass, false, ShimBridge.class.getClassLoader());
            return owner.getMethod(method, parameterTypes).invoke(null, args);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
