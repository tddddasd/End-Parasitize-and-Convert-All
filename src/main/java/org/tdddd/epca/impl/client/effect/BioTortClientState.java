package org.tdddd.epca.impl.client.effect;

import net.minecraft.util.Mth;
import org.tdddd.epca.impl.overworld.registry.entities.entity.special.BioTortSkillConstants;

/**
 *  1  2
 *
 * <h2></h2>
 * <p>""
 *  {@code SyncHotbarMarkPacket} / {@code ScreenCorruptionPacket}
 * ""
 * </p>
 *
 * <h2></h2>
 * <ul>
 *   <li>{@link System#currentTimeMillis()}
 *       {@link #MARK_ANIMATION_MILLIS} {@code durationTicks}
 *       </li>
 *   <li> +  tick ""
 *       / tick
 *       </li>
 * </ul>
 *
 * <p> {@code net.minecraft.client}
 * /
 * </p>
 */
public final class BioTortClientState {

    /** " 0.25 " */
    public static final float MARK_ANIMATION_MILLIS = 250.0F;

    /** bit 0..80 =  */
    private static int hotbarMask;

    /**  */
    private static long markAnimationStartMillis;

    /**  */
    private static long markExpireAtMillis;

    /**  */
    private static boolean corruptionActive;

    /**  {@link System#nanoTime()} */
    private static long corruptionStartNanos;

    /**  */
    private static float corruptionWindowSeconds;

    /**  */
    private static float corruptionDecaySeconds;

    /**  */
    private static long frameToken;

    private BioTortClientState() {
    }

    // ===============================================================================================
    //   1
    // ===============================================================================================

    /**
     * {@code slotMask == 0} ""
     *
     * @param slotMask      9
     * @param durationTicks tick
     */
    public static void applyHotbarMark(int slotMask, int durationTicks) {
        hotbarMask = slotMask & ((1 << (BioTortSkillConstants.MARKED_SLOT_MAX + 1)) - 1);
        markAnimationStartMillis = System.currentTimeMillis();
        markExpireAtMillis = markAnimationStartMillis + Math.max(1, durationTicks) * 50L;
        if (hotbarMask == 0) {
            //  0
            markExpireAtMillis = markAnimationStartMillis;
        }
    }

    /**  /  /  */
    public static void clearHotbarMark() {
        hotbarMask = 0;
    }

    public static int hotbarMask() {
        return hotbarMask;
    }

    public static boolean hasHotbarMark() {
        return hotbarMask != 0 && System.currentTimeMillis() < markExpireAtMillis;
    }

    /** 0..8 */
    public static boolean isSlotMarked(int slot) {
        return slot >= 0 && slot <= BioTortSkillConstants.MARKED_SLOT_MAX
                && (hotbarMask & (1 << slot)) != 0;
    }

    /**
     * ""0 = 1 =
     *
     * <p> 0.25  0..1
     * " 0..0.5 0.5..1"</p>
     */
    public static float markAnimationProgress() {
        long elapsed = System.currentTimeMillis() - markAnimationStartMillis;
        return Mth.clamp(elapsed / MARK_ANIMATION_MILLIS, 0.0F, 1.0F);
    }

    /** 1 = 0 = / */
    public static float markRemainingFraction() {
        long total = Math.max(1L, markExpireAtMillis - markAnimationStartMillis);
        long remaining = markExpireAtMillis - System.currentTimeMillis();
        return Mth.clamp((float) remaining / (float) total, 0.0F, 1.0F);
    }

    // ===============================================================================================
    //   2
    // ===============================================================================================

    /**
     *
     *
     * @param corruptionTicks tick 4  = 80
     * @param decayTicks      tick""1.5  = 30
     */
    public static void startCorruption(int corruptionTicks, int decayTicks) {
        corruptionActive = true;
        corruptionStartNanos = System.nanoTime();
        corruptionWindowSeconds = Math.max(0, corruptionTicks) / 20.0F;
        corruptionDecaySeconds = Math.max(0, decayTicks) / 20.0F;
    }

    /**  /  /  /  */
    public static void stopCorruption() {
        corruptionActive = false;
    }

    public static boolean corruptionActive() {
        return corruptionActive;
    }

    /**  */
    public static float corruptionElapsedSeconds() {
        if (!corruptionActive) {
            return 0.0F;
        }
        return (float) ((System.nanoTime() - corruptionStartNanos) / 1_000_000_000.0D);
    }

    /**  */
    public static float corruptionWindowSeconds() {
        return corruptionWindowSeconds;
    }

    /**  */
    public static float corruptionDecaySeconds() {
        return corruptionDecaySeconds;
    }

    /**  */
    public static float corruptionTotalSeconds() {
        return corruptionWindowSeconds + corruptionDecaySeconds;
    }

    /** "" */
    public static long nextFrameToken() {
        return ++frameToken;
    }

    // ===============================================================================================
    // ===============================================================================================

    /**  tick  */
    public static void clientTick() {
        if (hotbarMask != 0 && System.currentTimeMillis() >= markExpireAtMillis) {
            hotbarMask = 0;
        }
        if (corruptionActive && corruptionElapsedSeconds() >= corruptionTotalSeconds()) {
            corruptionActive = false;
        }
    }

    /**  /  /  */
    public static void clear() {
        hotbarMask = 0;
        markExpireAtMillis = 0L;
        corruptionActive = false;
        corruptionWindowSeconds = 0.0F;
        corruptionDecaySeconds = 0.0F;
    }
}

